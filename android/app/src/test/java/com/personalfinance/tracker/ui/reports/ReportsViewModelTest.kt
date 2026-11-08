package com.personalfinance.tracker.ui.reports

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.summary.SummaryService
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsViewModelTest {
    @Test fun validatesDateRangeAgainstFirstRecordButAllowsEmptyDays() {
        val first = LocalDate.of(2026, 3, 15)
        assertEquals(ReportDateValidation.BeforeFirst(first), validateReportDates("01-03-2026", "31-03-2026", first))
        assertEquals(ReportDateValidation.Valid, validateReportDates("16-03-2026", "20-03-2026", first))
        assertEquals(ReportDateValidation.InvalidDates, validateReportDates("31-02-2026", "20-03-2026", first))
        assertEquals(ReportDateValidation.InvalidRange, validateReportDates("21-03-2026", "20-03-2026", first))
        assertEquals(ReportDateValidation.Valid, validateReportDates("01-01-2026", "03-01-2026", null))
    }
    @Test fun archivedAndActiveSameNameCategoriesRemainDistinct() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveCategory(com.personalfinance.tracker.domain.model.Category("old-food", "Food", "Food", true, true, "dining"))
        repository.saveCategory(com.personalfinance.tracker.domain.model.Category("new-food", "Food", "Food", true, false, "groceries"))
        repository.saveTransaction(transaction("old", TransactionType.EXPENSE, "2026-10-01", 1000).copy(categoryId = "old-food"))
        repository.saveTransaction(transaction("new", TransactionType.EXPENSE, "2026-10-01", 2000).copy(categoryId = "new-food"))
        val model = ReportsViewModel(repository, ExchangeRateService(EmptyRateRepository), SummaryService(repository))
        val state = model.load(PeriodRange(LocalDate.of(2026,10,1), LocalDate.of(2026,11,1)), "USD", "en")
        assertEquals(2, state.categories.size)
        assertEquals(listOf(2000L, 1000L), state.categories.map { it.minorUnits })
        assertEquals(listOf("new-food", "old-food"), state.categories.map { it.categoryId })
        assertEquals(listOf("groceries", "dining"), state.categories.map { it.iconKey })
        assertEquals(listOf("Food", "Food"), state.categories.map { it.name })
        assertEquals(listOf(2000L, 1000L), state.categories.map { category -> category.details.sumOf { it.convertedMinorUnits } })
        assertEquals(listOf("new", "old"), state.categories.map { it.details.single().transaction.id })
    }
    @Test
    fun loadIncludesImmediatelyPrecedingEqualLengthPeriodForComparison() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveTransaction(transaction("current-income", TransactionType.INCOME, "2026-10-05", 10_000))
        repository.saveTransaction(transaction("previous-expense", TransactionType.EXPENSE, "2026-09-15", 4_000))
        val viewModel = ReportsViewModel(
            repository, ExchangeRateService(EmptyRateRepository), SummaryService(repository),
            today = LocalDate.of(2026, 10, 20),
        )

        val state = viewModel.load(
            PeriodRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21)), "USD", "en",
        )

        assertEquals(LocalDate.of(2026, 9, 11), state.previousRange?.start)
        assertEquals(LocalDate.of(2026, 10, 1), state.previousRange?.endExclusive)
        assertEquals(10_000L, state.summary?.income?.minorUnits)
        assertEquals(0L, state.summary?.expense?.minorUnits)
        assertEquals(0L, state.previousSummary?.income?.minorUnits)
        assertEquals(4_000L, state.previousSummary?.expense?.minorUnits)
    }

    @Test
    fun previousPeriodCurrencyRequiresRatesBeforeEitherSummaryIsShown() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveTransaction(transaction("current-income", TransactionType.INCOME, "2026-10-05", 10_000))
        repository.saveTransaction(transaction("previous-income", TransactionType.INCOME, "2026-09-15", 4_000)
            .copy(money = Money(4_000, "EUR")))
        val viewModel = ReportsViewModel(
            repository, ExchangeRateService(EmptyRateRepository), SummaryService(repository),
        )

        val state = viewModel.load(
            PeriodRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21)), "USD", "en",
        )

        assertEquals(setOf("EUR"), state.missingCurrencies)
        assertEquals(null, state.summary)
        assertEquals(null, state.previousSummary)
    }

    @Test
    fun precedingRangeStaysValidAtMonthEndAndDoesNotOverlapMultiMonthSelection() = runBlocking {
        val repository = FakeFinanceRepository()
        val viewModel = ReportsViewModel(
            repository, ExchangeRateService(EmptyRateRepository), SummaryService(repository),
        )
        listOf(
            PeriodRange(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 3, 31)),
            PeriodRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 1)),
        ).forEach { range ->
            val previous = viewModel.load(range, "USD", "en").previousRange!!
            val days = range.endExclusive.toEpochDay() - range.start.toEpochDay()
            assertEquals(range.start.minusDays(days), previous.start)
            assertEquals(range.start, previous.endExclusive)
            assertEquals(days, previous.endExclusive.toEpochDay() - previous.start.toEpochDay())
        }
    }

    @Test
    fun defaultRangeIncludesFutureDatedFirstTransactionAndIsValid() = runBlocking {
        val today = LocalDate.of(2026, 10, 20)
        val futureDate = LocalDate.of(2026, 11, 5)
        val repository = FakeFinanceRepository()
        repository.saveTransaction(
            FinanceTransaction(
                id = "8f3e77b3-3144-4b29-9e30-3616ad0e4ac4",
                owner = "Alex",
                type = TransactionType.EXPENSE,
                date = futureDate,
                money = Money(1250, "EUR"),
                categoryId = "groceries",
            ),
        )
        val viewModel = ReportsViewModel(
            repository = repository,
            exchangeRateService = ExchangeRateService(EmptyRateRepository),
            summaryService = SummaryService(repository),
            today = today,
        )

        val range = viewModel.defaultRange()

        assertEquals(futureDate, range.start)
        assertEquals(futureDate.plusDays(1), range.endExclusive)
        assertEquals(1, viewModel.load(range, "USD", "en").let { state ->
            repository.transactionsBetween(state.range, null).size
        })
    }
}

private fun transaction(id: String, type: TransactionType, date: String, minorUnits: Long) = FinanceTransaction(
    id = id, owner = "Alex", type = type, date = LocalDate.parse(date),
    money = Money(minorUnits, "USD"), categoryId = null,
)

private object EmptyRateRepository : RateRepository {
    override fun ratesFor(range: PeriodRange, baseCurrency: String): Flow<Map<String, BigDecimal>> = flowOf(emptyMap())

    override suspend fun saveRates(
        range: PeriodRange,
        baseCurrency: String,
        rates: Map<String, BigDecimal>,
    ) = Unit
}
