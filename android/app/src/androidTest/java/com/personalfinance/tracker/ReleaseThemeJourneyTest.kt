package com.personalfinance.tracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.ui.theme.*
import com.personalfinance.tracker.ui.operations.TransactionFormScreen
import com.personalfinance.tracker.ui.reports.ReportsScreen
import com.personalfinance.tracker.ui.family.FamilyScreen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

/** Fictional populated release journey at 280dp and 1.6 font scale, Russian resources. */
class ReleaseThemeJourneyTest {
 @get:Rule val ui = createComposeRule()
 @Test fun populatedRussianReportsFamilyAndIncomeSaveAcrossAllThemes() {
  check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
  val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
  val repo = app.container.financeRepository
  val date = java.time.LocalDate.now()
  runBlocking {
   app.appLock.recoverAfterDeviceCredential(app.appLock.generation)
   app.container.database.clearAllTables()
   repo.saveProfile(UserProfile("Demo Theme", "ru", "USD", true))
   repo.saveCategory(Category("release-food", "Продукты", "Food", true))
   repo.saveTransaction(FinanceTransaction("55555555-5555-4555-8555-555555555555", "Demo Theme", TransactionType.EXPENSE, date, Money(1200,"USD"), "release-food"))
   app.container.familyRepository.saveImportBatch(listOf(FamilyTransaction("55555555-5555-4555-8555-555555555556", "Demo Family", Money(3000,"USD"), date, null, TransactionType.INCOME, null)))
  }
  val config = android.content.res.Configuration(app.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag("ru")) }
  val context = app.createConfigurationContext(config)
  val theme = mutableStateOf(AppThemeRegistry.default)
  val screen = mutableIntStateOf(0)
  var saves = 0
  ui.setContent {
   val registryOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current!!
   val d = LocalDensity.current
   CompositionLocalProvider(androidx.activity.compose.LocalActivityResultRegistryOwner provides registryOwner, LocalContext provides context, LocalConfiguration provides config, LocalDensity provides Density(d.density,1.6f)) {
    FinanceTheme(theme.value) { Box(Modifier.requiredWidth(280.dp)) {
     key(screen.intValue, theme.value.id) { when(screen.intValue) {
      0 -> ReportsScreen()
      1 -> FamilyScreen({})
      else -> TransactionFormScreen(null, TransactionType.INCOME, { saves++ }, {})
     } }
    } }
   }
  }
  AppThemeRegistry.available.forEachIndexed { index, selected ->
   ui.runOnIdle { theme.value = selected; screen.intValue = 0 }
   val from = ui.onNodeWithText(context.getString(R.string.reports_from)).fetchSemanticsNode().boundsInRoot
   val to = ui.onNodeWithText(context.getString(R.string.reports_to)).fetchSemanticsNode().boundsInRoot
   assertTrue("Dates must stack at narrow large text instead of clipping", to.top >= from.bottom)
   ui.onNodeWithText(context.getString(R.string.reports_apply)).performScrollTo().assertIsDisplayed().performClick()
   ui.onRoot().captureToImage().asAndroidBitmap().let { bitmap -> java.io.File(app.filesDir,"release-theme-${selected.id}.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
   ui.runOnIdle { screen.intValue = 1 }
   ui.onNodeWithText(context.getString(R.string.family_import)).performScrollTo().assertIsDisplayed()
   ui.runOnIdle { screen.intValue = 2 }
   ui.onNodeWithText(context.getString(R.string.operation_amount)).performScrollTo().performTextInput("12.34")
   ui.onNodeWithTag("income_comment").performScrollTo().performTextInput("Демо ${selected.id}")
   ui.onNodeWithText(context.getString(R.string.operation_save)).performScrollTo().assertIsDisplayed().performClick()
   ui.waitUntil(10000) { saves == index + 1 }
   assertTrue(runBlocking { repo.observeTransactions(null).first().any { it.comment == "Демо ${selected.id}" && it.money.minorUnits == 1234L } })
  }
 }
}
