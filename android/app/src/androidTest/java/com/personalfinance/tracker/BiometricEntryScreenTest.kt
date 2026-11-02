package com.personalfinance.tracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.security.AppLockController
import com.personalfinance.tracker.security.AppLockStore
import com.personalfinance.tracker.security.LockResult
import com.personalfinance.tracker.ui.security.AppLockScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiometricEntryScreenTest {
    @get:Rule val ui = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
    private lateinit var lock: AppLockController

    @Before fun prepare() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val emulated = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("getprop ro.kernel.qemu"))
            .use { it.readBytes().toString(Charsets.UTF_8).trim() }
        Assume.assumeTrue("This suite resets security and requires a disposable emulator", isDisposableEmulator(emulated, android.os.Build.HARDWARE))
        runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
        // A screen fixture owns its request lifecycle; do not leak an unterminated fake
        // framework request into the next test through the Application singleton.
        lock = AppLockController(AppLockStore(app))
    }

    @After fun cleanup() {
        if (::lock.isInitialized) runBlocking { lock.recoverAfterDeviceCredential(lock.generation) }
    }

    private fun launchLock(biometrics: Boolean, onRetry: () -> Unit = {}) : AppLockController {
        assertEquals(LockResult.SUCCESS, runBlocking { lock.enable("Ab12", biometrics, true) })
        lock.lockNow()
        lock.biometricEntry.beginSession(lock.generation, biometrics, true)
        ui.setContent { FinanceTheme { AppLockScreen(lock, onRetry, {}, {}) } }
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("app_lock_screen").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        return lock
    }

    @Test fun biometricPromptHidesCodeUntilFallbackAndRetryIsExplicit() {
        var retries = 0
        val lock = launchLock(true) { retries++ }
        val first = lock.biometricEntry.beginAttempt()!!
        ui.onNodeWithTag("unlock_code").assertDoesNotExist()
        ui.onNodeWithTag("unlock_button").assertDoesNotExist()

        ui.runOnIdle { lock.biometricEntry.fallback(first) }
        ui.onNodeWithTag("unlock_code").assertIsDisplayed()
        ui.onNodeWithTag("biometric_retry_button").assertIsNotEnabled()
        ui.runOnIdle { lock.biometricEntry.terminal(first) }
        ui.onNodeWithTag("biometric_retry_button").performClick()
        ui.runOnIdle { assertTrue(retries == 1) }
    }

    @Test fun codeOnlyModeUnlocksWithCorrectCode() {
        val lock = launchLock(false)
        ui.onNodeWithTag("biometric_retry_button").assertDoesNotExist()
        ui.onNodeWithTag("unlock_code").performTextInput("Ab12")
        ui.onNodeWithTag("unlock_button").performClick()
        ui.waitUntil(10_000) { !lock.state.value.locked }
        assertFalse(lock.state.value.locked)
    }

    @Test fun newGenerationWithUnconfirmedCancellationShowsCode() {
        val lock = launchLock(true)
        val old = lock.biometricEntry.beginAttempt()!!
        ui.runOnIdle {
            lock.lockNow()
            lock.biometricEntry.beginSession(lock.generation, true, true)
        }
        ui.onNodeWithTag("unlock_code").assertIsDisplayed()
        ui.onNodeWithTag("biometric_retry_button").assertIsNotEnabled()
        assertEquals(LockResult.UNAVAILABLE, runBlocking { lock.biometricSucceeded(lock.generation, old) })
    }
}
