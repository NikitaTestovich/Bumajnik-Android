package com.personalfinance.tracker.ui.home

import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.summary.PeriodSummary
import com.personalfinance.tracker.domain.summary.SummaryService
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import com.personalfinance.tracker.ui.charts.ExpenseSlice
import com.personalfinance.tracker.ui.charts.expenseBreakdown

data class HomeUiState(
    val summary: PeriodSummary?,
    val latestTransaction: FinanceTransaction?,
    val goal: SavingsGoal?,
    val missingCurrencies: Set<String>,
    val expenses: List<ExpenseSlice> = emptyList(),
)

class HomeViewModel(
    private val repository: FinanceRepository,
    private val exchangeRateService: ExchangeRateService,
    private val summaryService: SummaryService,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    fun currentMonthRange(): PeriodRange {
        val firstDay = today().withDayOfMonth(1)
        return PeriodRange(firstDay, firstDay.plusMonths(1))
    }

    suspend fun load(baseCurrency: String, languageTag: String = "en"): HomeUiState {
        val range = currentMonthRange()
        val periodTransactions = repository.transactionsBetween(range, null)
        val sourceCurrencies = periodTransactions.mapTo(mutableSetOf()) { it.money.currencyCode }
        val rates = exchangeRateService.getRates(range, baseCurrency, sourceCurrencies)
        val missing = sourceCurrencies - rates.keys
        val summary = if (missing.isEmpty()) {
            summaryService.summarize(range, baseCurrency, rates)
        } else {
            null
        }
        return HomeUiState(
            summary = summary,
            latestTransaction = repository.observeTransactions(null).first().firstOrNull(),
            goal = repository.observeGoal().first(),
            missingCurrencies = missing,
            expenses = if (missing.isEmpty()) expenseBreakdown(periodTransactions, repository.observeCategories().first(), languageTag, baseCurrency, rates) else emptyList(),
        )
    }

    suspend fun saveRates(baseCurrency: String, enteredRates: Map<String, BigDecimal>) {
        val range = currentMonthRange()
        val sourceCurrencies = repository.transactionsBetween(range, null)
            .mapTo(mutableSetOf()) { it.money.currencyCode }
        val currentRates = exchangeRateService.getRates(range, baseCurrency, sourceCurrencies)
        exchangeRateService.saveRates(range, baseCurrency, currentRates + enteredRates)
    }
}
