package com.personalfinance.tracker.domain.goal

import java.math.BigDecimal
import java.math.RoundingMode

data class GoalProgress(
    val percent: String,
    val remainingMinorUnits: Long,
    val nextMilestonePercent: Int,
    val nextMilestoneMinorUnits: Long,
    val completed: Boolean,
    val segmentFractions: List<Float>,
) {
    companion object {
        fun calculate(target: Long, saved: Long): GoalProgress {
            require(target > 0 && saved >= 0)
            val bounded = saved.coerceAtMost(target)
            val denominator = BigDecimal.valueOf(target)
            val ratio = BigDecimal.valueOf(bounded).divide(denominator, 20, RoundingMode.DOWN)
            val next = listOf(25, 50, 75, 100).firstOrNull { percent ->
                BigDecimal.valueOf(bounded).multiply(BigDecimal.valueOf(100)) <
                    denominator.multiply(BigDecimal.valueOf(percent.toLong()))
            } ?: 100
            val threshold = denominator.multiply(BigDecimal.valueOf(next.toLong()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING).longValueExact()
            return GoalProgress(
                percent = BigDecimal.valueOf(bounded).multiply(BigDecimal.valueOf(100))
                    .divide(denominator, 1, RoundingMode.DOWN).stripTrailingZeros().toPlainString(),
                remainingMinorUnits = target - bounded,
                nextMilestonePercent = next,
                nextMilestoneMinorUnits = (threshold - bounded).coerceAtLeast(0),
                completed = saved >= target,
                segmentFractions = List(4) { index ->
                    (ratio.toDouble() * 4 - index).coerceIn(0.0, 1.0).toFloat()
                },
            )
        }
    }
}
