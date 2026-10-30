package com.personalfinance.tracker

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.ui.goals.GoalScreen
import com.personalfinance.tracker.ui.goals.GoalProgressCard
import com.personalfinance.tracker.ui.theme.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*

class GoalProgressScreenTest {
    @get:Rule val ui = createComposeRule()
    @Before fun emulatorOnly() { Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) }
    @Test fun actualRussianGoalScreenFitsNarrowLargeFontInEveryTheme() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
        runBlocking {
            app.container.financeRepository.saveProfile(UserProfile("Demo Goal", "ru", "USD", true))
            app.container.financeRepository.saveGoal(SavingsGoal(Money(10000, "USD"), Money(3800, "USD")))
        }
        val theme = mutableStateOf(AppThemeRegistry.default)
        val config = android.content.res.Configuration(app.resources.configuration).apply { setLocale(java.util.Locale("ru")) }
        val context = app.createConfigurationContext(config)
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides context,
                androidx.compose.ui.platform.LocalConfiguration provides config,
                LocalDensity provides Density(density.density, 1.6f)
            ) {
                FinanceTheme(theme.value) { Box(Modifier.requiredWidth(280.dp)) { GoalScreen({}) } }
            }
        }
        AppThemeRegistry.available.forEach { selected ->
            ui.runOnIdle { theme.value = selected }
            ui.waitUntil(10000) { ui.onAllNodesWithTag("goal_segments").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithTag("goal_segments").performScrollTo().assertIsDisplayed()
            val bar = ui.onNodeWithTag("goal_segments").fetchSemanticsNode().boundsInRoot
            val root = ui.onRoot().fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("Bar fits root in theme ${selected.id}", bar.left >= root.left && bar.right <= root.right)
            listOf("0%", "25%", "50%", "75%", "100%").forEach { label ->
                val node = ui.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                org.junit.Assert.assertTrue("Milestone $label fits bar", node.left >= bar.left && node.right <= bar.right)
            }
            ui.onNodeWithTag("goal_segments").assertContentDescriptionContains("38%", substring = true)
            ui.onNodeWithText(context.getString(R.string.goal_title)).performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun factualProgressAndMilestonesRemainVisibleWithoutForecastRates() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
        runBlocking {
            app.container.financeRepository.saveProfile(UserProfile("Goal fixture", "en", "USD", true))
            app.container.financeRepository.saveGoal(SavingsGoal(Money(101, "USD"), Money(25, "USD")))
            app.container.financeRepository.saveTransaction(FinanceTransaction(
                "c731352b-9f42-40b4-b3b5-749036a11703", "Goal fixture", TransactionType.INCOME,
                java.time.LocalDate.now().minusMonths(1), Money(100, "EUR"), null,
            ))
        }
        runBlocking {
            val firstDate = app.container.financeRepository.observeTransactions(null).first().minOf { it.date }
            val range = com.personalfinance.tracker.ui.goals.GoalViewModel.forecastRange(firstDate, java.time.LocalDate.now())!!
            app.container.rateRepository.saveRates(range, "USD", emptyMap())
        }
        ui.setContent { FinanceTheme { GoalScreen({}) } }
        ui.waitUntil(10000) { ui.onAllNodesWithTag("goal_segments").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("goal_segments").performScrollTo().assertContentDescriptionContains("24.7%", substring = true)
        ui.onNodeWithText("25%").assertExists()
        ui.onNodeWithText("50%").assertExists()
        ui.onNodeWithText("75%").assertExists()
        ui.onNodeWithText("100%").assertExists()
        ui.onNodeWithText(app.getString(R.string.exchange_rate_open)).performScrollTo().assertIsDisplayed()
        runBlocking { app.container.financeRepository.deleteTransaction("c731352b-9f42-40b4-b3b5-749036a11703") }
    }

    @Test fun allThemesSupportNarrowLargeTextAndActualOverTargetAmounts() {
        val theme = mutableStateOf(AppThemeRegistry.default)
        val saved = mutableLongStateOf(38)
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                FinanceTheme(theme.value) {
                    Column(Modifier.width(280.dp).verticalScroll(rememberScrollState())) {
                        GoalProgressCard(SavingsGoal(Money(100, "USD"), Money(saved.longValue, "USD")))
                    }
                }
            }
        }
        AppThemeRegistry.available.forEach { selected ->
            ui.runOnIdle { theme.value = selected; saved.longValue = 38 }
            ui.onNodeWithTag("goal_segments").performScrollTo().assertContentDescriptionContains("38%", substring = true)
            ui.onNodeWithTag("goal_segments").assertContentDescriptionContains("0.12 USD", substring = true)
            ui.runOnIdle { saved.longValue = 150 }
            ui.onNodeWithTag("goal_segments").assertContentDescriptionContains("1.50 USD", substring = true)
            ui.onNodeWithTag("goal_segments").assertContentDescriptionContains("100%", substring = true)
            ui.onNodeWithTag("goal_segments").assertContentDescriptionContains("0.00 USD", substring = true)
        }
    }
}
