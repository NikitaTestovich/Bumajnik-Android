package com.personalfinance.tracker.ui.reports

import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.summary.PeriodSummary
import com.personalfinance.tracker.domain.summary.SummaryService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.flow.first

data class CategoryExpense(val name: String, val minorUnits: Long, val categoryId: String? = null, val iconKey: String = "other")

enum class ReportError { BeforeFirstTransaction, InvalidRange }

data class ReportUiState(
    val range: PeriodRange,
    val firstTransactionDate: LocalDate?,
    val summary: PeriodSummary?,
    val categories: List<CategoryExpense>,
    val missingCurrencies: Set<String>,
    val error: ReportError? = null,
    val previousRange: PeriodRange? = null,
    val previousSummary: PeriodSummary? = null,
)

class ReportsViewModel(
    private val repository: FinanceRepository,
    private val exchangeRateService: ExchangeRateService,
    private val summaryService: SummaryService,
    private val today: LocalDate = LocalDate.now(),
) {
    suspend fun firstTransactionDate(): LocalDate? =
        repository.observeTransactions(null).first().minOfOrNull { it.date }

    suspend fun defaultRange(): PeriodRange {
        val first = firstTransactionDate()
        val firstOfMonth = today.withDayOfMonth(1)
        val start = if (first != null && first > firstOfMonth) first else firstOfMonth
        val defaultEndExclusive = today.plusDays(1)
        val endExclusive = if (start < defaultEndExclusive) defaultEndExclusive else start.plusDays(1)
        return PeriodRange(start, endExclusive)
    }

    suspend fun load(
        range: PeriodRange,
        baseCurrency: String,
        languageTag: String,
    ): ReportUiState {
        val first = firstTransactionDate()
        if (first != null && range.start < first) {
            return ReportUiState(range, first, null, emptyList(), emptySet(), ReportError.BeforeFirstTransaction)
        }
        val transactions = repository.transactionsBetween(range, null)
        val durationDays = range.endExclusive.toEpochDay() - range.start.toEpochDay()
        val previousRange = PeriodRange(range.start.minusDays(durationDays), range.start)
        val previousTransactions = repository.transactionsBetween(previousRange, null)
        val currencies = (transactions + previousTransactions).mapTo(mutableSetOf()) { it.money.currencyCode }
        val rates = exchangeRateService.getRates(range, baseCurrency, currencies)
        val missing = currencies - rates.keys
        if (missing.isNotEmpty()) {
            return ReportUiState(range, first, null, emptyList(), missing)
        }

        val summary = summaryService.summarize(range, baseCurrency, rates)
        val previousSummary = summaryService.summarize(previousRange, baseCurrency, rates)
        val categoriesById = repository.observeCategories().first().associateBy { it.id }
        val expensesByCategory = transactions.asSequence()
            .filter { it.type.name == "EXPENSE" }
            .groupBy { it.categoryId }
            .map { (categoryId, entries) ->
                val category = categoriesById[categoryId]
                val name = category?.let { if (languageTag == "ru") it.nameRu ?: it.nameEn else it.nameEn ?: it.nameRu }.orEmpty()
                val total = entries.fold(0L) { acc, transaction ->
                    val rate = rates.getValue(transaction.money.currencyCode)
                    val converted = BigDecimal.valueOf(transaction.money.minorUnits)
                        .multiply(rate)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact()
                    Math.addExact(acc, converted)
                }
                CategoryExpense(name.ifBlank { if (languageTag == "ru") "Без категории" else "Uncategorized" }, total,
                    categoryId, com.personalfinance.tracker.domain.model.CategoryIcons.resolve(category?.iconKey ?: "other"))
            }
            .sortedWith(compareByDescending<CategoryExpense> { it.minorUnits }.thenBy { it.categoryId.orEmpty() })

        return ReportUiState(
            range, first, summary, expensesByCategory, emptySet(),
            previousRange = previousRange,
            previousSummary = previousSummary,
        )
    }

    suspend fun saveRates(range: PeriodRange, baseCurrency: String, rates: Map<String, BigDecimal>) {
        exchangeRateService.saveRates(range, baseCurrency, rates)
    }
}
