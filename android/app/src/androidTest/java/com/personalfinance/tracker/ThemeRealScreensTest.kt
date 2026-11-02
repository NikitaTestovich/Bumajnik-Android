package com.personalfinance.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.ui.theme.*
import com.personalfinance.tracker.ui.charts.*
import com.personalfinance.tracker.ui.home.*
import com.personalfinance.tracker.ui.profile.ProfileScreen
import com.personalfinance.tracker.ui.operations.TransactionFormScreen
import com.personalfinance.tracker.ui.reports.ReportsScreen
import com.personalfinance.tracker.ui.family.FamilyScreen
import com.personalfinance.tracker.ui.security.AppLockScreen
import com.personalfinance.tracker.security.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class ThemeRealScreensTest {
 @get:Rule val ui=createComposeRule()
 private val app get()=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
 @Before fun emulatorOnly() { Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu","goldfish")) }
 @Test fun allDesignsKeepRealFormsReportsFamilyAndLockUsableAtLargeText() {
  val theme=mutableStateOf(AppThemeRegistry.default)
  val screen=mutableIntStateOf(0)
  val scale=mutableFloatStateOf(1f)
  val profile=UserProfile("Theme fixture","en","USD",true)
  val repo=app.container.financeRepository
  val oldProfile=runBlocking { repo.observeProfile().first() }
  val originalSecurity=app.getSharedPreferences("app_lock",0).all.toMap()
  val lock=visualLock()
  runBlocking { lock.enable("Ab12",false,true) }
  lock.lockNow(); lock.biometricEntry.beginSession(lock.generation,false,true)
  val homeStore=com.personalfinance.tracker.data.preferences.HomeLayoutStore(app)
  val oldLayout=homeStore.layout.value
  homeStore.reset()
  try {
   ui.setContent {
    val d=LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density,scale.floatValue)) {
     FinanceTheme(theme.value) {
      key(screen.intValue) {
       when(screen.intValue) {
        0 -> HomeScreen({},{},{},{})
        1 -> ProfileScreen(profile,emptyList(),{_,_,_->},{_,_,_->true},{_,_,_->true},{true},{},{},{},{},theme.value,{})
        2 -> TransactionFormScreen(null,TransactionType.EXPENSE,{},{})
        3 -> ReportsScreen()
        4 -> FamilyScreen({})
        else -> AppLockScreen(lock,{},{},{})
       }
      }
     }
    }
   }
   AppThemeRegistry.available.forEach { selected ->
    listOf(1f,1.6f).forEach { fontScale ->
     ui.runOnIdle { theme.value=selected; scale.floatValue=fontScale; screen.intValue=0 }
     ui.onNodeWithTag("home_customize").performScrollTo()
     if(fontScale==1f) {
      val image=ui.onRoot().captureToImage()
      assertEquals(themeColorScheme(selected).background,image.toPixelMap()[0,0])
      java.io.File(app.filesDir,"theme-${selected.id}-home.png").outputStream().use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
     }
     ui.onNodeWithTag("home_block_goal").performScrollTo().assertIsDisplayed()
     val goal=ui.onNodeWithTag("home_block_goal").fetchSemanticsNode().boundsInRoot
     val latest=ui.onNodeWithTag("home_block_latest").fetchSemanticsNode().boundsInRoot
     if(app.resources.configuration.screenWidthDp >= 360) {
      if(selected.id in listOf(6,8) && fontScale==1f) assertEquals(goal.top,latest.top,2f)
      else assertTrue(latest.top>=goal.bottom)
     } else ui.onNodeWithTag("home_block_latest").performScrollTo().assertIsDisplayed()
     ui.onNodeWithText(app.getString(R.string.goal_open)).performScrollTo().performClick()
     ui.runOnIdle { screen.intValue=1 }
     ui.onNodeWithText(app.getString(R.string.profile_save)).performScrollTo().assertIsEnabled()
     assertSurface(selected)
     ui.runOnIdle { screen.intValue=2 }
     ui.onNodeWithTag("expense_category").performScrollTo().assertIsDisplayed().performClick()
     androidx.test.espresso.Espresso.pressBack()
     assertSurface(selected)
     ui.runOnIdle { screen.intValue=3 }
     ui.onNodeWithText(app.getString(R.string.reports_apply)).performScrollTo().assertIsDisplayed().performClick()
     assertSurface(selected)
     ui.runOnIdle { screen.intValue=4 }
     ui.onNodeWithText(app.getString(R.string.family_import)).assertIsDisplayed()
     assertSurface(selected)
     ui.runOnIdle { screen.intValue=5 }
     ui.onNodeWithTag("unlock_code").assertIsDisplayed().performTextInput("Ab12")
     ui.onNodeWithTag("unlock_button").performScrollTo().assertIsEnabled()
     assertSurface(selected)
    }
   }
  } finally {
   homeStore.save(oldLayout)
   assertEquals(originalSecurity,app.getSharedPreferences("app_lock",0).all.toMap())
   assertEquals(oldProfile,runBlocking { repo.observeProfile().first() })
  }
 }
 @Test fun isolatedHomeModulesStayFullWidthAndOnlyAdjacentVisibleModulesPair() {
  val store=com.personalfinance.tracker.data.preferences.HomeLayoutStore(app)
  val original=store.layout.value
  val theme=mutableStateOf(AppThemeRegistry.resolve(6))
  val layoutVersion=mutableIntStateOf(0)
  try {
   store.save(listOf(HomeBlockSetting(HomeBlock.GOAL,true),HomeBlockSetting(HomeBlock.LATEST,false),HomeBlockSetting(HomeBlock.SUMMARY,false),HomeBlockSetting(HomeBlock.QUICK_ACTIONS,false),HomeBlockSetting(HomeBlock.EXPENSES,false)))
   ui.setContent { FinanceTheme(theme.value) { key(layoutVersion.intValue) { HomeScreen({},{},{},{}) } } }
   listOf(6,8).forEach { id ->
    ui.runOnIdle { theme.value=AppThemeRegistry.resolve(id) }
    val layouts=listOf(
     listOf(HomeBlock.GOAL),
     listOf(HomeBlock.LATEST),
     listOf(HomeBlock.GOAL,HomeBlock.QUICK_ACTIONS,HomeBlock.LATEST),
     listOf(HomeBlock.LATEST,HomeBlock.GOAL),
    )
    layouts.forEach { visible ->
     ui.runOnIdle { store.save(visible.map { HomeBlockSetting(it,true) }+HomeBlock.entries.filter { it !in visible }.map { HomeBlockSetting(it,false) }); layoutVersion.intValue++ }
     val full=ui.onNodeWithTag("home_customize").fetchSemanticsNode().boundsInRoot.width
     visible.filter { it in listOf(HomeBlock.GOAL,HomeBlock.LATEST) }.forEach { block ->
      val width=ui.onNodeWithTag("home_block_${block.key}").fetchSemanticsNode().boundsInRoot.width
      if(visible==listOf(HomeBlock.LATEST,HomeBlock.GOAL) && app.resources.configuration.screenWidthDp >= 360) assertEquals(full*.48f,width,2f)
      else assertEquals("Unpaired $block in $visible, theme $id",full,width,2f)
     }
     if(visible==listOf(HomeBlock.LATEST,HomeBlock.GOAL) && app.resources.configuration.screenWidthDp >= 360) {
      val latest=ui.onNodeWithTag("home_block_latest").fetchSemanticsNode().boundsInRoot
      val goal=ui.onNodeWithTag("home_block_goal").fetchSemanticsNode().boundsInRoot
      assertEquals(latest.top,goal.top,2f); assertTrue(latest.left<goal.left)
     }
    }
   }
  } finally { store.save(original) }
 }
 private class VisualLockStorage : LockStorage {
  var record: StoredLock? = null
  override val enabled get() = record != null
  override fun read() = record
  override fun write(record: StoredLock) { this.record=record }
  override fun clear() { record=null }
 }
 private fun visualLock() = AppLockController(VisualLockStorage())
 @Test fun visualLockHasOwnStorageAndDoesNotTouchPopulatedBiometricFixture() {
  val retainedStorage=VisualLockStorage()
  val retained=AppLockController(retainedStorage)
  assertEquals(LockResult.SUCCESS,runBlocking { retained.enable("Keep123",true,true) })
  val originalRecord=retainedStorage.record
  val fixture=visualLock()
  assertEquals(LockResult.SUCCESS,runBlocking { fixture.enable("Ab12",false,true) })
  fixture.lockNow(); fixture.biometricEntry.beginSession(fixture.generation,false,true)
  ui.setContent { FinanceTheme(AppThemeRegistry.resolve(9)) { AppLockScreen(fixture,{},{},{}) } }
  ui.onNodeWithTag("unlock_code").performTextInput("Ab12")
  ui.onNodeWithTag("unlock_button").performScrollTo().performClick()
  ui.waitUntil(10_000) { !fixture.state.value.locked }
  runBlocking { fixture.recoverAfterDeviceCredential(fixture.generation) }
  assertSame(originalRecord,retainedStorage.record)
  assertTrue(retained.state.value.enabled)
  assertTrue(retained.state.value.biometrics)
  assertEquals(LockResult.SUCCESS,runBlocking { retained.unlock("Keep123") })
 }
 private fun assertSurface(theme: AppTheme) { assertEquals(themeColorScheme(theme).background,ui.onRoot().captureToImage().toPixelMap()[0,0]) }
 @Test fun nightAnalyticsComparesPeriodsWithLabelledHorizontalBars() {
  ui.setContent { FinanceTheme(AppThemeRegistry.resolve(9)) { FinanceCharts.GroupedColumnChart(listOf(FinanceBarGroup("Income",listOf(FinanceBar("Current",12345,financeColors.income),FinanceBar("Previous",6789,financeColors.income))))) } }
  ui.onNodeWithTag("comparison_horizontal").assertExists()
  ui.onNodeWithContentDescription("Income, Current: 123.45").assertExists()
 }
}
