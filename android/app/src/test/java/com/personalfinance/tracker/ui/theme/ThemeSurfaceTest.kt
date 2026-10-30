package com.personalfinance.tracker.ui.theme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test
class ThemeSurfaceTest {
 @Test fun `preview radio colors have contrast on their own card`() {
   AppThemeRegistry.available.forEach { preview ->
    val c = themeColorScheme(preview)
    // Selected/disabled-selected and unchecked/disabled-unchecked share these opaque colors.
    contrast(c.primary, c.surface)
    contrast(c.onSurfaceVariant, c.surface)
   }
 }
 @Test fun `selected designs provide their own surfaces instead of material defaults`() {
  assertEquals(Color(0xFFFFFEFA), themeColorScheme(AppThemeRegistry.resolve(2)).surface)
  assertEquals(Color(0xFF272727), themeColorScheme(AppThemeRegistry.resolve(6)).surface)
  assertEquals(Color(0xFFE5DCFF), themeColorScheme(AppThemeRegistry.resolve(8)).surfaceContainerLow)
  assertEquals(Color(0xFF313131), themeColorScheme(AppThemeRegistry.resolve(9)).surfaceContainerHigh)
 }
 @Test fun `every theme has readable normal text and semantic errors on every used surface`() {
  AppThemeRegistry.available.forEach { theme ->
   val c = themeColorScheme(theme)
   listOf(c.background,c.surface,c.surfaceContainerLow,c.surfaceContainerHigh).forEach { bg -> contrast(c.onSurface,bg); contrast(c.onSurfaceVariant,bg); contrast(c.error,bg) }
   contrast(c.onPrimary,c.primary); contrast(c.onSecondary,c.secondary); contrast(c.onSecondaryContainer,c.secondaryContainer)
  }
 }
 private fun contrast(fg: Color,bg: Color) { val a=fg.luminance(); val b=bg.luminance(); assertTrue("Unreadable $fg on $bg",(maxOf(a,b)+.05)/(minOf(a,b)+.05)>=4.5) }
}
