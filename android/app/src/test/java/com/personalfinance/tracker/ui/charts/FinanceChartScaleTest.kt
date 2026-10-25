package com.personalfinance.tracker.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceChartScaleTest {
    @Test
    fun `empty scale is finite and zero`() {
        val fraction = normalizedBarFraction(0L, 0L)
        assertTrue(fraction.isFinite())
        assertEquals(0f, fraction, 0f)
    }

    @Test
    fun `maximum fills chart`() {
        assertEquals(1f, normalizedBarFraction(100L, 100L), 0f)
    }

    @Test
    fun `overflowing scale is clamped`() {
        assertEquals(1f, normalizedBarFraction(200L, 100L), 0f)
    }

    @Test
    fun `values scale proportionally`() {
        assertEquals(0.25f, normalizedBarFraction(25L, 100L), 0f)
    }

    @Test
    fun `negative values and nonpositive maxima produce zero`() {
        assertEquals(0f, normalizedBarFraction(-1L, 100L), 0f)
        assertEquals(0f, normalizedBarFraction(10L, 0L), 0f)
        assertEquals(0f, normalizedBarFraction(10L, -1L), 0f)
    }

    @Test
    fun `large values stay finite without integer overflow`() {
        assertEquals(0.5f, normalizedBarFraction(Long.MAX_VALUE / 2, Long.MAX_VALUE), 0.000001f)
    }
}
