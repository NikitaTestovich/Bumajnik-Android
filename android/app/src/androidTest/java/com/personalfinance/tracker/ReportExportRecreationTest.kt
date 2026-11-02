package com.personalfinance.tracker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.core.app.ActivityOptionsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.ui.reports.ReportsScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReportExportRecreationTest {
    @get:Rule val ui = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication
    private var prepared = false
    private lateinit var output: File
    @Before fun prepare() = runBlocking {
        val emulated = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("getprop ro.kernel.qemu")).use { it.readBytes().toString(Charsets.UTF_8).trim() }
        check(emulated == "1")
        prepared = true
        app.container.database.clearAllTables()
        app.container.financeRepository.saveProfile(UserProfile("Export Demo", "en", "USD", true))
        app.container.financeRepository.saveTransaction(FinanceTransaction(UUID.randomUUID().toString(), "Export Demo", TransactionType.INCOME, LocalDate.now(), Money(100,"USD"), null, comment="Salary"))
        output = File.createTempFile("recreated-report", ".csv", app.cacheDir)
    }
    @After fun cleanup() { if (prepared) { app.container.database.clearAllTables(); if (::output.isInitialized) output.delete() } }

    @Test fun systemPickerExportsCurrentComments() {
        ui.setContent { FinanceTheme { ReportsScreen() } }
        val label = app.getString(R.string.reports_export_csv)
        ui.waitUntil(10000) { ui.onAllNodesWithText(label).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
        ui.onNodeWithText(label).performScrollTo().performClick()
        val automation = instrumentation.uiAutomation
        fun find(node: android.view.accessibility.AccessibilityNodeInfo?, predicate: (android.view.accessibility.AccessibilityNodeInfo) -> Boolean): android.view.accessibility.AccessibilityNodeInfo? {
            if (node == null) return null
            if (predicate(node)) return node
            for (index in 0 until node.childCount) find(node.getChild(index), predicate)?.let { return it }
            return null
        }
        fun awaitNode(predicate: (android.view.accessibility.AccessibilityNodeInfo) -> Boolean): android.view.accessibility.AccessibilityNodeInfo {
            val until = android.os.SystemClock.uptimeMillis() + 10000
            while (android.os.SystemClock.uptimeMillis() < until) {
                find(automation.rootInActiveWindow, predicate)?.let { return it }
                android.os.SystemClock.sleep(100)
            }
            error("System document picker control missing")
        }
        val filename = "finance-picker-${UUID.randomUUID()}.csv"
        val name = awaitNode { it.viewIdResourceName == "android:id/title" && it.isEditable }
        assertTrue(name.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
            putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, filename)
        }))
        assertTrue(awaitNode { it.isClickable && it.text?.toString()?.equals("Save", ignoreCase = true) == true }.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        ui.waitUntil(10000) { ui.onAllNodesWithText(app.getString(R.string.reports_export_success)).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
        // DocumentsUI remembers the last folder, which can be nested under Downloads.
        val csv = android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("find /sdcard/Download -type f -name $filename -exec cat {} +")).use { it.readBytes().toString(Charsets.UTF_8) }
        assertTrue(csv.startsWith("owner,record_id,date,category,income,expense,currency,comment"))
        assertTrue(csv.contains("Salary"))
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("find /sdcard/Download -type f -name $filename -delete")).close()
    }

    @Test fun pickerResultAfterSavedStateRestoreKeepsCommentsAndEightColumns() {
        var pendingCode: Int? = null
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) { pendingCode = requestCode }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        val restoration = StateRestorationTester(ui)
        restoration.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) { FinanceTheme { ReportsScreen() } } }
        val exportLabel = app.getString(R.string.reports_export_csv)
        ui.waitUntil(10000) { ui.onAllNodesWithText(exportLabel).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
        ui.onAllNodes(isToggleable()).assertCountEquals(0)
        ui.onNodeWithText(exportLabel).performScrollTo().performClick()
        ui.waitUntil(10000) { pendingCode != null }
        restoration.emulateSavedInstanceStateRestore()
        ui.onAllNodes(isToggleable()).assertCountEquals(0)
        ui.runOnIdle { registry.dispatchResult(pendingCode!!, Activity.RESULT_OK, Intent().setData(Uri.fromFile(output))) }
        ui.waitUntil(10000) { output.length() > 0 }
        assertEquals("owner,record_id,date,category,income,expense,currency,comment", output.readLines().first())
        assertTrue(output.readText().contains("Export Demo"))
        assertTrue(output.readText().contains("Salary"))
    }
}
