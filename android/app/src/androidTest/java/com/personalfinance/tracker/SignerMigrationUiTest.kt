package com.personalfinance.tracker

import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit two-installation migration test, using fictional data on a disposable emulator only. */
@RunWith(AndroidJUnit4::class)
class SignerMigrationUiTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication

    @Test fun transferFullBackupThroughSystemPicker() {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("signerPickerPhase")
        assumeTrue(phase == "export" || phase == "restore")
        check(Build.HARDWARE in listOf("ranchu", "goldfish"))
        val fileName = requireNotNull(args.getString("signerPickerFile"))
        require(fileName.matches(Regex("finance-migration-[a-z0-9-]+\\.ftb")))
        fun mark(value: String) { instrumentation.sendStatus(2, android.os.Bundle().apply { putString("stream", "MIGRATION_UI:$value\n") }) }
        ActivityScenario.launch(MainActivity::class.java).use {
            fun text(id: Int) = app.getString(id)
            fun await(id: Int) = ui.waitUntil(15_000) { ui.onAllNodesWithText(text(id)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            fun click(id: Int) {
                val matcher = hasText(text(id)) and hasClickAction()
                if (ui.onAllNodes(matcher and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) ui.onNode(matcher).performScrollTo()
                ui.onNode(matcher).performClick()
            }
            if (phase == "export") {
                mark("waiting-biometric")
                ui.waitUntil(15_000) { app.appLock.biometricEntry.phase.value == com.personalfinance.tracker.security.BiometricEntryPolicy.Phase.PROMPT_ACTIVE }
                assertTrue(instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
                mark("waiting-code")
                ui.waitUntil(15_000) { ui.onAllNodesWithTag("unlock_code").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
                ui.onNodeWithTag("unlock_code").performTextInput("Ab12")
                ui.onNodeWithTag("unlock_button").performClick()
            } else {
                await(R.string.welcome_title)
                ui.onNode(hasText(text(R.string.profile_name)) and hasSetTextAction()).performTextReplacement("Temporary")
                click(R.string.welcome_continue)
            }
            await(R.string.home_balance)
            mark("home")
            ui.onNode(hasText(text(R.string.navigation_more)) and hasClickAction()).performClick()
            ui.onNodeWithTag("open_BACKUP").performScrollTo().performClick()
            mark("backup-page")
            if (phase == "export") {
                val monitor = object : Instrumentation.ActivityMonitor() {
                    override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                        if (intent.action == Intent.ACTION_CREATE_DOCUMENT) intent.putExtra(Intent.EXTRA_TITLE, fileName)
                        return null
                    }
                }
                instrumentation.addMonitor(monitor)
                try { click(R.string.full_backup_export) } finally { instrumentation.removeMonitor(monitor) }
                mark("system-save")
                clickDocument(listOf("Save", "SAVE", "Сохранить", "СОХРАНИТЬ"))
                await(R.string.full_backup_export_done)
                mark("exported")
            } else {
                click(R.string.full_backup_restore)
                if (clickDocument(listOf(fileName, "FinanceTrackerTests")) != fileName) clickDocument(listOf(fileName))
                await(R.string.full_backup_confirm_title)
                ui.onNode(hasText(text(R.string.full_backup_restore)) and hasAnyAncestor(isDialog())).performClick()
                await(R.string.full_backup_restore_done)
                runBlocking {
                    val repo = app.container.financeRepository
                    assertEquals("Upgrade Demo", repo.observeProfile().first()?.name)
                    assertEquals(12345L, repo.observeTransactions(null).first().single().money.minorUnits)
                    assertEquals("groceries", repo.observeCategories().first().single().iconKey)
                    assertEquals(20000L, repo.observeGoal().first()?.saved?.minorUnits)
                    val range = PeriodRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))
                    assertEquals("Family Demo", app.container.familyRepository.observeFamilyTransactions(range).first().single().owner)
                    assertEquals(BigDecimal("3"), app.container.rateRepository.ratesFor(range, "BYN").first()["USD"])
                    assertEquals(8, app.themeStore.theme.value.id)
                    assertEquals(TransactionType.INCOME, app.operationsTabStore.selectedType.value)
                    assertEquals("summary:1;goal:0", app.getSharedPreferences("home_layout", 0).getString("blocks", ""))
                    assertFalse(app.getSharedPreferences("appearance", 0).getBoolean("category_icon_labels", true))
                    val layout = com.personalfinance.tracker.data.preferences.HomeLayoutStore(app)
                    try { assertFalse(layout.layout.value.single { it.id == com.personalfinance.tracker.ui.home.HomeBlock.GOAL }.visible) }
                    finally { layout.close() }
                    assertFalse(app.appLock.state.value.enabled)
                }
            }
        }
    }

    private fun clickDocument(labels: List<String>): String {
        var chosen: AccessibilityNodeInfo? = null
        fun clickable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            var current = node
            repeat(8) { if (current == null) return null; if (current!!.isClickable) return current; current = current!!.parent }
            return null
        }
        ui.waitUntil(15_000) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root == null || root.packageName == app.packageName) false else {
                chosen = labels.asSequence().flatMap { label -> root.findAccessibilityNodeInfosByText(label).asSequence().filter { it.text?.toString() == label } }
                    .firstOrNull { it.isVisibleToUser && clickable(it) != null }
                if (chosen == null) root.findAccessibilityNodeInfosByViewId("com.google.android.documentsui:id/dir_list").firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                chosen != null
            }
        }
        val node = requireNotNull(chosen)
        val label = node.text.toString()
        assertTrue(requireNotNull(clickable(node)).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        return label
    }
}
