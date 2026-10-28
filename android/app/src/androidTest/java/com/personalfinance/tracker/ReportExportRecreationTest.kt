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

    @Test fun legacyPickerResultAfterSavedStateRestoreKeepsSevenColumns() {
        var pendingCode: Int? = null
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) { pendingCode = requestCode }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        val restoration = StateRestorationTester(ui)
        restoration.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) { FinanceTheme { ReportsScreen() } } }
        val exportLabel = app.getString(R.string.reports_export_csv)
        ui.waitUntil(10000) { ui.onAllNodesWithText(exportLabel).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
        ui.onNodeWithText(app.getString(R.string.reports_legacy_csv)).performScrollTo()
        ui.onNode(isToggleable()).performClick()
        ui.onNodeWithText(exportLabel).performScrollTo().performClick()
        ui.waitUntil(10000) { pendingCode != null }
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText(app.getString(R.string.reports_legacy_csv)).performScrollTo().assertIsOn()
        ui.runOnIdle { registry.dispatchResult(pendingCode!!, Activity.RESULT_OK, Intent().setData(Uri.fromFile(output))) }
        ui.waitUntil(10000) { output.length() > 0 }
        assertEquals("owner,record_id,date,category,income,expense,currency", output.readLines().first())
        assertTrue(output.readText().contains("Export Demo"))
        assertFalse(output.readText().contains("Salary"))
    }
}
