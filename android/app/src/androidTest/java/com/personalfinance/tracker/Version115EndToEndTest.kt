package com.personalfinance.tracker

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.csv.FamilyImportManager
import com.personalfinance.tracker.data.csv.FinanceCsvCodec
import com.personalfinance.tracker.data.csv.PersonalBackupManager
import com.personalfinance.tracker.data.preferences.HomeLayoutStore
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.security.LockResult
import com.personalfinance.tracker.ui.charts.expenseBreakdown
import com.personalfinance.tracker.ui.home.*
import com.personalfinance.tracker.ui.profile.ProfileViewModel
import com.personalfinance.tracker.ui.theme.FinanceTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Joined synthetic release path. System document picker and real biometric prompt have separate UI tests. */
@RunWith(AndroidJUnit4::class)
class Version115EndToEndTest {
    @get:Rule val ui = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication
    private val repo get() = app.container.financeRepository
    private val store get() = HomeLayoutStore(app)
    private val date = LocalDate.of(2026, 10, 21)
    private val incomeId = "90000000-0000-4000-8000-000000000001"
    private val expenseId = "90000000-0000-4000-8000-000000000002"
    private var prepared = false

    @Before fun prepare() = runBlocking {
        val emulated = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("getprop ro.kernel.qemu"))
            .use { it.readBytes().toString(Charsets.UTF_8).trim() }
        check(emulated == "1" && Build.HARDWARE in listOf("ranchu", "goldfish")) { "Synthetic release E2E requires a disposable emulator" }
        prepared = true
        app.appLock.recoverAfterDeviceCredential(app.appLock.generation)
        app.container.database.clearAllTables()
        store.reset()
        repo.saveProfile(UserProfile("Demo Alex", "en", "USD", true))
        repo.saveCategory(Category("groceries", "Продукты", "Groceries", false, iconKey = "groceries"))
        repo.saveTransaction(FinanceTransaction(incomeId, "Demo Alex", TransactionType.INCOME, date, Money(30_000, "USD"), null,
            comment = "Salary, \"bonus\"\n😀"))
        repo.saveTransaction(FinanceTransaction(expenseId, "Demo Alex", TransactionType.EXPENSE, date, Money(6_000, "USD"), "groceries"))
    }

    @After fun cleanup() {
        if (prepared) {
            runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
            app.container.database.clearAllTables()
            store.reset()
        }
    }

    @Test fun csvFamilyRestoreCategoryAndCodeFallbackStayConsistent() = runBlocking {
        val backup = PersonalBackupManager(repo, { byteArrayOf() }, { _, _ -> }).backupBytes()
        assertEquals(FinanceCsvCodec.header.joinToString(","), backup.toString(Charsets.UTF_8).lineSequence().first())
        assertEquals("Salary, \"bonus\"\n😀", FinanceCsvCodec.decode(backup).records.first { it.recordId == incomeId }.comment)

        // A second member exports the same current format, including the optional income comment.
        val memberRows = listOf(
            FinanceTransaction("90000000-0000-4000-8000-000000000003", "Demo Sam", TransactionType.INCOME, date, Money(20_000, "EUR"), null, comment = "Gift"),
            FinanceTransaction("90000000-0000-4000-8000-000000000004", "Demo Sam", TransactionType.EXPENSE, date, Money(2_000, "EUR"), "groceries"),
        )
        val memberCsv = FinanceCsvCodec.encode(memberRows, "Demo Sam", mapOf("groceries" to "Groceries"))
        val importer = FamilyImportManager(app.container.familyRepository) { memberCsv }
        val preview = importer.previewBytes(memberCsv)
        val first = importer.import(preview, "USD", mapOf("EUR" to java.math.BigDecimal("1.1")))
        val second = importer.import(preview, "USD", mapOf("EUR" to java.math.BigDecimal("1.1")))
        assertEquals(2, first.addedCount)
        assertEquals(0, second.addedCount)
        assertEquals(2, second.duplicateCount)
        val family = app.container.familyRepository.observeFamilyTransactions(PeriodRange(date, date.plusDays(1))).first()
        assertEquals("Gift", family.first { it.type == TransactionType.INCOME }.comment)

        repo.deleteTransaction(incomeId)
        repo.deleteTransaction(expenseId)
        val restored = PersonalBackupManager(repo, { byteArrayOf() }, { _, _ -> }).restoreBytes(backup)
        assertEquals(2, restored.addedCount)
        assertEquals("Salary, \"bonus\"\n😀", repo.observeTransactions(null).first().first { it.id == incomeId }.comment)

        val editor = ProfileViewModel(repo)
        assertTrue(editor.renameCategory("groceries", "Food renamed", "dining"))
        assertTrue(editor.archiveCategory("groceries"))
        val category = repo.observeCategories().first().single { it.id == "groceries" }
        assertEquals("Food renamed", category.nameEn)
        assertEquals("dining", category.iconKey)
        assertTrue(category.isArchived)
        assertEquals("groceries", repo.observeTransactions(null).first().first { it.id == expenseId }.categoryId)

        val expenseSlice = expenseBreakdown(repo.observeTransactions(null).first(), listOf(category), "en", "USD", emptyMap()).single()
        assertEquals("Food renamed", expenseSlice.name)
        assertEquals(6_000L, expenseSlice.minorUnits)

        val moved = moveHomeBlock(normalizeHomeLayout(emptyList()), HomeBlock.EXPENSES, 0)
            .map { if (it.id == HomeBlock.EXPENSES) it.copy(visible = true) else it }
        store.save(moved)
        assertEquals(HomeBlock.EXPENSES, HomeLayoutStore(app).layout.value.first().id)
        assertTrue(HomeLayoutStore(app).layout.value.first().visible)

        assertEquals(LockResult.SUCCESS, app.appLock.enable("Ab12", true, true))
        app.appLock.lockNow()
        assertEquals(LockResult.SUCCESS, app.appLock.unlock("Ab12"))
        assertEquals(2, repo.observeTransactions(null).first().size)
    }

    @Test fun selectedArchivedCategoryChartShowsHistoricalExpense() {
        val category = runBlocking {
            val editor = ProfileViewModel(repo)
            assertTrue(editor.renameCategory("groceries", "Food renamed", "dining"))
            assertTrue(editor.archiveCategory("groceries"))
            repo.observeCategories().first().single()
        }
        val slices = runBlocking { expenseBreakdown(repo.observeTransactions(null).first(), listOf(category), "en", "USD", emptyMap()) }
        ui.setContent { FinanceTheme { Column(Modifier.verticalScroll(rememberScrollState())) { MonthlyExpensesWidget(slices, "USD", "en") } } }
        ui.onNodeWithTag("expense_center").assertTextEquals("60.00 USD")
        ui.onNodeWithTag("expense_slice_groceries").performClick()
        ui.onNodeWithTag("expense_center").assertTextEquals("60.00 USD")
        assertTrue(ui.onAllNodesWithText("Food renamed", substring = true).fetchSemanticsNodes().isNotEmpty())
    }
}
