package com.personalfinance.tracker.domain.goal

import org.junit.Assert.*
import org.junit.Test

class GoalProgressTest {
    @Test fun nearCompletionNeverClaimsAnUnreachedGoal() {
        assertEquals("99.4", GoalProgress.calculate(1000, 994).percent)
        assertEquals("99.5", GoalProgress.calculate(1000, 995).percent)
        assertEquals("99.9", GoalProgress.calculate(1000, 999).percent)
        assertEquals("100", GoalProgress.calculate(1000, 1000).percent)
    }
    @Test fun partialSegmentFractionsFollowActualSavings() {
        assertEquals(listOf(1f, .52f, 0f, 0f), GoalProgress.calculate(100, 38).segmentFractions)
        assertEquals(listOf(1f, 1f, .04f, 0f), GoalProgress.calculate(100, 51).segmentFractions)
    }
    @Test fun zeroAndPartialProgress() {
        assertEquals("0", GoalProgress.calculate(100, 0).percent)
        val progress = GoalProgress.calculate(1000, 249)
        assertEquals("24.9", progress.percent)
        assertEquals(1L, progress.nextMilestoneMinorUnits)
        assertEquals(25, progress.nextMilestonePercent)
        assertEquals(751L, progress.remainingMinorUnits)
    }
    @Test fun exactMilestonesAdvanceToNextThreshold() {
        for (saved in listOf(25L, 50L, 75L)) {
            val progress = GoalProgress.calculate(100, saved)
            assertEquals(saved.toString(), progress.percent)
            assertEquals(saved.toInt() + 25, progress.nextMilestonePercent)
            assertEquals(25L, progress.nextMilestoneMinorUnits)
        }
    }
    @Test fun fractionalThresholdRoundsUpWithoutFalseMilestone() {
        val progress = GoalProgress.calculate(101, 25)
        assertEquals("24.7", progress.percent)
        assertEquals(1L, progress.nextMilestoneMinorUnits)
        assertEquals(25, progress.nextMilestonePercent)
        assertEquals(50, GoalProgress.calculate(101, 26).nextMilestonePercent)
    }
    @Test fun completedAndOverTargetClampFillAndRemaining() {
        for (saved in listOf(100L, 150L, Long.MAX_VALUE)) {
            val progress = GoalProgress.calculate(100, saved)
            assertEquals("100", progress.percent)
            assertEquals(0L, progress.remainingMinorUnits)
            assertEquals(0L, progress.nextMilestoneMinorUnits)
            assertTrue(progress.completed)
            assertEquals(listOf(1f, 1f, 1f, 1f), progress.segmentFractions)
        }
    }
    @Test fun maximumValuesDoNotOverflowOrRoundUp() {
        val progress = GoalProgress.calculate(Long.MAX_VALUE, Long.MAX_VALUE - 1)
        assertEquals("99.9", progress.percent)
        assertEquals(1L, progress.nextMilestoneMinorUnits)
        assertEquals(1L, progress.remainingMinorUnits)
        assertFalse(progress.completed)
        assertEquals(2305843009213693952L, GoalProgress.calculate(Long.MAX_VALUE, 0).nextMilestoneMinorUnits)
    }
}
