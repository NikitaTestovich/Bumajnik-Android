package com.personalfinance.tracker.ui.goals

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.summary.SummaryService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalViewModelTest {
    @Test
    fun rangeUsesLatestThreeCompleteMonths() {
        assertEquals(
            PeriodRange(LocalDate.parse("2026-07-01"), LocalDate.parse("2026-10-01")),
            GoalViewModel.forecastRange(LocalDate.parse("2025-02-18"), LocalDate.parse("2026-10-20")),
        )
    }

    @Test
    fun rangeUsesOnlyAvailableCompleteMonths() {
        assertEquals(
            PeriodRange(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-10-01")),
            GoalViewModel.forecastRange(LocalDate.parse("2026-08-12"), LocalDate.parse("2026-10-20")),
        )
    }

    @Test
    fun noCompleteMonthReturnsNoForecastRange() {
        assertNull(GoalViewModel.forecastRange(LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-20")))
        assertNull(GoalViewModel.forecastRange(null, LocalDate.parse("2026-10-20")))
    }

    @Test
    fun loadUsesOneSavedRateSetForAllCompletedMonths() = runBlocking {
        val repository = FakeFinanceRepository()
        val firstDate = LocalDate.parse("2026-08-10")
        repository.saveTransaction(transaction(firstDate, TransactionType.INCOME, 20_000, "EUR"))
        repository.saveTransaction(transaction(LocalDate.parse("2026-08-12"), TransactionType.EXPENSE, 10_000, "EUR"))
        repository.saveTransaction(transaction(LocalDate.parse("2026-09-10"), TransactionType.INCOME, 16_000, "EUR"))
        repository.saveTransaction(transaction(LocalDate.parse("2026-09-12"), TransactionType.EXPENSE, 10_000, "EUR"))
        repository.saveGoal(SavingsGoal(Money(10_000, "USD"), Money(0, "USD")))
        val rates = TestRateRepository()
        val viewModel = GoalViewModel(repository, rates, SummaryService(repository), LocalDate.parse("2026-10-20"))

        val beforeRate = viewModel.load("USD")
        assertEquals(setOf("EUR"), beforeRate.missingCurrencies)
        assertNull(beforeRate.forecast)
        rates.saveRates(beforeRate.forecastPeriod!!, "USD", mapOf("EUR" to BigDecimal("0.5")))

        val afterRate = viewModel.load("USD")

        assertEquals(emptySet<String>(), afterRate.missingCurrencies)
        assertEquals(PeriodRange(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-10-01")), afterRate.forecastPeriod)
        assertEquals(4_000L, afterRate.forecast?.averageMonthlySurplusMinorUnits)
        assertEquals(3, afterRate.forecast?.estimatedMonths)
        assertEquals(2, afterRate.completedMonthCount)
    }

    @Test
    fun oldGoalCurrencyRequestsPeriodRateAndKeepsOriginalGoalUntilRateIsAvailable() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveTransaction(transaction(LocalDate.parse("2026-09-01"), TransactionType.INCOME, 100, "USD"))
        val originalGoal = SavingsGoal(Money(50_000, "EUR"), Money(10_000, "EUR"))
        repository.saveGoal(originalGoal)
        val viewModel = GoalViewModel(repository, TestRateRepository(), SummaryService(repository), LocalDate.parse("2026-10-20"))

        val state = viewModel.load("USD")

        assertEquals(setOf("EUR"), state.missingCurrencies)
        assertEquals(true, state.goalCurrencyChanged)
        assertNull(state.forecast)
        assertEquals(originalGoal, repository.observeGoal().first())
    }

    @Test
    fun convertsOldGoalUsingSavedForecastPeriodRateAndPersistsBaseCurrency() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveTransaction(transaction(LocalDate.parse("2026-09-01"), TransactionType.INCOME, 100, "USD"))
        repository.saveGoal(SavingsGoal(Money(50_000, "EUR"), Money(10_000, "EUR")))
        val rates = TestRateRepository()
        val viewModel = GoalViewModel(repository, rates, SummaryService(repository), LocalDate.parse("2026-10-20"))
        val period = GoalViewModel.forecastRange(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-20"))!!
        rates.saveRates(period, "USD", mapOf("EUR" to BigDecimal("0.5")))

        val state = viewModel.load("USD")

        val convertedGoal = repository.observeGoal().first()
        assertEquals(SavingsGoal(Money(25_000, "USD"), Money(5_000, "USD")), convertedGoal)
        assertEquals(false, state.goalCurrencyChanged)
        assertEquals(emptySet<String>(), state.missingCurrencies)
        assertEquals("USD", state.goal?.target?.currencyCode)
    }

    @Test
    fun changedGoalCurrencyWithoutForecastPeriodDoesNotMutateSavedValues() = runBlocking {
        val repository = FakeFinanceRepository()
        val originalGoal = SavingsGoal(Money(50_000, "EUR"), Money(10_000, "EUR"))
        repository.saveGoal(originalGoal)
        val viewModel = GoalViewModel(repository, TestRateRepository(), SummaryService(repository), LocalDate.parse("2026-10-20"))

        val state = viewModel.load("USD")

        assertNull(state.forecastPeriod)
        assertEquals(true, state.goalCurrencyChanged)
        assertEquals(originalGoal, repository.observeGoal().first())
    }

    private fun transaction(date: LocalDate, type: TransactionType, amount: Long, currency: String) = FinanceTransaction(
        id = UUID.randomUUID().toString(), owner = "Alex", type = type, date = date,
        money = Money(amount, currency), categoryId = null,
    )

    private class TestRateRepository : RateRepository {
        private val flows = mutableMapOf<Pair<PeriodRange, String>, MutableStateFlow<Map<String, BigDecimal>>>()
        override fun ratesFor(range: PeriodRange, baseCurrency: String): Flow<Map<String, BigDecimal>> =
            flows.getOrPut(range to baseCurrency) { MutableStateFlow(emptyMap()) }

        override suspend fun saveRates(range: PeriodRange, baseCurrency: String, rates: Map<String, BigDecimal>) {
            flows.getOrPut(range to baseCurrency) { MutableStateFlow(emptyMap()) }.value = rates
        }
    }
}
