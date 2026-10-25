package com.personalfinance.tracker.domain.goal

import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.summary.PeriodSummary
import java.math.BigDecimal
import java.math.RoundingMode

data class GoalForecast(
    val remainingMinorUnits: Long,
    val averageMonthlySurplusMinorUnits: Long?,
    val estimatedMonths: Int?,
    val status: GoalForecastStatus,
)

enum class GoalForecastStatus { READY, NO_COMPLETED_MONTHS, NO_POSITIVE_SURPLUS }

class GoalForecastService {
    fun calculate(goal: SavingsGoal, completedMonthSummaries: List<PeriodSummary>): GoalForecast {
        val remaining = (goal.target.minorUnits - goal.saved.minorUnits).coerceAtLeast(0L)
        if (remaining == 0L) {
            return GoalForecast(0, null, 0, GoalForecastStatus.READY)
        }
        if (completedMonthSummaries.isEmpty()) {
            return GoalForecast(remaining, null, null, GoalForecastStatus.NO_COMPLETED_MONTHS)
        }

        completedMonthSummaries.forEach { summary ->
            require(summary.income.currencyCode == goal.target.currencyCode) {
                "Completed month income must use the goal currency"
            }
            require(summary.expense.currencyCode == goal.target.currencyCode) {
                "Completed month expenses must use the goal currency"
            }
        }
        val totalSurplus = completedMonthSummaries.fold(0L) { total, summary ->
            Math.addExact(total, summary.balance.minorUnits)
        }
        val averageSurplus = BigDecimal.valueOf(totalSurplus)
            .divide(BigDecimal.valueOf(completedMonthSummaries.size.toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()
        if (totalSurplus <= 0L) {
            return GoalForecast(remaining, averageSurplus, null, GoalForecastStatus.NO_POSITIVE_SURPLUS)
        }
        val months = BigDecimal.valueOf(remaining)
            .multiply(BigDecimal.valueOf(completedMonthSummaries.size.toLong()))
            .divide(BigDecimal.valueOf(totalSurplus), 0, RoundingMode.CEILING)
            .intValueExact()
        return GoalForecast(remaining, averageSurplus, months, GoalForecastStatus.READY)
    }
}
