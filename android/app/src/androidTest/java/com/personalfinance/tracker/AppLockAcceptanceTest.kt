package com.personalfinance.tracker

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.WindowManager
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.security.AppLockController
import com.personalfinance.tracker.security.AppLockStore
import com.personalfinance.tracker.security.LockResult
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockAcceptanceTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun prepare() {
        runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
        app.container.database.clearAllTables()
        runBlocking {
            app.container.financeRepository.saveProfile(UserProfile("Security Demo", "en", "USD", true))
            app.container.financeRepository.saveTransaction(FinanceTransaction(
                "55555555-5555-4555-8555-555555555555", "Security Demo", TransactionType.INCOME,
                LocalDate.now(), Money(12345, "USD"), null))
        }
    }

    @After fun cleanup() {
        scenario?.close()
        runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
        app.container.database.clearAllTables()
    }

    private fun launchLocked() {
        runBlocking { assertEquals(LockResult.SUCCESS, app.appLock.enable("Ab12", false, true)) }
        app.appLock.lockNow()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("app_lock_screen").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    }

    @Test fun wrongCodeCannotOpenNavigationAndCorrectCodePreservesRecords() {
        launchLocked()
        ui.onNodeWithText(app.getString(R.string.navigation_operations)).assertDoesNotExist()
        ui.onNodeWithTag("unlock_code").performTextInput("0000")
        ui.onNodeWithTag("unlock_button").performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText(app.getString(R.string.security_wrong_code)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        ui.onNodeWithTag("app_lock_screen").assertIsDisplayed()
        ui.onNodeWithTag("unlock_code").performTextReplacement("Ab12")
        ui.onNodeWithTag("unlock_button").performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText(app.getString(R.string.home_balance)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        assertEquals(12345L, runBlocking { app.container.financeRepository.observeTransactions(null).first().single().money.minorUnits })
        scenario!!.onActivity { assertTrue(it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0) }
        // Screen-off is the production controller signal; navigation must disappear.
        scenario!!.onActivity { app.appLock.lockNow() }
        ui.onNodeWithTag("app_lock_screen").assertIsDisplayed()
        ui.onNodeWithText(app.getString(R.string.home_balance)).assertDoesNotExist()
    }

    @Test fun encryptedVerifierSurvivesReloadAndContainsNoPlaintextCode() {
        launchLocked()
        val xml = app.getSharedPreferences("app_lock", 0).all.values.joinToString()
        assertFalse(xml.contains("Ab12"))
        assertFalse(xml.contains("hash"))
        val fresh = AppLockController(AppLockStore(app))
        assertTrue(fresh.state.value.locked)
        assertEquals(LockResult.SUCCESS, runBlocking { fresh.unlock("Ab12") })
    }

    @Test fun profileCanEnableChangeAndDisableCodeWithCurrentAuthentication() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ui.waitUntil(10_000) { ui.onAllNodesWithText(app.getString(R.string.home_balance)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        var secure = false
        scenario!!.onActivity { secure = it.isDeviceSecure() }
        Assume.assumeTrue("Configure emulator PIN for settings tests", secure)
        ui.onNode(hasText(app.getString(R.string.navigation_more)) and hasClickAction()).performClick()
        ui.onNodeWithTag("new_code").performScrollTo().performTextInput("Ab12")
        ui.onNodeWithTag("confirm_code").performScrollTo().performTextInput("Ab12")
        ui.onNodeWithTag("save_security").performScrollTo().performClick()
        ui.waitUntil(10_000) { app.appLock.state.value.enabled }
        ui.onNodeWithTag("current_code").performScrollTo().performTextInput("0000")
        ui.onNodeWithTag("new_code").performScrollTo().performTextReplacement("Cd34")
        ui.onNodeWithTag("confirm_code").performScrollTo().performTextReplacement("Cd34")
        ui.onNodeWithTag("save_security").performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText(app.getString(R.string.security_wrong_code)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        ui.onNodeWithTag("current_code").performScrollTo().performTextReplacement("Ab12")
        ui.onNodeWithTag("save_security").performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText(app.getString(R.string.security_saved)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        scenario!!.onActivity { app.appLock.lockNow() }
        ui.onNodeWithTag("unlock_code").performTextInput("Cd34")
        ui.onNodeWithTag("unlock_button").performClick()
        ui.waitUntil(10_000) { !app.appLock.state.value.locked }
        ui.onNode(hasText(app.getString(R.string.navigation_more)) and hasClickAction()).performClick()
        ui.onNodeWithTag("current_code").performScrollTo().performTextInput("Cd34")
        ui.onNodeWithTag("disable_security").performScrollTo().performClick()
        ui.waitUntil(10_000) { !app.appLock.state.value.enabled }
        assertEquals(1, runBlocking { app.container.financeRepository.observeTransactions(null).first().size })
    }

    @Test fun canceledDeviceCredentialRecoveryKeepsProtectionAndRecords() {
        launchLocked()
        // Requires a configured emulator screen lock. The OS confirmation itself is checked separately.
        var secure = false
        scenario!!.onActivity { secure = it.isDeviceSecure() }
        Assume.assumeTrue("Configure emulator PIN for recovery tests", secure)
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent?): Instrumentation.ActivityResult? =
                if (intent?.action == "android.app.action.CONFIRM_DEVICE_CREDENTIAL")
                    Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null) else null
        }
        instrumentation.addMonitor(monitor)
        try {
            ui.onNodeWithText(app.getString(R.string.security_forgot)).performClick()
            ui.waitForIdle()
            assertTrue(app.appLock.state.value.enabled)
            assertTrue(app.appLock.state.value.locked)
            assertEquals(1, runBlocking { app.container.financeRepository.observeTransactions(null).first().size })
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun confirmedDeviceCredentialRecoveryDisablesOnlyProtection() {
        launchLocked()
        var secure = false
        scenario!!.onActivity { secure = it.isDeviceSecure() }
        Assume.assumeTrue("Configure emulator PIN for recovery tests", secure)
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent?): Instrumentation.ActivityResult? =
                if (intent?.action == "android.app.action.CONFIRM_DEVICE_CREDENTIAL")
                    Instrumentation.ActivityResult(Activity.RESULT_OK, null) else null
        }
        instrumentation.addMonitor(monitor)
        try {
            ui.onNodeWithText(app.getString(R.string.security_forgot)).performClick()
            ui.waitUntil(10_000) { !app.appLock.state.value.enabled }
            assertEquals("Security Demo", runBlocking { app.container.financeRepository.observeProfile().first() }!!.name)
            assertEquals(12345L, runBlocking { app.container.financeRepository.observeTransactions(null).first().single().money.minorUnits })
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun realSystemPinRecoversAccessWithoutDeletingRecords() {
        val pin = InstrumentationRegistry.getArguments().getString("devicePin")
        Assume.assumeTrue("Supply emulator-only devicePin to exercise the real Android prompt", pin?.matches(Regex("[0-9]{4,10}")) == true)
        launchLocked()
        ui.onNodeWithText(app.getString(R.string.security_forgot)).performClick()
        fun findNode(node: android.view.accessibility.AccessibilityNodeInfo?, matches: (android.view.accessibility.AccessibilityNodeInfo) -> Boolean): android.view.accessibility.AccessibilityNodeInfo? {
            if (node == null) return null
            if (matches(node)) return node
            for (index in 0 until node.childCount) findNode(node.getChild(index), matches)?.let { return it }
            return null
        }
        val deadline = SystemClock.elapsedRealtime() + 10_000
        var systemWindow = false
        while (SystemClock.elapsedRealtime() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null && root.packageName?.toString() != app.packageName &&
                root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT) != null) {
                systemWindow = true
                break
            }
            SystemClock.sleep(100)
        }
        assertTrue("Android credential input must be visible before entering the emulator PIN", systemWindow)
        assertTrue(app.appLock.state.value.locked)
        // API 37's confirmation UI uses a Compose PIN pad, not an editable text field.
        // Click its real system keys so this exercises Android credential verification.
        pin!!.forEach { digit ->
            val keyDeadline = SystemClock.elapsedRealtime() + 10_000
            var key: android.view.accessibility.AccessibilityNodeInfo? = null
            var currentPackage: CharSequence? = null
            while (key == null && SystemClock.elapsedRealtime() < keyDeadline) {
                val root = instrumentation.uiAutomation.rootInActiveWindow
                currentPackage = root?.packageName
                key = findNode(root) { it.text?.toString() == digit.toString() }?.parent
                if (key == null) SystemClock.sleep(100)
            }
            checkNotNull(key) { "System PIN digit key was not found; window=$currentPackage" }
            assertTrue("System PIN key must be clickable", key.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        }
        val enter = checkNotNull(instrumentation.uiAutomation.rootInActiveWindow)
            .let { findNode(it) { node -> node.viewIdResourceName == "com.android.systemui:id/key_enter" } }
        assertNotNull("System PIN confirmation key must be present", enter)
        assertTrue("System PIN confirmation must be clickable", enter!!.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        ui.waitUntil(15_000) { !app.appLock.state.value.enabled }
        assertEquals(12345L, runBlocking { app.container.financeRepository.observeTransactions(null).first().single().money.minorUnits })
    }
}
