package com.personalfinance.tracker.ui.charts

import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.summary.SummaryService
import com.personalfinance.tracker.ui.home.HomeViewModel

class ExpenseBreakdownTest {
    @Test fun zeroConvertedExpensesRetainCardsWithoutInventingShares() {
        val rates = mapOf("EUR" to BigDecimal("0.1"))
        val mixed = expenseBreakdown(listOf(expense("tiny", 1, "EUR"), expense("large", 100)), emptyList(), "en", "USD", rates)
        assertEquals(listOf("large", "tiny"), mixed.map { it.categoryId })
        assertEquals(listOf(100L, 0L), mixed.map { it.minorUnits })
        assertEquals(listOf(1.0, 0.0), mixed.map { it.share })
        val zero = expenseBreakdown(listOf(expense("a", 1, "EUR"), expense("b", 2, "EUR")), emptyList(), "en", "USD", rates)
        assertEquals(2, zero.size)
        assertTrue(zero.all { it.minorUnits == 0L && it.share == 0.0 })
    }
    @Test fun homeTotalsAndMonthRolloverUseSameContext() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveTransaction(expense("a", 1, "EUR"))
        repository.saveTransaction(expense("b", 1, "EUR"))
        val rates = object : RateRepository {
            override fun ratesFor(range: PeriodRange, baseCurrency: String) = flowOf(mapOf("EUR" to BigDecimal("1.5")))
            override suspend fun saveRates(range: PeriodRange, baseCurrency: String, rates: Map<String, BigDecimal>) = Unit
        }
        var day = LocalDate.of(2026, 10, 31)
        val home = HomeViewModel(repository, ExchangeRateService(rates), SummaryService(repository)) { day }
        val october = home.load("USD")
        assertEquals(october.summary!!.expense.minorUnits, october.expenses.sumOf { it.minorUnits })
        day = day.plusDays(1)
        assertEquals(LocalDate.of(2026, 11, 1), home.currentMonthRange().start)
        assertTrue(home.load("USD").expenses.isEmpty())
    }
    private fun expense(id: String, amount: Long, currency: String = "USD") = FinanceTransaction(id, "Demo", TransactionType.EXPENSE, LocalDate.of(2026, 10, 1), Money(amount, currency), id)
    @Test fun sharesAndArchivedIdentityArePreserved() {
        val rows = listOf(expense("a", 600), expense("b", 360), expense("c", 240))
        val slices = expenseBreakdown(rows, listOf(Category("a", "A", "A", true, true)), "en", "USD", emptyMap())
        assertEquals(listOf(.5, .3, .2), slices.map { it.share })
        assertEquals("a", slices.first().categoryId)
    }
    @Test fun conversionRoundsPerRecordAndMissingRatesFail() {
        val rows = listOf(expense("a", 1, "EUR"), expense("b", 1, "EUR"))
        assertEquals(4L, expenseBreakdown(rows, emptyList(), "en", "USD", mapOf("EUR" to BigDecimal("1.5"))).sumOf { it.minorUnits })
        assertThrows(IllegalArgumentException::class.java) { expenseBreakdown(rows, emptyList(), "en", "USD", emptyMap()) }
    }
    @Test fun emptySingleTinyAndOverflow() {
        assertTrue(expenseBreakdown(emptyList(), emptyList(), "en", "USD", emptyMap()).isEmpty())
        assertEquals(1.0, expenseBreakdown(listOf(expense("a", 1)), emptyList(), "en", "USD", emptyMap()).single().share, 0.0)
        val tiny = expenseBreakdown((1..100).map { expense(it.toString(), 1) }, emptyList(), "en", "USD", emptyMap())
        assertEquals(100, tiny.size)
        assertEquals(.01, tiny.first().share, 0.0)
        assertThrows(ArithmeticException::class.java) { expenseBreakdown(listOf(expense("a", Long.MAX_VALUE), expense("b", 1)), emptyList(), "en", "USD", emptyMap()) }
    }
}
