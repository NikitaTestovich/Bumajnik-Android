package com.personalfinance.tracker

import androidx.compose.ui.test.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.ui.operations.TransactionFormScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomeCommentAcceptanceTest {
    @get:Rule val ui = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication
    private var prepared = false
    @Before fun prepare() = runBlocking {
        val emulated = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("getprop ro.kernel.qemu")).use { it.readBytes().toString(Charsets.UTF_8).trim() }
        check(emulated == "1") { "Synthetic-data UI test requires emulator" }
        prepared = true
        app.container.database.clearAllTables()
        app.container.financeRepository.saveProfile(UserProfile("Comment Demo", "en", "USD", true))
    }
    @After fun cleanup() { if (prepared) app.container.database.clearAllTables() }

    @Test fun enterEditAndReopenIncomeComment() {
        var saved = false
        var id: String? = null
        var screen by androidx.compose.runtime.mutableStateOf(0)
        ui.setContent { FinanceTheme {
            androidx.compose.runtime.key(screen) {
                TransactionFormScreen(id, TransactionType.INCOME, { saved = true }, {})
            }
        } }
        ui.waitUntil(10000) { ui.onAllNodesWithTag("income_comment").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
        ui.onNodeWithText(app.getString(R.string.operation_amount)).performTextInput("12.50")
        ui.onNodeWithTag("income_comment").performTextInput("Salary, \"bonus\"\n😀")
        ui.onNodeWithText(app.getString(R.string.operation_save)).performScrollTo().performClick()
        ui.waitUntil(10000) { saved }
        val original = runBlocking { app.container.financeRepository.observeTransactions(null).first().single() }
        assertEquals("Salary, \"bonus\"\n😀", original.comment)
        ui.runOnIdle { id = original.id; saved = false; screen++ }
        ui.waitUntil(10000) { ui.onAllNodesWithText(original.comment).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
        ui.onNodeWithTag("income_comment").performTextReplacement("Dividends")
        ui.onNodeWithText(app.getString(R.string.operation_save)).performScrollTo().performClick()
        ui.waitUntil(10000) { saved }
        assertEquals("Dividends", runBlocking { app.container.financeRepository.observeTransactions(null).first().single().comment })
        ui.runOnIdle { screen++ }
        ui.waitUntil(10000) { ui.onAllNodesWithText("Dividends").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
    }
}
