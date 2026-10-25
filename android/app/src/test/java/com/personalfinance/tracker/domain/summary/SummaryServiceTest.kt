package com.personalfinance.tracker.domain.summary

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SummaryServiceTest {
    private val repository = FakeFinanceRepository()
    private val service = SummaryService(repository)
    private val range = PeriodRange(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-04-01"))

    @Test
    fun aggregatesBaseCurrencyIncomeExpensesAndBalance() = runBlocking {
        repository.saveTransaction(transaction("2026-03-03", TransactionType.INCOME, 15_000, "USD"))
        repository.saveTransaction(transaction("2026-03-04", TransactionType.EXPENSE, 3_000, "USD"))

        val summary = service.summarize(range, "USD", mapOf("USD" to BigDecimal.ONE))

        assertEquals(Money(15_000, "USD"), summary.income)
        assertEquals(Money(3_000, "USD"), summary.expense)
        assertEquals(Money(12_000, "USD"), summary.balance)
    }

    @Test
    fun convertsMultipleForeignCurrenciesUsingRatesToBase() = runBlocking {
        repository.saveTransaction(transaction("2026-03-03", TransactionType.INCOME, 10_000, "EUR"))
        repository.saveTransaction(transaction("2026-03-04", TransactionType.INCOME, 200_000, "RUB"))

        val summary = service.summarize(
            range,
            "USD",
            mapOf("EUR" to BigDecimal("1.10"), "RUB" to BigDecimal("0.01")),
        )

        assertEquals(Money(13_000, "USD"), summary.income)
    }

    @Test
    fun roundsConvertedMinorUnitsHalfUp() = runBlocking {
        repository.saveTransaction(transaction("2026-03-03", TransactionType.INCOME, 100, "EUR"))

        val summary = service.summarize(range, "USD", mapOf("EUR" to BigDecimal("0.335")))

        assertEquals(Money(34, "USD"), summary.income)
    }

    @Test
    fun usesInclusiveStartAndExclusiveEnd() = runBlocking {
        repository.saveTransaction(transaction("2026-03-01", TransactionType.INCOME, 100, "USD"))
        repository.saveTransaction(transaction("2026-03-31", TransactionType.INCOME, 200, "USD"))
        repository.saveTransaction(transaction("2026-04-01", TransactionType.INCOME, 400, "USD"))

        val summary = service.summarize(range, "USD", emptyMap<String, BigDecimal>())

        assertEquals(Money(300, "USD"), summary.income)
    }

    @Test
    fun summarizingDoesNotChangeStoredSourceAmounts() = runBlocking {
        val source = transaction("2026-03-03", TransactionType.INCOME, 10_000, "EUR")
        repository.saveTransaction(source)

        service.summarize(range, "USD", mapOf("EUR" to BigDecimal("1.25")))

        assertEquals(source, repository.transactionsBetween(range, null).single())
    }

    @Test(expected = MissingExchangeRatesException::class)
    fun refusesToShowTotalsWhenForeignCurrencyRateIsMissing() {
        runBlocking {
            repository.saveTransaction(transaction("2026-03-03", TransactionType.INCOME, 100, "EUR"))

            service.summarize(range, "USD", emptyMap())
        }
    }

    @Test
    fun exchangeRateServiceReusesSavedRatesForTheSamePeriod() = runBlocking {
        val rates = FakeRateRepository()
        val service = ExchangeRateService(rates)
        service.saveRates(range, "USD", mapOf("EUR" to BigDecimal("1.25")))

        val restored = service.getRates(range, "USD", setOf("EUR", "RUB"))

        assertEquals(BigDecimal.ONE, restored["USD"])
        assertEquals(BigDecimal("1.25"), restored["EUR"])
        assertFalse(restored.containsKey("RUB"))
    }

    @Test
    fun savingAnotherCurrencyKeepsPreviouslySavedRatesForThePeriod() = runBlocking {
        val rates = FakeRateRepository()
        val service = ExchangeRateService(rates)
        service.saveRates(range, "USD", mapOf("EUR" to BigDecimal("1.25")))
        service.saveRates(range, "USD", mapOf("RUB" to BigDecimal("0.01")))

        val restored = service.getRates(range, "USD", setOf("EUR", "RUB"))

        assertEquals(BigDecimal("1.25"), restored["EUR"])
        assertEquals(BigDecimal("0.01"), restored["RUB"])
    }

    private fun transaction(
        date: String,
        type: TransactionType,
        minorUnits: Long,
        currency: String,
    ) = FinanceTransaction(
        id = UUID.randomUUID().toString(),
        owner = "Test",
        type = type,
        date = LocalDate.parse(date),
        money = Money(minorUnits, currency),
        categoryId = null,
    )

    private class FakeRateRepository : RateRepository {
        private val values = mutableMapOf<Triple<PeriodRange, String, String>, BigDecimal>()
        override fun ratesFor(range: PeriodRange, baseCurrency: String): Flow<Map<String, BigDecimal>> =
            MutableStateFlow(values.filterKeys { it.first == range && it.second == baseCurrency }
                .mapKeys { it.key.third })

        override suspend fun saveRates(
            range: PeriodRange,
            baseCurrency: String,
            rates: Map<String, BigDecimal>,
        ) {
            values.keys.removeAll { it.first == range && it.second == baseCurrency }
            rates.forEach { (currency, rate) -> values[Triple(range, baseCurrency, currency)] = rate }
        }
    }
}
