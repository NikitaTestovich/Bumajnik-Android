package com.personalfinance.tracker

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.ui.operations.OperationsScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import com.personalfinance.tracker.ui.theme.AppThemeRegistry
import com.personalfinance.tracker.domain.model.TransactionType
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.*
import org.junit.Assert.*
import java.util.Locale

@RunWith(Parameterized::class)
class OperationsTabAccessibilityTest(private val themeId: Int, private val language: String) {
 companion object {
  @JvmStatic @Parameterized.Parameters(name = "theme={0}, language={1}, font=200%")
  fun cases(): List<Array<Any>> = listOf(2, 6, 8, 9, 10, 11, 12).flatMap { theme -> listOf("en", "ru").map { language -> arrayOf<Any>(theme, language) } }
 }
 @get:Rule val ui = createComposeRule()
 @After fun resetSelection() {
  if (android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) {
   val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
   app.operationsTabStore.select(TransactionType.EXPENSE)
  }
 }
 @Test fun tabsAreAccessibleAtLargeFont() {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
  val localized = context.createConfigurationContext(config)
  val store = (context.applicationContext as FinanceApplication).operationsTabStore
  Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
  store.select(TransactionType.EXPENSE)
  ui.setContent {
   val density = LocalDensity.current.density
   CompositionLocalProvider(LocalContext provides localized, LocalDensity provides Density(density, 2f)) {
    FinanceTheme(AppThemeRegistry.resolve(themeId)) { Box(Modifier.width(320.dp)) { OperationsScreen(emptyList(), language, {}, {}) } }
   }
  }
  val labels = listOf(R.string.operation_expenses, R.string.operation_income).map { localized.getString(it) }
  labels.forEach { label ->
   ui.onNodeWithText(label).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).assertHeightIsAtLeast(48.dp)
  }
  ui.onNodeWithText(labels[0]).assertIsSelected()
  ui.onNodeWithText(labels[1]).assertIsNotSelected().performClick().assertIsSelected()
  ui.onNodeWithText(labels[0]).assertIsNotSelected().performClick().assertIsSelected()
  ui.onNodeWithText(labels[1]).assertIsNotSelected()
  val bounds = labels.map { ui.onNodeWithText(it).assertHasClickAction().assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
  assertEquals(bounds[0].width, bounds[1].width, 1f)
  labels.forEach { label ->
   val layouts = mutableListOf<TextLayoutResult>()
   ui.onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
   assertTrue(layouts.isNotEmpty())
   assertFalse("Readable label $label: size=${layouts.single().size}, paragraph=${layouts.single().multiParagraph.width}x${layouts.single().multiParagraph.height}, lines=${layouts.single().lineCount}, width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}", layouts.single().hasVisualOverflow)
  }
 }
}
