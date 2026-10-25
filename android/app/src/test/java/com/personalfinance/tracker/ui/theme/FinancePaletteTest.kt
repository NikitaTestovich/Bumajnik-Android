package com.personalfinance.tracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancePaletteTest {
    @Test
    fun `material cards and navigation use finance containers rather than default lavender`() {
        assertEquals(FinancePalette.surface, FinanceColorScheme.surfaceContainerLow)
        assertEquals(FinancePalette.surface, FinanceColorScheme.surfaceContainer)
        assertEquals(FinancePalette.surface, FinanceColorScheme.surfaceContainerHigh)
        assertEquals(FinancePalette.background, FinanceColorScheme.surfaceContainerHighest)
        assertEquals(FinancePalette.accentContainer, FinanceColorScheme.secondaryContainer)
        assertAccessibleContrast(FinanceColorScheme.onSecondaryContainer, FinanceColorScheme.secondaryContainer)
        assertAccessibleContrast(FinanceColorScheme.onSurface, FinanceColorScheme.surfaceContainer)
    }

    @Test
    fun `palette uses approved finance colors`() {
        assertEquals(Color(0xFF52697F), FinancePalette.primary)
        assertEquals(Color(0xFF247A75), FinancePalette.accent)
        assertEquals(Color(0xFFF3F6F8), FinancePalette.background)
        assertEquals(Color(0xFFFFFFFF), FinancePalette.surface)
        assertEquals(Color(0xFF1E2933), FinancePalette.mainText)
        assertEquals(Color(0xFF626F7D), FinancePalette.supportingText)
        assertEquals(Color(0xFF2E7D32), FinancePalette.income)
        assertEquals(Color(0xFFC62828), FinancePalette.expense)
    }

    @Test
    fun `main text meets normal text contrast on app background`() {
        assertAccessibleContrast(FinancePalette.mainText, FinancePalette.background)
    }

    @Test
    fun `supporting text meets normal text contrast on app background`() {
        assertAccessibleContrast(FinancePalette.supportingText, FinancePalette.background)
    }

    @Test
    fun `white text meets normal text contrast on primary buttons`() {
        assertAccessibleContrast(Color.White, FinancePalette.primary)
    }

    @Test
    fun `white text meets normal text contrast on accent buttons`() {
        assertAccessibleContrast(Color.White, FinancePalette.accent)
    }

    private fun assertAccessibleContrast(foreground: Color, background: Color) {
        val first = foreground.luminance().toDouble()
        val second = background.luminance().toDouble()
        val contrast = (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
        assertTrue("Expected contrast >= 4.5:1, got $contrast:1", contrast >= 4.5)
    }
}
