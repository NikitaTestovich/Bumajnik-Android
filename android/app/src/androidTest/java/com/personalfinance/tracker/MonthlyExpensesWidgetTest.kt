package com.personalfinance.tracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import android.content.res.Configuration
import java.util.Locale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.ui.charts.ExpenseSlice
import com.personalfinance.tracker.ui.home.*
import com.personalfinance.tracker.ui.theme.FinanceTheme
import com.personalfinance.tracker.data.preferences.HomeLayoutStore
import com.personalfinance.tracker.domain.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonthlyExpensesWidgetTest {
    @get:Rule val ui = createComposeRule()
    @Before fun emulatorOnly() { Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) }
    private fun awaitWidget() = ui.waitUntil(10_000) { ui.onAllNodesWithTag("monthly_expenses").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    @Test fun selectionByCardAndRingResetAndRemovedSelection() {
        var slices by mutableStateOf(listOf(ExpenseSlice("a", "Groceries", "groceries", 600, .6), ExpenseSlice("b", "Transport", "transport", 400, .4)))
        ui.setContent { FinanceTheme { Column(Modifier.verticalScroll(rememberScrollState())) { MonthlyExpensesWidget(slices, "USD", "en") } } }
        awaitWidget()
        ui.onNodeWithTag("expense_center").assertTextEquals("10.00 USD")
        ui.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "expenses-en.png").outputStream())
        ui.onNodeWithTag("expense_slice_a").performClick()
        ui.onNodeWithTag("expense_center").assertTextEquals("6.00 USD")
        ui.onNodeWithTag("expense_slice_a").performClick()
        ui.onNodeWithTag("expense_center").assertTextEquals("10.00 USD")
        ui.onNodeWithTag("expense_ring").performTouchInput { click(Offset(width * .88f, height / 2f)) }
        ui.onNodeWithTag("expense_center").assertTextEquals("6.00 USD")
        ui.runOnIdle { slices = listOf(ExpenseSlice("b", "Transport", "transport", 400, 1.0)) }
        ui.onNodeWithTag("expense_center").assertTextEquals("4.00 USD")
    }
    @Test fun russianLargeTextAndLongCategory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) })
        ui.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                FinanceTheme { Column(Modifier.verticalScroll(rememberScrollState())) { MonthlyExpensesWidget(listOf(ExpenseSlice("long", "Очень длинное название категории расходов", "home", 12345, 1.0)), "BYN", "ru") } }
            }
        }
        awaitWidget()
        ui.onNodeWithText("Расходы по категориям").assertIsDisplayed()
        ui.onNodeWithTag("expense_slice_long").performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("expense_slice_long").performClick()
        ui.onNodeWithTag("expense_center").performScrollTo().assertIsDisplayed().assertTextEquals("123.45 BYN")
        val ring = ui.onNodeWithTag("expense_ring").fetchSemanticsNode().boundsInRoot
        val name = ui.onNodeWithTag("expense_center_name").fetchSemanticsNode().boundsInRoot
        val amount = ui.onNodeWithTag("expense_center").fetchSemanticsNode().boundsInRoot
        assertTrue("Selected name remains inside ring", name.top >= ring.top && name.bottom <= ring.bottom)
        assertTrue("Amount remains inside ring", amount.top >= ring.top && amount.bottom <= ring.bottom)
        assertTrue("Name does not displace/overlap amount", name.bottom <= amount.top)
        assertTrue("Centre name stays within 125dp", name.width <= 125 * context.resources.displayMetrics.density + 1)
        ui.onNodeWithTag("expense_center_name").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val layouts = mutableListOf<TextLayoutResult>()
            assertTrue(action(layouts))
            assertTrue("Centre name is bounded to two lines", layouts.single().lineCount <= 2)
        }
        ui.onNodeWithTag("expense_center").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val layouts = mutableListOf<TextLayoutResult>()
            assertTrue(action(layouts))
            assertFalse("Full amount fits without clipping", layouts.single().hasVisualOverflow)
        }
        ui.onNodeWithTag("expense_slice_long").assertContentDescriptionContains("Очень длинное название категории расходов", substring = true)
        ui.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.File(context.filesDir, "expenses-ru.png").outputStream())
    }
    @Test fun emptyMissingRatesAndManyLongNamesRemainReachable() {
        var missing by mutableStateOf(false)
        var slices by mutableStateOf(emptyList<ExpenseSlice>())
        var rateRequests = 0
        ui.setContent { FinanceTheme { Column(Modifier.verticalScroll(rememberScrollState())) { MonthlyExpensesWidget(slices, "USD", "en", missing) { rateRequests++ } } } }
        awaitWidget()
        ui.onNodeWithTag("expenses_empty").assertIsDisplayed()
        ui.runOnIdle { missing = true }
        ui.onNodeWithTag("expense_ring").assertDoesNotExist()
        ui.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.exchange_rate_open)).performClick()
        ui.runOnIdle { assertEquals(1, rateRequests); missing = false; slices = (1..20).map { ExpenseSlice("$it", "A very long category name number $it", "other", 1, .05) } }
        ui.onNodeWithTag("expense_slice_20").performScrollTo().assertIsDisplayed()
    }
    @Test fun zeroConvertedCardsRemainVisibleWithoutNoExpensesMessageOrSegments() {
        ui.setContent { FinanceTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            MonthlyExpensesWidget(listOf(ExpenseSlice("tiny", "Tiny expense", "other", 0, 0.0)), "USD", "en")
        } } }
        awaitWidget()
        ui.onNodeWithTag("expenses_empty").assertDoesNotExist()
        ui.onNodeWithTag("expense_center").assertTextEquals("0.00 USD")
        ui.onNodeWithTag("expense_slice_tiny").assertIsDisplayed().assertIsNotSelected()
        ui.onNodeWithTag("expense_ring").performTouchInput { click(Offset(width * .88f, height / 2f)) }
        ui.onNodeWithTag("expense_slice_tiny").assertIsNotSelected()
        ui.onNodeWithTag("expense_slice_tiny").performClick().assertIsSelected()
        ui.onNodeWithTag("expense_center").assertTextEquals("0.00 USD")
    }
    @Test fun defaultWidgetCanBeHiddenAndResetRestoresIt() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
        val store = HomeLayoutStore(app)
        runBlocking { app.container.financeRepository.saveProfile(UserProfile("Widget Demo", "en", "USD", true)) }
        store.reset()
        try {
            ui.setContent { FinanceTheme { HomeScreen({}, {}, {}, {}) } }
            ui.waitUntil(10_000) { ui.onAllNodesWithTag("home_customize").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            ui.onNodeWithTag("monthly_expenses").performScrollTo().assertIsDisplayed()
            ui.onNodeWithTag("home_customize").performClick()
            ui.onNodeWithTag("home_settings_list").performScrollToNode(hasTestTag("home_visible_expenses"))
            ui.onNodeWithTag("home_visible_expenses").performClick()
            ui.onNodeWithText(app.getString(R.string.home_done)).performClick()
            ui.onNodeWithTag("monthly_expenses").assertDoesNotExist()
            assertFalse(HomeLayoutStore(app).layout.value.single { it.id == HomeBlock.EXPENSES }.visible)
            ui.onNodeWithTag("home_customize").performClick()
            ui.onNodeWithText(app.getString(R.string.home_reset)).performClick()
            ui.onAllNodesWithText(app.getString(R.string.home_reset)).onLast().performClick()
            ui.onNodeWithText(app.getString(R.string.home_done)).performClick()
            ui.onNodeWithTag("monthly_expenses").performScrollTo().assertIsDisplayed()
        } finally { store.reset() }
    }
    @Test fun savedHiddenWidgetAndOrderSurviveOpeningHome() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
        val store = HomeLayoutStore(app)
        val saved = normalizeHomeLayout(listOf(HomeBlockSetting(HomeBlock.EXPENSES, false), HomeBlockSetting(HomeBlock.LATEST, true)))
        store.save(saved)
        runBlocking { app.container.financeRepository.saveProfile(UserProfile("Widget Demo", "en", "USD", true)) }
        try {
            ui.setContent { FinanceTheme { HomeScreen({}, {}, {}, {}) } }
            ui.waitUntil(10_000) { ui.onAllNodesWithTag("home_customize").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
            ui.onNodeWithTag("monthly_expenses").assertDoesNotExist()
            assertEquals(saved, HomeLayoutStore(app).layout.value)
        } finally { store.reset() }
    }
}
