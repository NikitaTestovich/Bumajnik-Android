package com.personalfinance.tracker.data.backup

import com.personalfinance.tracker.data.local.*
import com.personalfinance.tracker.domain.model.IncomeComment
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class FullBackupSnapshot(
    val profile: UserProfileEntity?,
    val categories: List<CategoryEntity>,
    val transactions: List<FinanceTransactionEntity>,
    val familyTransactions: List<FamilyTransactionEntity>,
    val batches: List<FamilyImportBatchEntity>,
    val rates: List<ExchangeRateEntity>,
    val goal: SavingsGoalEntity?,
)

/** Run before changing any persistent state; callers must restore in a database transaction. */
fun validateFullBackup(snapshot: FullBackupSnapshot) {
    fun date(value: String) = LocalDate.parse(value)
    fun currency(value: String) = require(value in SupportedCurrencies.codes)
    // CSV imports historically accepted every UUID representation parsed by UUID.fromString.
    // Preserve the exact stored ID so previously accepted records remain exportable.
    fun uuid(value: String) { UUID.fromString(value) }
    fun validTransaction(id: String, owner: String, type: String, day: String, amount: Long, code: String, comment: String) {
        uuid(id); require(owner.isNotBlank()); require(type == "INCOME" || type == "EXPENSE")
        date(day); require(amount > 0); currency(code); require(IncomeComment.isValid(comment))
    }
    snapshot.profile?.let { profile ->
        require(profile.id == 1 && profile.name.isNotBlank() && profile.languageTag in setOf("en", "ru"))
        currency(profile.baseCurrency)
    }
    require(snapshot.categories.map { it.id }.distinct().size == snapshot.categories.size)
    snapshot.categories.forEach { require(it.id.isNotBlank() && (!it.nameEn.isNullOrBlank() || !it.nameRu.isNullOrBlank())) }
    val categories = snapshot.categories.mapTo(mutableSetOf()) { it.id }
    require(snapshot.transactions.map { it.id }.distinct().size == snapshot.transactions.size)
    snapshot.transactions.forEach { row ->
        validTransaction(row.id, row.owner, row.type, row.date, row.minorUnits, row.currencyCode, row.comment)
        require(row.createdOrder >= 0)
        if (row.categoryId != null) require(row.categoryId in categories)
        if (row.type == "INCOME") require(row.categoryId == null)
    }
    require(snapshot.batches.map { it.id }.distinct().size == snapshot.batches.size)
    snapshot.batches.forEach { uuid(it.id); require(it.recordCount >= 0) }
    val batches = snapshot.batches.mapTo(mutableSetOf()) { it.id }
    require(snapshot.familyTransactions.map { it.id }.distinct().size == snapshot.familyTransactions.size)
    snapshot.familyTransactions.forEach { row ->
        validTransaction(row.id, row.owner, row.type, row.date, row.minorUnits, row.currencyCode, row.comment)
        currency(row.importBaseCurrency)
        if (row.importRate != null) require(BigDecimal(row.importRate) > BigDecimal.ZERO)
        if (row.batchId != null) require(row.batchId in batches)
    }
    require(snapshot.rates.map { listOf(it.rangeStart, it.rangeEndExclusive, it.baseCurrency, it.sourceCurrency) }.distinct().size == snapshot.rates.size)
    snapshot.rates.forEach { rate ->
        require(date(rate.rangeStart) < date(rate.rangeEndExclusive)); currency(rate.baseCurrency); currency(rate.sourceCurrency)
        require(BigDecimal(rate.rateToBase) > BigDecimal.ZERO)
    }
    snapshot.goal?.let { goal ->
        require(goal.id == 1 && goal.targetMinorUnits > 0 && goal.savedMinorUnits >= 0)
        currency(goal.targetCurrency); currency(goal.savedCurrency)
    }
}
