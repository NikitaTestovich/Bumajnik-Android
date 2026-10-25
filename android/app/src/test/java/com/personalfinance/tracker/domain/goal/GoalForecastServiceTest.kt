package com.personalfinance.tracker.domain.goal

import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.summary.PeriodSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalForecastServiceTest {
    private val service = GoalForecastService()

    @Test
    fun noCompletedMonthsHasNoForecast() {
        val result = service.calculate(goal(target = 10_000, saved = 0), emptyList())

        assertEquals(GoalForecastStatus.NO_COMPLETED_MONTHS, result.status)
        assertEquals(10_000L, result.remainingMinorUnits)
        assertNull(result.averageMonthlySurplusMinorUnits)
        assertNull(result.estimatedMonths)
    }

    @Test
    fun oneCompletedMonthSetsAverageToThatMonthSurplus() {
        val result = service.calculate(goal(10_000, 1_000), listOf(summary(income = 7_000, expense = 6_000)))

        assertEquals(GoalForecastStatus.READY, result.status)
        assertEquals(9_000L, result.remainingMinorUnits)
        assertEquals(1_000L, result.averageMonthlySurplusMinorUnits)
        assertEquals(9, result.estimatedMonths)
    }

    @Test
    fun averagesThreeCompletedMonths() {
        val result = service.calculate(
            goal(20_000, 0),
            listOf(
                summary(7_000, 6_000),
                summary(8_000, 6_000),
                summary(10_000, 7_000),
            ),
        )

        assertEquals(2_000L, result.averageMonthlySurplusMinorUnits)
        assertEquals(10, result.estimatedMonths)
    }

    @Test
    fun roundsEstimatedMonthsUpToNextWholeMonth() {
        val result = service.calculate(goal(1_000, 0), listOf(summary(400, 0)))

        assertEquals(3, result.estimatedMonths)
    }

    @Test
    fun estimatesFromExactAverageInsteadOfRoundedDisplayAverage() {
        val result = service.calculate(
            goal(301, 0),
            listOf(summary(100, 0), summary(100, 0), summary(101, 0)),
        )

        assertEquals(100L, result.averageMonthlySurplusMinorUnits)
        assertEquals(3, result.estimatedMonths)
    }

    @Test
    fun accountsForAlreadySavedAmountAndReportsGoalReached() {
        val result = service.calculate(goal(10_000, 10_000), emptyList())

        assertEquals(GoalForecastStatus.READY, result.status)
        assertEquals(0L, result.remainingMinorUnits)
        assertEquals(0, result.estimatedMonths)
    }

    @Test
    fun zeroOrNegativeAverageSurplusHasNoFinishEstimate() {
        listOf(
            listOf(summary(5_000, 5_000)),
            listOf(summary(4_000, 5_000), summary(5_000, 5_000)),
        ).forEach { months ->
            val result = service.calculate(goal(10_000, 0), months)
            assertEquals(GoalForecastStatus.NO_POSITIVE_SURPLUS, result.status)
            assertNull(result.estimatedMonths)
        }
    }

    private fun goal(target: Long, saved: Long) = SavingsGoal(Money(target, "USD"), Money(saved, "USD"))

    private fun summary(income: Long, expense: Long) = PeriodSummary(
        income = Money(income, "USD"),
        expense = Money(expense, "USD"),
        balance = Money(income - expense, "USD"),
    )
}
