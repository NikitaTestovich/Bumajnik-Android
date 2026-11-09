package com.personalfinance.tracker

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.security.BiometricEntryPolicy
import com.personalfinance.tracker.security.LockResult
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Run with scripts/test-biometric-prompt.ps1 on an unlocked emulator with finger 1 enrolled. */
@RunWith(AndroidJUnit4::class)
class BiometricPromptAcceptanceTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as FinanceApplication
    private val policy get() = app.appLock.biometricEntry
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun prepare() {
        Assume.assumeTrue("Requires the external emulator finger driver",
            InstrumentationRegistry.getArguments().getString("emulatorFingerDriver") == "true")
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) { "Disposable emulator required" }
        runBlocking {
            app.appLock.recoverAfterDeviceCredential(app.appLock.generation)
            app.container.financeRepository.saveProfile(UserProfile("Biometric Demo", "en", "USD", true))
            assertEquals(LockResult.SUCCESS, app.appLock.enable("Ab12", true, true))
        }
        app.appLock.lockNow()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario!!.onActivity { assertTrue("Enroll emulator finger 1 first", it.canUseBiometrics()) }
        awaitPrompt()
        ui.onNodeWithTag("unlock_code").assertDoesNotExist()
    }

    @After fun cleanup() {
        scenario?.close()
        if (scenario != null) {
            await { policy.platformRequest == null }
            runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
            app.container.database.clearAllTables()
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        fail("Timed out: phase=${policy.phase.value}, request=${policy.platformRequest}, " +
            "window=${instrumentation.uiAutomation.rootInActiveWindow?.packageName}, locked=${app.appLock.state.value.locked}")
    }

    private fun awaitPrompt() = await {
        policy.phase.value == BiometricEntryPolicy.Phase.PROMPT_ACTIVE &&
            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == "com.android.systemui"
    }

    private fun awaitCode() {
        await { policy.phase.value == BiometricEntryPolicy.Phase.CODE && policy.platformRequest == null }
        ui.waitUntil(10_000) {
            ui.onAllNodesWithTag("unlock_code").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        assertTrue(app.appLock.state.value.locked)
        ui.onNodeWithTag("unlock_code").assertIsDisplayed()
    }

    private fun finger(id: Int) {
        // SystemUI can become visible before the emulator HAL starts accepting touches.
        SystemClock.sleep(700)
        // Host driver translates this status into adb emu finger touch; never calls app callbacks.
        instrumentation.sendStatus(2, Bundle().apply { putString("stream", "BIOMETRIC_FINGER:$id\n") })
    }

    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }

    @Test fun realSuccessAfterRotationRetainsAttemptAndUnlocks() {
        val request = policy.platformRequest
        lateinit var previous: MainActivity
        scenario!!.onActivity { previous = it; it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        await { previous.isDestroyed }
        awaitPrompt()
        assertEquals("Rotation must retain the same framework request", request, policy.platformRequest)
        ui.onNodeWithTag("unlock_code").assertDoesNotExist()
        finger(1)
        await { !app.appLock.state.value.locked }
        assertNull(policy.platformRequest)
    }

    @Test fun realNonmatchFallsBackAndExplicitRetrySucceeds() {
        val first = policy.platformRequest
        finger(2)
        awaitCode()
        scenario!!.recreate()
        awaitCode()
        ui.onNodeWithTag("biometric_retry_button").performClick()
        awaitPrompt()
        assertNotEquals(first, policy.platformRequest)
        finger(1)
        await { !app.appLock.state.value.locked }
    }

    @Test fun realNegativeButtonFallsBackToWorkingCode() {
        val root = instrumentation.uiAutomation.rootInActiveWindow
        val buttons = root.findAccessibilityNodeInfosByText(app.getString(R.string.security_use_code))
        assertTrue("System prompt must offer app code", buttons.isNotEmpty())
        assertTrue(buttons.first().performAction(AccessibilityNodeInfo.ACTION_CLICK))
        awaitCode()
        ui.onNodeWithTag("unlock_code").performTextInput("Ab12")
        ui.onNodeWithTag("unlock_button").performClick()
        await { !app.appLock.state.value.locked }
    }

    @Test fun realBackCancellationDoesNotAutomaticallyReprompt() {
        shell("input keyevent 4")
        awaitCode()
        scenario!!.recreate()
        awaitCode()
    }

    @Test fun backgroundCancelsPromptAndReturnRequiresExplicitRetry() {
        shell("input keyevent 3")
        await { policy.activeAttempt == null && policy.platformRequest == null }
        assertTrue(app.appLock.state.value.locked)
        shell("am start -f 0x20000000 -n com.personalfinance.tracker/.MainActivity")
        awaitCode()
        ui.onNodeWithTag("biometric_retry_button").performClick()
        awaitPrompt()
        finger(1)
        await { !app.appLock.state.value.locked }
    }

    @Test fun screenOffInvalidatesPromptBeforeDeviceUnlock() {
        val pin = InstrumentationRegistry.getArguments().getString("devicePin")!!
        require(pin.matches(Regex("[0-9]{4,10}")))
        val generation = app.appLock.generation
        val oldRequest = policy.platformRequest
        shell("input keyevent 223")
        await { app.appLock.generation > generation && policy.activeAttempt == null }
        assertTrue(app.appLock.state.value.locked)
        shell("input keyevent 224")
        val power = app.getSystemService(android.os.PowerManager::class.java)
        val keyguard = app.getSystemService(android.app.KeyguardManager::class.java)
        await { power.isInteractive }
        SystemClock.sleep(500) // Allow SystemUI to finish the wake animation before entering the PIN.
        shell("wm dismiss-keyguard")
        shell("input swipe 540 1900 540 500 300")
        shell("input text $pin")
        shell("input keyevent 66")
        await { !keyguard.isDeviceLocked && !keyguard.isKeyguardLocked }
        shell("am start -f 0x20000000 -n com.personalfinance.tracker/.MainActivity")
        // Screen-off creates a new lock generation. It may offer a fresh automatic prompt
        // after the previous request drains; it must never retain the old authorization.
        await { policy.phase.value != BiometricEntryPolicy.Phase.PROMPT_PENDING }
        assertTrue(app.appLock.state.value.locked)
        assertNotEquals(oldRequest, policy.platformRequest)
        if (policy.phase.value == BiometricEntryPolicy.Phase.CODE) {
            awaitCode()
            ui.onNodeWithTag("biometric_retry_button").performClick()
        }
        awaitPrompt()
        finger(1)
        await { !app.appLock.state.value.locked }
    }
}
