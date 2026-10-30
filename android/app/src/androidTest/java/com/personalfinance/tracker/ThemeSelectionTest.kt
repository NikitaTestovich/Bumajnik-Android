package com.personalfinance.tracker

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.preferences.ThemeStore
import com.personalfinance.tracker.ui.theme.*
import org.junit.*
import org.junit.Assert.*

class ThemeSelectionTest {
 @get:Rule val ui = createComposeRule()
 @Before fun emulatorOnly() { Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) }
 @Test fun previewRadiosUseTheirOwnPaletteAcrossEveryActiveTheme() {
  val active = mutableStateOf(AppThemeRegistry.default)
  ui.setContent { FinanceTheme(active.value) { ThemePicker(active.value, {}) } }
  AppThemeRegistry.available.forEach { selected ->
   ui.runOnIdle { active.value = selected }
   AppThemeRegistry.available.forEach { preview ->
    val c = themeColorScheme(preview)
    val expected = if (selected.id == preview.id) c.primary else c.onSurfaceVariant
    val pixels = ui.onNodeWithTag("theme_radio_${preview.id}", useUnmergedTree = true).captureToImage().toPixelMap()
    var matches = 0
    for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
     val pixel = pixels[x,y]
     if (kotlin.math.abs(pixel.red - expected.red) < .02f && kotlin.math.abs(pixel.green - expected.green) < .02f && kotlin.math.abs(pixel.blue - expected.blue) < .02f) matches++
    }
    assertTrue("Radio ${preview.id} under theme ${selected.id} must use its preview ink", matches > 10)
   }
  }
 }
 @Test fun selectEveryThemeAndReloadStoreWithoutChangingOtherKeys() {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val prefs = context.getSharedPreferences("appearance",0)
  prefs.edit().clear().putString("future", "retained").commit()
  val store = ThemeStore(context)
  assertEquals(2, store.theme.value.id)
  ui.setContent {
   val theme by store.theme.collectAsState()
   FinanceTheme(theme) { ThemePicker(theme, store::select) }
  }
  listOf(6,8,9,2).forEach { id ->
   ui.onNodeWithTag("theme_$id").performClick().assertIsSelected()
   assertEquals(id, ThemeStore(context).theme.value.id)
  }
  assertEquals("retained", prefs.getString("future", null))
  prefs.edit().putInt("theme_id",77).commit()
  assertEquals(2, ThemeStore(context).theme.value.id)
  prefs.edit().clear().commit()
 }
}
