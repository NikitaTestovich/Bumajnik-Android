package com.personalfinance.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.toPixelMap
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class ThemeRecreationTest {
 @get:Rule val ui = createEmptyComposeRule()
 private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
 private var scenario: ActivityScenario<MainActivity>? = null
 private var prepared = false
 @Before fun prepare() {
  Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
  prepared = true
  runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
  app.container.database.clearAllTables()
  runBlocking { app.container.financeRepository.saveProfile(UserProfile("Theme Demo", "en", "USD", true)) }
  app.themeStore.select(2)
 }
 @After fun cleanup() {
  if (!prepared) return
  scenario?.close()
  runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
  app.themeStore.select(2)
  app.container.database.clearAllTables()
 }
 @Test fun profileChoiceSurvivesActivityRecreationAndAppliesToLock() {
  scenario = ActivityScenario.launch(MainActivity::class.java)
  fun openProfile() {
   val more = app.getString(R.string.navigation_more)
   ui.waitUntil(10000) { ui.onAllNodesWithText(more).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
   ui.onNodeWithText(more).performClick()
  }
  openProfile()
  ui.onNodeWithTag("open_THEMES").performScrollTo().performClick()
  ui.onNodeWithTag("theme_9").performScrollTo().performClick().assertIsSelected()
  assertEquals(9, app.themeStore.theme.value.id)
  scenario!!.recreate()
  ui.waitForIdle()
  // Navigation state can itself be restored on recreation.
  if (ui.onAllNodesWithTag("theme_9").fetchSemanticsNodes().isEmpty()) {
   openProfile()
   ui.onNodeWithTag("open_THEMES").performScrollTo().performClick()
  }
  ui.onNodeWithTag("theme_9").performScrollTo().assertIsSelected()
  runBlocking { app.appLock.enable("Ab12", false, true) }
  scenario!!.onActivity { app.appLock.lockNow() }
  ui.onNodeWithTag("app_lock_screen").assertIsDisplayed()
  val color = ui.onNodeWithTag("app_lock_screen").captureToImage().toPixelMap()[2,2]
  assertEquals(themeColorScheme(AppThemeRegistry.resolve(9)).background, color)
  assertEquals(9, com.personalfinance.tracker.data.preferences.ThemeStore(app).theme.value.id)
 }
}
