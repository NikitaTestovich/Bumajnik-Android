package com.personalfinance.tracker

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.csv.PersonalBackupManager
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.ui.operations.TransactionFormScreen
import com.personalfinance.tracker.ui.operations.TransactionFormViewModel
import com.personalfinance.tracker.ui.operations.TransactionSaveResult
import com.personalfinance.tracker.ui.operations.OperationsScreen
import com.personalfinance.tracker.ui.reports.ReportsScreen
import com.personalfinance.tracker.ui.profile.ProfileScreen
import com.personalfinance.tracker.ui.profile.ProfileViewModel
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CategoryAcceptanceTest {
    @get:Rule val ui = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication
    private val repository get() = app.container.financeRepository
    private var prepared = false
    private val expenseId = UUID.randomUUID().toString()
    @Before fun prepare() = runBlocking {
        val emulated = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("getprop ro.kernel.qemu")).use { it.readBytes().toString(Charsets.UTF_8).trim() }
        check(emulated == "1") { "Synthetic category tests require disposable emulator" }
        prepared = true
        app.container.database.clearAllTables()
        repository.saveProfile(UserProfile("Category Demo", "en", "USD", true))
        repository.saveCategory(Category("groceries", "Продукты", "Groceries", false, iconKey = "groceries"))
        repository.saveTransaction(FinanceTransaction(expenseId, "Category Demo", TransactionType.EXPENSE, LocalDate.now(), Money(1500, "USD"), "groceries"))
    }
    @After fun cleanup() { if (prepared) app.container.database.clearAllTables() }

    @Composable private fun Profile() {
        val categories by repository.observeCategories().collectAsState(initial = emptyList())
        val model = remember { ProfileViewModel(repository) }
        ProfileScreen(UserProfile("Category Demo", "en", "USD", true), categories, { _, _, _ -> },
            { name, lang, icon -> model.addCustomCategory(name, lang, icon) }, model::renameCategory, model::archiveCategory,
            {}, {}, {}, {})
    }
    private fun awaitTag(tag: String) = ui.waitUntil(10000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }

    @Test fun renameIconArchiveAndEditHistoricalExpense() {
        var screen by mutableStateOf(0)
        var saved = false
        ui.setContent { FinanceTheme { key(screen) {
            val categories by repository.observeCategories().collectAsState(initial = emptyList())
            when (screen) {
                1 -> OperationsScreen(categories, "en", {}, {})
                2 -> ReportsScreen()
                4 -> TransactionFormScreen(expenseId, TransactionType.EXPENSE, { saved = true }, {})
                else -> Profile()
            }
        } } }
        awaitTag("category_edit_groceries")
        ui.onNodeWithTag("category_edit_groceries").performScrollTo().performClick()
        ui.onNodeWithTag("category_editor_name").performTextReplacement("Food renamed")
        ui.onNodeWithTag("category_icon_dining").performScrollTo().performClick()
        ui.onNodeWithTag("category_editor_save").performClick()
        ui.waitUntil(10000) { runBlocking { repository.observeCategories().first().single().nameEn == "Food renamed" } }
        val category = runBlocking { repository.observeCategories().first().single() }
        assertEquals("dining", category.iconKey)
        assertEquals("Food renamed", category.nameRu)
        val bytes = runBlocking { PersonalBackupManager(repository, { byteArrayOf() }, { _, _ -> }).backupBytes() }
        assertTrue(bytes.toString(Charsets.UTF_8).contains("Food renamed"))
        assertEquals("groceries", runBlocking { repository.observeTransactions(null).first().single().categoryId })
        ui.runOnIdle { screen = 1 }
        ui.waitUntil(10000) { ui.onAllNodesWithText("Food renamed", substring = true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        ui.runOnIdle { screen = 2 }
        ui.waitUntil(10000) { ui.onAllNodes(hasContentDescription("Food renamed", substring = true)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        ui.runOnIdle { screen = 0 }
        awaitTag("category_delete_groceries")
        ui.onNodeWithTag("category_delete_groceries").performScrollTo().performClick()
        ui.onNodeWithTag("category_delete_confirm").performClick()
        ui.waitUntil(10000) { runBlocking { repository.observeCategories().first().single().isArchived } }
        ui.runOnIdle { screen = 3 }
        ui.onNodeWithTag("category_row_groceries").assertDoesNotExist()
        val result = runBlocking { TransactionFormViewModel(repository).save(null, TransactionType.EXPENSE, "Demo", "20-10-2026", "1", "USD", "groceries") }
        assertEquals(TransactionSaveResult.CategoryRequired, result)
        ui.runOnIdle { screen = 4 }
        awaitTag("expense_category")
        ui.onNodeWithTag("expense_category").performScrollTo().assertTextContains("Food renamed")
        ui.onNodeWithText(app.getString(R.string.operation_save)).performScrollTo().performClick()
        ui.waitUntil(10000) { saved }
        assertTrue(runBlocking { repository.observeCategories().first().single().isArchived })
    }

    @Test fun archiveAllCreateCategoryAndSaveNewExpense() {
        runBlocking { ProfileViewModel(repository).archiveCategory("groceries") }
        var form by mutableStateOf(false)
        var saved = false
        ui.setContent { FinanceTheme { if (!form) Profile() else TransactionFormScreen(null, TransactionType.EXPENSE, { saved = true }, {}) } }
        awaitTag("category_add")
        ui.onNodeWithTag("category_add").performScrollTo().performClick()
        ui.onNodeWithTag("category_editor_name").performTextInput("New category")
        ui.onNodeWithTag("category_editor_save").performClick()
        ui.waitUntil(10000) { runBlocking { repository.observeCategories().first().any { !it.isArchived } } }
        ui.runOnIdle { form = true }
        awaitTag("expense_category")
        ui.onNodeWithText(app.getString(R.string.operation_amount)).performTextInput("2")
        ui.onNodeWithTag("expense_category").performScrollTo().performClick()
        ui.onNodeWithText("Groceries").assertDoesNotExist()
        ui.onNodeWithText("New category").performClick()
        ui.onNodeWithText(app.getString(R.string.operation_save)).performScrollTo().performClick()
        ui.waitUntil(10000) { saved }
        assertEquals(2, runBlocking { repository.observeTransactions(null).first().size })
    }
}
