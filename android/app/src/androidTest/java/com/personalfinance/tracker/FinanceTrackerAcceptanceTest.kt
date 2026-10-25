package com.personalfinance.tracker

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.csv.FamilyReportCalculator
import com.personalfinance.tracker.data.csv.FinanceCsvCodec
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against MainActivity, its application container, and the on-device Room database. */
@RunWith(AndroidJUnit4::class)
class FinanceTrackerAcceptanceTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication
    private val repository get() = app.container.financeRepository
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalLocales: androidx.core.os.LocaleListCompat? = null
    private val temporaryCsvUris = mutableListOf<Uri>()

    @Before fun resetLocalStateAndLaunch() {
        // The empty rule does not launch an activity before this reset.
        app.container.database.clearAllTables()
        instrumentation.runOnMainSync {
            originalLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText(R.string.welcome_title)
    }

    @After fun closeAndReset() {
        scenario?.close()
        temporaryCsvUris.forEach { app.contentResolver.delete(it, null, null) }
        temporaryCsvUris.clear()
        instrumentation.runOnMainSync {
            originalLocales?.let { androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(it) }
        }
        app.container.database.clearAllTables()
    }

    @Test fun onboardingPersistsProfileAndBottomNavigationOpensEveryDestination() {
        button(R.string.welcome_continue).assertIsNotEnabled()
        onboard()
        val profile = runBlocking { repository.observeProfile().first() }!!
        assertEquals("Acceptance User", profile.name)
        assertEquals("USD", profile.baseCurrency)
        assertTrue(profile.onboardingComplete)
        assertTrue(runBlocking { repository.observeCategories().first() }.isNotEmpty())
        navigate(R.string.navigation_operations)
        awaitText(R.string.operation_empty)
        navigate(R.string.navigation_reports)
        awaitText(R.string.reports_period)
        navigate(R.string.navigation_more)
        awaitText(R.string.profile_title)
        navigate(R.string.navigation_home)
        awaitText(R.string.home_balance)
        scenario!!.recreate()
        awaitText(R.string.home_balance)
        ui.onNodeWithText(text(R.string.welcome_title)).assertDoesNotExist()
    }

    @Test fun incomeCanBeCreatedEditedAndDeletedOnlyAfterConfirmation() {
        onboard()
        click(R.string.home_add_income)
        awaitTransactionForm(R.string.operation_new_title)
        ui.onNode(hasText(text(R.string.operation_currency)) and hasSetTextAction()).assertDoesNotExist()
        fill(R.string.operation_date, "21-10-2026")
        fill(R.string.operation_amount, "123.45")
        saveTransaction()
        navigate(R.string.navigation_operations)
        click(R.string.operation_income)
        awaitLiteral("123.45 USD")
        awaitLiteral("21-10-2026")
        var stored = runBlocking { repository.observeTransactions(null).first() }.single()
        val id = stored.id
        assertEquals(TransactionType.INCOME, stored.type)
        assertEquals(java.time.LocalDate.of(2026, 10, 21), stored.date)
        assertEquals(12345L, stored.money.minorUnits)
        click(R.string.operation_edit)
        awaitTransactionForm(R.string.operation_edit_title)
        fill(R.string.operation_amount, "87.65")
        saveTransaction()
        click(R.string.operation_income)
        awaitLiteral("87.65 USD")
        stored = runBlocking { repository.observeTransactions(null).first() }.single()
        assertEquals(id, stored.id)
        assertEquals(8765L, stored.money.minorUnits)
        click(R.string.operation_delete)
        awaitText(R.string.operation_delete_title)
        assertEquals(1, runBlocking { repository.observeTransactions(null).first() }.size)
        click(R.string.operation_cancel)
        awaitLiteral("87.65 USD")
        assertEquals(1, runBlocking { repository.observeTransactions(null).first() }.size)
        click(R.string.operation_delete)
        awaitText(R.string.operation_delete_title)
        // The row and dialog both say Delete; restrict the action to the dialog subtree.
        ui.onNode(hasText(text(R.string.operation_delete_confirm)) and
            androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isDialog()))
            .performClick()
        awaitText(R.string.operation_empty)
        assertTrue(runBlocking { repository.observeTransactions(null).first() }.isEmpty())
    }

    @Test fun homeSummaryUsesThemeAndKeepsTransactionAndGoalDetails() {
        onboard()
        val profile = runBlocking { repository.observeProfile().first() }!!
        val today = LocalDate.now()
        runBlocking {
            repository.saveTransaction(FinanceTransaction(
                id = UUID.randomUUID().toString(), owner = profile.name, type = TransactionType.INCOME,
                date = today.withDayOfMonth(1), money = Money(100_000, "USD"), categoryId = null,
            ))
            repository.saveTransaction(FinanceTransaction(
                id = UUID.randomUUID().toString(), owner = profile.name, type = TransactionType.EXPENSE,
                date = today, money = Money(20_000, "USD"), categoryId = "groceries",
            ))
            repository.saveGoal(SavingsGoal(target = Money(100_000, "USD"), saved = Money(25_000, "USD")))
        }
        awaitLiteral("+800.00 USD")
        ui.onNodeWithText("+800.00 USD").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("${text(R.string.home_income)}: 1000.00 USD").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("${text(R.string.home_expenses)}: 200.00 USD").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText(text(R.string.home_latest)).performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("200.00 USD").performScrollTo().assertIsDisplayed()
        val progress = text(R.string.goal_progress, "250.00 USD", "1000.00 USD")
        ui.onNodeWithText(progress).performScrollTo().assertIsDisplayed()
        ui.onNodeWithContentDescription(text(R.string.home_goal_progress, 25))
            .performScrollTo().assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.25f, 0f..1f))
    }

    @Test fun dateFieldOpensPickerAndStillAcceptsManualInput() {
        onboard()
        click(R.string.home_add_income)
        awaitTransactionForm(R.string.operation_new_title)
        ui.onNodeWithContentDescription(text(R.string.date_picker_open)).performClick()
        awaitText(R.string.date_picker_title)
        ui.onNode(
            hasText(text(R.string.date_picker_cancel)) and
                hasAnyAncestor(androidx.compose.ui.test.isDialog()),
        ).performClick()
        fill(R.string.operation_date, "21-10-2026")
        fill(R.string.operation_amount, "10")
        saveTransaction()
        val saved = runBlocking { repository.observeTransactions(null).first() }.single()
        assertEquals(LocalDate.of(2026, 10, 21), saved.date)
    }

    @Test fun expenseRequiresCategoryAndSavesSelectedCategory() {
        onboard()
        click(R.string.home_add_expense)
        awaitTransactionForm(R.string.operation_new_title)
        awaitText(R.string.operation_category_profile_hint)
        fill(R.string.operation_amount, "12.34")
        button(R.string.operation_save).performClick()
        awaitText(R.string.operation_error_category)
        click(R.string.operation_select_category)
        val category = runBlocking { repository.observeCategories().first() }.first()
        val profile = runBlocking { repository.observeProfile().first() }!!
        val label = if (profile.languageTag == "ru") category.nameRu!! else category.nameEn!!
        ui.onNodeWithText(label).performClick()
        saveTransaction()
        navigate(R.string.navigation_operations)
        awaitLiteral("12.34 USD")
        val stored = runBlocking { repository.observeTransactions(null).first() }.single()
        assertEquals(TransactionType.EXPENSE, stored.type)
        assertEquals(category.id, stored.categoryId)
    }

    @Test fun profileSavesNameCurrencyAndBothLanguagesAcrossRecreation() {
        onboard()
        navigate(R.string.navigation_more)
        awaitText(R.string.profile_title)
        fill(R.string.profile_name, "Updated User")
        ui.onNodeWithText("Русский").performClick()
        ui.onNodeWithText("USD").performClick()
        ui.onNodeWithText("EUR").performClick()
        click(R.string.profile_save)
        awaitProfile("ru", "EUR")
        // Locale changes recreate MainActivity; navigate again using its current resources.
        navigate(R.string.navigation_more)
        awaitText(R.string.profile_title)
        ui.onNodeWithText("Русский").assertIsSelected()
        ui.onNodeWithText("EUR").assertIsDisplayed()
        assertEquals("Updated User", runBlocking { repository.observeProfile().first() }!!.name)
        ui.onNodeWithText("English").performClick()
        click(R.string.profile_save)
        awaitProfile("en", "EUR")
        scenario!!.recreate()
        navigate(R.string.navigation_more)
        awaitText(R.string.profile_title)
        ui.onNodeWithText("English").assertIsSelected()
        ui.onNodeWithText("EUR").assertIsDisplayed()
        assertEquals("Updated User", runBlocking { repository.observeProfile().first() }!!.name)
    }

    @Test fun goalShowsEmptyStateThenPersistsProgressWithoutInventingForecast() {
        onboard()
        awaitText(R.string.home_no_goal)
        click(R.string.goal_open)
        awaitText(R.string.goal_create_hint)
        button(R.string.goal_save).assertIsNotEnabled()
        fill(R.string.goal_target, "1000", "USD")
        fill(R.string.goal_saved, "250", "USD")
        click(R.string.goal_save)
        awaitText(R.string.goal_no_completed_months)
        val progress = text(R.string.goal_progress, "250.00 USD", "1000.00 USD")
        ui.onNodeWithText(progress).performScrollTo().assertIsDisplayed()
        val goal = runBlocking { repository.observeGoal().first() }!!
        assertEquals(100000L, goal.target.minorUnits)
        assertEquals(25000L, goal.saved.minorUnits)
        click(R.string.family_back)
        navigate(R.string.navigation_more)
        click(R.string.goal_open)
        awaitText(R.string.goal_no_completed_months)
        ui.onNodeWithText(progress).performScrollTo().assertIsDisplayed()
        ui.onNodeWithText(text(R.string.goal_create_hint)).assertDoesNotExist()
    }

    @Test fun goalScreenShowsAccessibleProgressAndExistingForecastStates() {
        onboard()
        runBlocking { repository.saveGoal(SavingsGoal(Money(100_000L, "USD"), Money(25_000L, "USD"))) }
        click(R.string.goal_open)
        awaitText(R.string.goal_no_completed_months)
        val progress = text(R.string.goal_progress, "250.00 USD", "1000.00 USD")
        ui.onNodeWithText(progress).performScrollTo().assertIsDisplayed()
        ui.onNodeWithContentDescription(progress).performScrollTo().assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.25f, 0f..1f))
        ui.onNodeWithText(text(R.string.goal_estimate, 0)).assertDoesNotExist()

        val profile = runBlocking { repository.observeProfile().first() }!!
        val previousMonth = LocalDate.now().withDayOfMonth(1).minusMonths(1)
        val income = FinanceTransaction(UUID.randomUUID().toString(), profile.name,
            TransactionType.INCOME, previousMonth, Money(100_000L, "USD"), null)
        val expense = FinanceTransaction(UUID.randomUUID().toString(), profile.name,
            TransactionType.EXPENSE, previousMonth, Money(75_000L, "USD"), "groceries")
        runBlocking {
            repository.saveTransaction(income)
            repository.saveTransaction(expense)
        }
        awaitLiteral(text(R.string.goal_estimate, 3))
        ui.onNodeWithText(text(R.string.goal_average_surplus, "250.00", "USD"))
            .performScrollTo().assertIsDisplayed()

        runBlocking { repository.saveTransaction(expense.copy(money = Money(100_000L, "USD"))) }
        awaitText(R.string.goal_no_positive_surplus)
        ui.onNodeWithText(text(R.string.goal_estimate, 3)).assertDoesNotExist()
        ui.onNodeWithText(text(R.string.goal_estimate, 0)).assertDoesNotExist()

        runBlocking { repository.saveGoal(SavingsGoal(Money(100_000L, "USD"), Money(125_000L, "USD"))) }
        awaitText(R.string.goal_reached)
        val reached = text(R.string.goal_progress, "1250.00 USD", "1000.00 USD")
        ui.onNodeWithContentDescription(reached).performScrollTo().assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
        ui.onNodeWithText(text(R.string.goal_estimate, 0)).assertDoesNotExist()
    }

    @Test fun reportScreenShowsPairedPeriodColumnsAndCategoryShares() {
        onboard()
        val profile = runBlocking { repository.observeProfile().first() }!!
        val categories = runBlocking { repository.observeCategories().first() }.take(2)
        val start = LocalDate.now().withDayOfMonth(1)
        runBlocking {
            listOf(
                Triple(start, TransactionType.INCOME, 200_000L),
                Triple(start.minusDays(1), TransactionType.INCOME, 100_000L),
                Triple(start.minusDays(1), TransactionType.EXPENSE, 10_000L),
            ).forEach { (date, type, amount) ->
                repository.saveTransaction(FinanceTransaction(UUID.randomUUID().toString(), profile.name,
                    type, date, Money(amount, "USD"), if (type == TransactionType.EXPENSE) categories[0].id else null))
            }
            categories.forEachIndexed { index, category ->
                repository.saveTransaction(FinanceTransaction(UUID.randomUUID().toString(), profile.name,
                    TransactionType.EXPENSE, start, Money(if (index == 0) 30_000L else 10_000L, "USD"), category.id))
            }
        }
        navigate(R.string.navigation_reports)
        awaitText(R.string.reports_period)
        // Apply an explicit range so this test does not depend on initial effect timing.
        fill(R.string.reports_to, LocalDate.now().plusDays(1)
            .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu")))
        click(R.string.reports_apply)
        awaitText(R.string.reports_comparison)
        listOf(
            Triple(R.string.chart_income, R.string.chart_selected_period, "2000.00"),
            Triple(R.string.chart_income, R.string.chart_previous_period, "1000.00"),
            Triple(R.string.chart_expense, R.string.chart_selected_period, "400.00"),
            Triple(R.string.chart_expense, R.string.chart_previous_period, "100.00"),
        ).forEach { (metric, period, amount) ->
            ui.onNodeWithContentDescription(text(R.string.chart_value_description, text(metric), text(period), amount))
                .performScrollTo().assertIsDisplayed()
        }
        val percent = java.text.NumberFormat.getPercentInstance(app.resources.configuration.locales[0])
        categories.forEachIndexed { index, category ->
            val name = if (profile.languageTag == "ru") category.nameRu!! else category.nameEn!!
            val amount = if (index == 0) "300.00" else "100.00"
            val share = text(R.string.chart_category_share, percent.format(if (index == 0) 0.75 else 0.25))
            ui.onNodeWithContentDescription(text(R.string.chart_category_description, name, amount, share))
                .performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun emptyProfileReportInitializesWithoutApplyingPeriod() {
        onboard()
        navigate(R.string.navigation_reports)
        awaitText(R.string.reports_no_expenses)
        ui.onNodeWithText(text(R.string.reports_no_expenses)).performScrollTo().assertIsDisplayed()
        ui.onNodeWithContentDescription(text(R.string.chart_category_breakdown)).assertDoesNotExist()
        ui.onNodeWithContentDescription(text(R.string.chart_income_expense_comparison)).assertDoesNotExist()
        button(R.string.reports_export_csv).assertIsEnabled()
    }

    @Test fun reportWithoutExpensesKeepsEmptyCategoryState() {
        onboard()
        val profile = runBlocking { repository.observeProfile().first() }!!
        runBlocking {
            repository.saveTransaction(FinanceTransaction(UUID.randomUUID().toString(), profile.name,
                TransactionType.INCOME, LocalDate.now(), Money(10_000L, "USD"), null))
        }
        navigate(R.string.navigation_reports)
        awaitText(R.string.reports_no_expenses)
        ui.onNodeWithText(text(R.string.reports_no_expenses)).performScrollTo().assertIsDisplayed()
        ui.onNodeWithContentDescription(text(R.string.chart_category_breakdown)).assertDoesNotExist()
    }

    @Test fun reportWithMissingRatesHidesChartsAndKeepsRatePrompt() {
        onboard()
        val profile = runBlocking { repository.observeProfile().first() }!!
        runBlocking {
            repository.saveTransaction(FinanceTransaction(UUID.randomUUID().toString(), profile.name,
                TransactionType.INCOME, LocalDate.now(), Money(10_000L, "EUR"), null))
        }
        navigate(R.string.navigation_reports)
        awaitText(R.string.exchange_rate_title)
        ui.onNodeWithContentDescription(text(R.string.chart_income_expense_comparison)).assertDoesNotExist()
        ui.onNodeWithContentDescription(text(R.string.chart_category_breakdown)).assertDoesNotExist()
        click(R.string.operation_cancel)
        awaitLiteral(text(R.string.reports_need_rates, "EUR"))
        ui.onNodeWithText(text(R.string.reports_need_rates, "EUR")).performScrollTo().assertIsDisplayed()
    }

    @Test fun reportExportLaunchesCreateDocumentAndCancelReturnsToReport() {
        onboard()
        navigate(R.string.navigation_reports)
        awaitText(R.string.reports_period)
        ui.waitUntil(10_000) {
            ui.onAllNodes(hasText(text(R.string.reports_export_csv)) and
                androidx.compose.ui.test.isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        launchPickerAndCancel(R.string.reports_export_csv, Intent.ACTION_CREATE_DOCUMENT) { intent ->
            assertEquals("text/csv", intent.type)
            assertTrue(intent.getStringExtra(Intent.EXTRA_TITLE)!!.endsWith(".csv"))
        }
        awaitText(R.string.reports_period)
    }

    @Test fun familyImportLaunchesOpenDocumentAndCancelKeepsEmptyState() {
        onboard()
        navigate(R.string.navigation_more)
        click(R.string.family_open)
        awaitText(R.string.family_no_records)
        launchPickerAndCancel(R.string.family_import, Intent.ACTION_OPEN_DOCUMENT) { intent ->
            assertTrue(intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.contains("text/csv"))
        }
        awaitText(R.string.family_no_records)
        assertTrue(runBlocking { repository.observeTransactions(null).first() }.isEmpty())
    }

    @Test fun reportExportWritesAValidCsvToTheSelectedDocument() {
        onboard()
        val profile = runBlocking { repository.observeProfile().first() }!!
        val groceries = runBlocking { repository.observeCategories().first() }.first { it.id == "groceries" }
        val today = LocalDate.now()
        val income = FinanceTransaction(
            id = UUID.randomUUID().toString(), owner = profile.name, type = TransactionType.INCOME,
            date = today, money = Money(125_000, "USD"), categoryId = null,
        )
        val expense = FinanceTransaction(
            id = UUID.randomUUID().toString(), owner = profile.name, type = TransactionType.EXPENSE,
            date = today, money = Money(1_730, "USD"), categoryId = groceries.id,
        )
        runBlocking {
            repository.saveTransaction(income)
            repository.saveTransaction(expense)
        }

        navigate(R.string.navigation_reports)
        ui.waitUntil(10_000) {
            ui.onAllNodes(hasText(text(R.string.reports_export_csv)) and androidx.compose.ui.test.isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        val destination = createTemporaryCsv("report", ByteArray(0))
        launchPickerAndReturnUri(R.string.reports_export_csv, Intent.ACTION_CREATE_DOCUMENT, destination) { intent ->
            assertEquals("text/csv", intent.type)
            assertTrue(intent.getStringExtra(Intent.EXTRA_TITLE)!!.endsWith(".csv"))
        }
        awaitText(R.string.reports_export_success)

        val exported = app.contentResolver.openInputStream(destination)!!.use { it.readBytes() }
        val decoded = FinanceCsvCodec.decode(exported)
        assertTrue(decoded.errors.toString(), decoded.errors.isEmpty())
        assertEquals(2, decoded.records.size)
        val exportedIncome = decoded.records.single { it.type == TransactionType.INCOME }
        val exportedExpense = decoded.records.single { it.type == TransactionType.EXPENSE }
        assertEquals(profile.name, exportedIncome.owner)
        assertEquals(today, exportedIncome.date)
        assertEquals(Money(125_000, "USD"), exportedIncome.amount)
        assertEquals(today, exportedExpense.date)
        assertEquals(Money(1_730, "USD"), exportedExpense.amount)
        assertEquals(
            if (profile.languageTag == "ru") groceries.nameRu else groceries.nameEn,
            exportedExpense.categoryName,
        )
    }

    @Test fun familyCsvImportConvertsTotalsAndSkipsTheSameFileOnRepeat() {
        onboard()
        navigate(R.string.navigation_more)
        click(R.string.family_open)
        awaitText(R.string.family_no_records)
        val csv = instrumentation.context.assets.open("wife-anna-rub.csv").use { it.readBytes() }
        val source = createTemporaryCsv("wife-anna-rub", csv)

        launchPickerAndReturnUri(R.string.family_import, Intent.ACTION_OPEN_DOCUMENT, source) { intent ->
            assertTrue(intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.contains("text/csv"))
        }
        awaitText(R.string.exchange_rate_title)
        fill(R.string.exchange_rate_label, "0.01", "RUB", "USD")
        click(R.string.exchange_rate_save)
        awaitLiteral(text(R.string.family_import_done, 9, 0))

        val range = PeriodRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))
        var imported = runBlocking { app.container.familyRepository.observeFamilyTransactions(range).first() }
        assertEquals(9, imported.size)
        assertTrue(imported.all { it.owner == "Анна Иванова" })
        assertTrue(imported.all { it.importBaseCurrency == "USD" && it.importRate == BigDecimal("0.01") })
        val totals = FamilyReportCalculator.calculate(imported, "USD", emptyMap())
        assertEquals(83_500L, totals.incomeMinorUnits)
        assertEquals(48_700L, totals.expensesMinorUnits)

        launchPickerAndReturnUri(R.string.family_import, Intent.ACTION_OPEN_DOCUMENT, source) { intent ->
            assertTrue(intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.contains("text/csv"))
        }
        awaitText(R.string.exchange_rate_title)
        fill(R.string.exchange_rate_label, "0.01", "RUB", "USD")
        click(R.string.exchange_rate_save)
        awaitLiteral(text(R.string.family_import_done, 0, 9))
        imported = runBlocking { app.container.familyRepository.observeFamilyTransactions(range).first() }
        assertEquals(9, imported.size)
    }

    @Test fun familyReportShowsPerMemberComparisonAndCategoryShares() {
        onboard()
        navigate(R.string.navigation_more)
        click(R.string.family_open)
        awaitText(R.string.family_no_records)
        val owner = "Family Member"
        val longCategory = "Education and professional development for the whole family"
        val csv = FinanceCsvCodec.encode(listOf(
            FinanceTransaction(UUID.randomUUID().toString(), owner, TransactionType.INCOME,
                LocalDate.now(), Money(100_000L, "EUR"), null),
            FinanceTransaction(UUID.randomUUID().toString(), owner, TransactionType.EXPENSE,
                LocalDate.now(), Money(30_000L, "EUR"), "food"),
            FinanceTransaction(UUID.randomUUID().toString(), owner, TransactionType.EXPENSE,
                LocalDate.now(), Money(10_000L, "EUR"), "education"),
        ), owner, mapOf("food" to "Groceries", "education" to longCategory))
        val source = createTemporaryCsv("family-chart", csv)
        launchPickerAndReturnUri(R.string.family_import, Intent.ACTION_OPEN_DOCUMENT, source) { }
        awaitText(R.string.exchange_rate_title)
        fill(R.string.exchange_rate_label, "2", "EUR", "USD")
        click(R.string.exchange_rate_save)
        awaitLiteral(text(R.string.family_import_done, 3, 0))
        ui.onNodeWithText(owner).performScrollTo().assertIsDisplayed()
        listOf(R.string.chart_income to "2000.00", R.string.chart_expense to "800.00").forEach { (metric, amount) ->
            ui.onNodeWithContentDescription(text(R.string.chart_value_description, owner, text(metric), amount))
                .performScrollTo().assertIsDisplayed()
        }
        val percent = java.text.NumberFormat.getPercentInstance(app.resources.configuration.locales[0])
        listOf(Triple("Groceries", "600.00", 0.75), Triple(longCategory, "200.00", 0.25)).forEach { (name, amount, fraction) ->
            val share = text(R.string.chart_category_share, percent.format(fraction))
            ui.onNodeWithContentDescription(text(R.string.chart_category_description, name, amount, share))
                .performScrollTo().assertIsDisplayed()
        }
        click(R.string.family_clear_member)
        awaitText(R.string.family_clear_title)
        click(R.string.operation_cancel)
        assertEquals(3, runBlocking { app.container.familyRepository.observeFamilyTransactions(
            PeriodRange(LocalDate.now().withDayOfMonth(1), LocalDate.now().plusDays(1))).first() }.size)
        click(R.string.family_clear_member)
        click(R.string.family_clear_confirm)
        awaitText(R.string.family_no_records)
    }

    @Test fun familyWithForeignPersonalRecordsHidesChartsUntilRatesProvided() {
        onboard()
        val profile = runBlocking { repository.observeProfile().first() }!!
        runBlocking {
            repository.saveTransaction(FinanceTransaction(UUID.randomUUID().toString(), profile.name,
                TransactionType.INCOME, LocalDate.now(), Money(10_000L, "EUR"), null))
        }
        navigate(R.string.navigation_more)
        click(R.string.family_open)
        awaitLiteral(text(R.string.family_need_rates, "EUR", "USD"))
        ui.onNodeWithContentDescription(text(R.string.chart_income_expense_comparison)).assertDoesNotExist()
        ui.onNodeWithContentDescription(text(R.string.chart_category_breakdown)).assertDoesNotExist()
        click(R.string.exchange_rate_open)
        awaitText(R.string.exchange_rate_title)
        fill(R.string.exchange_rate_label, "2", "EUR", "USD")
        click(R.string.exchange_rate_save)
        ui.onNodeWithContentDescription(text(R.string.chart_value_description, profile.name, text(R.string.chart_income), "200.00"))
            .performScrollTo().assertIsDisplayed()
    }

    private fun onboard() {
        fill(R.string.profile_name, "Acceptance User")
        click(R.string.welcome_continue)
        awaitText(R.string.home_balance)
    }

    private fun navigate(@StringRes destination: Int) {
        val label = text(destination)
        ui.waitUntil(10_000) {
            ui.onAllNodes(hasText(label) and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNode(hasText(label) and hasClickAction()).performClick().assertIsSelected()
    }

    private fun text(@StringRes resource: Int, vararg args: Any): String {
        var value = ""
        scenario!!.onActivity { value = it.getString(resource, *args) }
        return value
    }

    private fun button(@StringRes resource: Int) = ui.onNode(hasText(text(resource)) and hasClickAction())

    private fun click(@StringRes resource: Int) {
        val matcher = hasText(text(resource)) and hasClickAction()
        if (ui.onAllNodes(matcher and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) {
            ui.onNode(matcher).performScrollTo()
        }
        ui.onNode(matcher).performClick()
    }

    private fun fill(@StringRes resource: Int, value: String, vararg args: Any) {
        val matcher = hasText(text(resource, *args)) and hasSetTextAction()
        if (ui.onAllNodes(matcher and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) {
            ui.onNode(matcher).performScrollTo()
        }
        ui.onNode(matcher).performTextReplacement(value)
        scenario!!.onActivity { activity ->
            val keyboard = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        ui.waitForIdle()
    }

    private fun awaitTransactionForm(@StringRes title: Int) {
        awaitText(title)
        // Save becomes enabled only after the asynchronous draft load has finished.
        // Entering text before that point allows the loaded draft to overwrite it.
        val saveLabel = text(R.string.operation_save)
        ui.waitUntil(10_000) {
            ui.onAllNodes(hasText(saveLabel) and androidx.compose.ui.test.isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        button(R.string.operation_save).assertIsEnabled()
    }

    private fun saveTransaction() {
        button(R.string.operation_save).assertIsEnabled().performClick()
        ui.waitUntil(10_000) {
            ui.onAllNodesWithText(text(R.string.operation_save)).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun awaitText(@StringRes resource: Int) = awaitLiteral(text(resource))

    private fun awaitLiteral(value: String) {
        ui.waitUntil(10_000) { ui.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitProfile(language: String, currency: String) {
        ui.waitUntil(10_000) {
            val profile = runBlocking { repository.observeProfile().first() }
            var activeLanguage: String? = null
            scenario!!.onActivity { activeLanguage = it.resources.configuration.locales[0].language }
            profile?.languageTag == language && profile.baseCurrency == currency && activeLanguage == language
        }
    }

    /** Observe the actual outgoing intent and external window, then cancel the system picker. */
    private fun launchPickerAndCancel(
        @StringRes action: Int,
        expectedAction: String,
        assertContract: (Intent) -> Unit,
    ) {
        val probe = Intent(expectedAction).addCategory(Intent.CATEGORY_OPENABLE).setType(
            if (expectedAction == Intent.ACTION_CREATE_DOCUMENT) "text/csv" else "*/*",
        )
        assumeTrue("This device has no system document picker", probe.resolveActivity(app.packageManager) != null)
        val captured = AtomicReference<Intent>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action == expectedAction) captured.set(Intent(intent))
                return null // Let Android open the real system picker.
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            click(action)
            ui.waitUntil(10_000) { captured.get() != null }
            val intent = captured.get()
            assertNotNull(intent)
            assertTrue("Captured intent did not include OPENABLE: action=${intent.action}, type=${intent.type}, categories=${intent.categories}", intent.hasCategory(Intent.CATEGORY_OPENABLE))
            assertContract(intent)
            ui.waitUntil(10_000) {
                val root = instrumentation.uiAutomation.rootInActiveWindow
                root != null && root.packageName != app.packageName
            }
            assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
            ui.waitUntil(10_000) {
                instrumentation.uiAutomation.rootInActiveWindow?.packageName == app.packageName
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun launchPickerAndReturnUri(
        @StringRes action: Int,
        expectedAction: String,
        selectedUri: Uri,
        assertContract: (Intent) -> Unit,
    ) {
        val captured = AtomicReference<Intent>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != expectedAction) return null
                captured.set(Intent(intent))
                val result = Intent().setData(selectedUri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                return Instrumentation.ActivityResult(Activity.RESULT_OK, result)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            click(action)
            ui.waitUntil(10_000) { captured.get() != null }
            val intent = captured.get()
            assertContract(intent)
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun createTemporaryCsv(name: String, contents: ByteArray): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "finance-test-$name-${UUID.randomUUID()}.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/FinanceTrackerTests")
        }
        val uri = requireNotNull(app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        app.contentResolver.openOutputStream(uri, "w")!!.use { it.write(contents) }
        temporaryCsvUris += uri
        return uri
    }
}
