package com.personalfinance.tracker.ui.operations

import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first

enum class TransactionSaveResult { Saved, CategoryRequired, InvalidDate, InvalidAmount, InvalidCurrency, NotFound }

data class TransactionDraft(
    val id: String?,
    val owner: String,
    val type: TransactionType,
    val dateText: String,
    val amountText: String,
    val currencyCode: String,
    val categoryId: String?,
)

class TransactionFormViewModel(private val repository: FinanceRepository) {
    suspend fun load(
        transactionId: String?,
        type: TransactionType,
        defaultCurrency: String,
    ): TransactionDraft? {
        val profile = repository.observeProfile().first()
        val owner = profile?.name.orEmpty()
        if (transactionId == null) {
            return TransactionDraft(
                id = null,
                owner = owner,
                type = type,
                dateText = FinanceDateFormat.format(LocalDate.now()),
                amountText = "",
                currencyCode = defaultCurrency,
                categoryId = null,
            )
        }
        val transaction = repository.observeTransactions(null).first().firstOrNull { it.id == transactionId }
            ?: return null
        return TransactionDraft(
            id = transaction.id,
            owner = transaction.owner,
            type = transaction.type,
            dateText = FinanceDateFormat.format(transaction.date),
            amountText = BigDecimal.valueOf(transaction.money.minorUnits, 2).toPlainString(),
            currencyCode = transaction.money.currencyCode,
            categoryId = transaction.categoryId,
        )
    }

    suspend fun save(
        transactionId: String?,
        type: TransactionType,
        owner: String,
        dateText: String,
        amountText: String,
        currencyCode: String,
        categoryId: String?,
    ): TransactionSaveResult {
        if (type == TransactionType.EXPENSE && categoryId.isNullOrBlank()) {
            return TransactionSaveResult.CategoryRequired
        }
        val date = FinanceDateFormat.parse(dateText)
            ?: return TransactionSaveResult.InvalidDate
        val minorUnits = runCatching {
            BigDecimal(amountText.trim().replace(',', '.'))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0 } ?: return TransactionSaveResult.InvalidAmount

        val existing = transactionId?.let { id ->
            repository.observeTransactions(null).first().firstOrNull { it.id == id }
                ?: return TransactionSaveResult.NotFound
        }
        // Existing entries keep the currency they were recorded in, even if the
        // profile's main currency changed. New entries use the profile currency.
        val savedCurrencyCode = existing?.money?.currencyCode ?: currencyCode
        if (savedCurrencyCode !in SupportedCurrencies.codes) return TransactionSaveResult.InvalidCurrency
        repository.saveTransaction(
            FinanceTransaction(
                id = existing?.id ?: UUID.randomUUID().toString(),
                owner = owner,
                type = type,
                date = date,
                money = Money(minorUnits, savedCurrencyCode),
                categoryId = if (type == TransactionType.EXPENSE) categoryId else null,
            ),
        )
        return TransactionSaveResult.Saved
    }
}
