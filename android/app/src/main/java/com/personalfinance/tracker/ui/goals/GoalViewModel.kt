package com.personalfinance.tracker.ui.goals

import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.goal.GoalForecast
import com.personalfinance.tracker.domain.goal.GoalForecastService
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.summary.PeriodSummary
import com.personalfinance.tracker.domain.summary.SummaryService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.flow.first

data class GoalUiState(
    val goal: SavingsGoal?,
    val forecastPeriod: PeriodRange?,
    val forecast: GoalForecast?,
    val missingCurrencies: Set<String>,
    val completedMonthCount: Int,
    val goalCurrencyChanged: Boolean,
)

class GoalViewModel(
    private val financeRepository: FinanceRepository,
    private val rateRepository: RateRepository,
    private val summaryService: SummaryService,
    private val today: LocalDate = LocalDate.now(),
    private val forecastService: GoalForecastService = GoalForecastService(),
) {
    suspend fun load(baseCurrency: String): GoalUiState {
        val goal = financeRepository.observeGoal().first()
        val firstTransactionDate = financeRepository.observeTransactions(null).first().minOfOrNull { it.date }
        val range = forecastRange(firstTransactionDate, today)
        val goalCurrencyChanged = goal != null && goal.target.currencyCode != baseCurrency
        if (goal == null) {
            return GoalUiState(null, range, null, emptySet(), range?.let(::monthCount) ?: 0, false)
        }
        if (range == null) {
            return GoalUiState(
                goal,
                null,
                if (goalCurrencyChanged) null else forecastService.calculate(goal, emptyList()),
                emptySet(),
                0,
                goalCurrencyChanged,
            )
        }

        val transactions = financeRepository.transactionsBetween(range, null)
        val sourceCurrencies = transactions.asSequence()
            .map { it.money.currencyCode }
            .filter { it != baseCurrency }
            .toMutableSet()
        if (goalCurrencyChanged) sourceCurrencies += goal.target.currencyCode
        val savedRates = rateRepository.ratesFor(range, baseCurrency).first()
        val missing = sourceCurrencies - savedRates.keys
        if (missing.isNotEmpty()) {
            return GoalUiState(goal, range, null, missing, monthCount(range), goalCurrencyChanged)
        }
        val rates = savedRates + (baseCurrency to BigDecimal.ONE)
        val normalizedGoal = if (goalCurrencyChanged) convertGoal(goal, baseCurrency, rates) else goal
        if (normalizedGoal == null) {
            return GoalUiState(goal, range, null, emptySet(), monthCount(range), true)
        }
        if (goalCurrencyChanged) financeRepository.saveGoal(normalizedGoal)
        val monthlySummaries = monthRanges(range).map { month ->
            summaryService.summarize(month, baseCurrency, rates)
        }
        return GoalUiState(
            goal = normalizedGoal,
            forecastPeriod = range,
            forecast = forecastService.calculate(normalizedGoal, monthlySummaries),
            missingCurrencies = emptySet(),
            completedMonthCount = monthlySummaries.size,
            goalCurrencyChanged = false,
        )
    }

    private fun convertGoal(
        goal: SavingsGoal,
        baseCurrency: String,
        rates: Map<String, BigDecimal>,
    ): SavingsGoal? = runCatching {
        val conversionRate = rates.getValue(goal.target.currencyCode)
        fun convert(minorUnits: Long) = BigDecimal.valueOf(minorUnits)
            .multiply(conversionRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val target = convert(goal.target.minorUnits)
        require(target > 0L) { "Converted goal target must remain positive" }
        SavingsGoal(
            target = Money(target, baseCurrency),
            saved = Money(convert(goal.saved.minorUnits), baseCurrency),
        )
    }.getOrNull()

    suspend fun saveGoal(targetMinorUnits: Long, savedMinorUnits: Long, baseCurrency: String) {
        financeRepository.saveGoal(
            SavingsGoal(
                target = Money(targetMinorUnits, baseCurrency),
                saved = Money(savedMinorUnits, baseCurrency),
            ),
        )
    }

    suspend fun saveRates(range: PeriodRange, baseCurrency: String, enteredRates: Map<String, BigDecimal>) {
        val current = rateRepository.ratesFor(range, baseCurrency).first()
        rateRepository.saveRates(range, baseCurrency, (current + enteredRates).filterKeys { it != baseCurrency })
    }

    companion object {
        fun forecastRange(firstTransactionDate: LocalDate?, today: LocalDate): PeriodRange? {
            val endExclusive = today.withDayOfMonth(1)
            val firstMonth = firstTransactionDate?.withDayOfMonth(1) ?: return null
            if (firstMonth >= endExclusive) return null
            val latestThreeMonthsStart = endExclusive.minusMonths(3)
            val start = if (firstMonth > latestThreeMonthsStart) firstMonth else latestThreeMonthsStart
            return PeriodRange(start, endExclusive)
        }
    }
}

private fun monthRanges(range: PeriodRange): List<PeriodRange> {
    val result = mutableListOf<PeriodRange>()
    var start = range.start
    while (start < range.endExclusive) {
        val endExclusive = start.plusMonths(1)
        result += PeriodRange(start, endExclusive)
        start = endExclusive
    }
    return result
}

private fun monthCount(range: PeriodRange): Int =
    (range.start.year * 12 + range.start.monthValue).let { start ->
        (range.endExclusive.year * 12 + range.endExclusive.monthValue) - start
    }
