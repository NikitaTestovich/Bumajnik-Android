package com.personalfinance.tracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class FinancePaletteTest {
 @Test fun `positive and negative amounts stay green and red in every design`() {
  AppThemeRegistry.available.forEach { theme ->
   val colors=financeColors(theme)
   assertTrue(colors.income.green>colors.income.red)
   assertTrue(colors.expense.red>colors.expense.green)
   val scheme=themeColorScheme(theme)
   listOf(scheme.background,scheme.surface,scheme.surfaceContainerLow,scheme.surfaceContainerHigh).forEach { background ->
    contrast(colors.income,background); contrast(colors.expense,background)
   }
   contrast(colors.goalInk,colors.goal); contrast(colors.latestInk,colors.latest)
  }
 }
 @Test fun `chart label ink contrasts against every theme slice colour`() {
  AppThemeRegistry.available.forEach { theme ->
   financeColors(theme).chart.forEach { slice -> contrast(if(theme.dark) Color(0xFF181818) else Color.White,slice) }
  }
 }
 private fun contrast(fg: Color,bg: Color) { val a=fg.luminance(); val b=bg.luminance(); assertTrue("Unreadable $fg on $bg",(maxOf(a,b)+.05)/(minOf(a,b)+.05)>=4.5) }
}
