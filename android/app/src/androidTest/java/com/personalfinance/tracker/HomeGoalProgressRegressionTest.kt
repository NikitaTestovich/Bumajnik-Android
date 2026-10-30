package com.personalfinance.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.preferences.HomeLayoutStore
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.ui.home.*
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.runBlocking
import org.junit.*

class HomeGoalProgressRegressionTest {
    @get:Rule val ui = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
    private var prepared = false
    private var originalLayout: List<HomeBlockSetting> = emptyList()

    @Before fun prepare() {
        Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        prepared = true
        app.container.database.clearAllTables()
        val store = HomeLayoutStore(app)
        originalLayout = store.layout.value
        store.save(HomeBlock.entries.map { HomeBlockSetting(it, it == HomeBlock.GOAL) })
        runBlocking { app.container.financeRepository.saveProfile(UserProfile("Goal fixture", "en", "USD", true)) }
    }

    @After fun cleanup() {
        if (!prepared) return
        HomeLayoutStore(app).save(originalLayout)
        app.container.database.clearAllTables()
    }

    @Test fun homeTextAndAccessibilityNeverRoundIncompleteSavingsToOneHundred() {
        ui.setContent { FinanceTheme { HomeScreen({}, {}, {}, {}) } }
        listOf(Triple(1000L, 995L, "99.5"), Triple(Long.MAX_VALUE, Long.MAX_VALUE - 1, "99.9"), Triple(1000L, 1000L, "100")).forEach { (target, saved, percent) ->
            runBlocking { app.container.financeRepository.saveGoal(SavingsGoal(Money(target, "USD"), Money(saved, "USD"))) }
            val label = app.getString(R.string.home_goal_progress, percent)
            ui.waitUntil(10_000) { ui.onAllNodesWithText(label).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            ui.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            ui.onNodeWithContentDescription(label).assertExists()
        }
    }
}
