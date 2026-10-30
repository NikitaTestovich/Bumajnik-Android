package com.personalfinance.tracker.ui.home

import org.junit.Assert.*
import org.junit.Test

class HomeLayoutTest {
    @Test fun defaultsAndMissingBlocksPreserveChoices() {
        val result = normalizeHomeLayout(listOf(HomeBlockSetting(HomeBlock.LATEST, false), HomeBlockSetting(HomeBlock.LATEST, true)))
        assertEquals(HomeBlock.LATEST, result.first().id)
        assertFalse(result.first().visible)
        assertEquals(5, result.size)
        assertTrue(result.single { it.id == HomeBlock.EXPENSES }.visible)
    }
    @Test fun movePreservesVisibilityAndClampsBoundaries() {
        val initial = normalizeHomeLayout(emptyList())
        val moved = moveHomeBlock(initial, HomeBlock.SUMMARY, 2)
        assertEquals(HomeBlock.SUMMARY, moved[2].id)
        assertEquals(initial.associate { it.id to it.visible }, moved.associate { it.id to it.visible })
        assertEquals(initial, moveHomeBlock(initial, HomeBlock.SUMMARY, -1))
    }
    @Test fun allHiddenRemainsHiddenAndResetRestoresDefaults() {
        val hidden = normalizeHomeLayout(emptyList()).map { it.copy(visible = false) }
        assertTrue(normalizeHomeLayout(hidden).none { it.visible })
        assertEquals(5, normalizeHomeLayout(emptyList()).count { it.visible })
    }
    @Test fun explicitlyHiddenExpensesAndSavedOrderSurviveNormalization() {
        val saved = listOf(HomeBlockSetting(HomeBlock.EXPENSES, false), HomeBlockSetting(HomeBlock.GOAL, true))
        assertEquals(saved, normalizeHomeLayout(saved).take(2))
        assertFalse(normalizeHomeLayout(saved).first().visible)
    }
}
