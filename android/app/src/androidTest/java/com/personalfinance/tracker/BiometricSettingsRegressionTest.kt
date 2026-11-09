package com.personalfinance.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.security.AppLockController
import com.personalfinance.tracker.security.AppLockStore
import com.personalfinance.tracker.security.LockResult
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiometricSettingsRegressionTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
    private var scenario: ActivityScenario<MainActivity>? = null
    private var prepared = false
    @Before fun prepare() = runBlocking {
        // This fixture resets protection and records; skip physical devices before any mutation.
        Assume.assumeTrue("Requires a disposable emulator", android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        prepared = true
        app.appLock.recoverAfterDeviceCredential(app.appLock.generation)
        app.container.database.clearAllTables()
        app.container.financeRepository.saveProfile(UserProfile("Biometric Demo", "en", "USD", true))
    }
    @After fun cleanup() {
        if (!prepared) return
        scenario?.close()
        runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
        app.container.database.clearAllTables()
    }
    private fun openSettings(enabled: Boolean) {
        assertEquals(LockResult.SUCCESS, runBlocking { app.appLock.enable("Ab12", enabled, true) })
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario!!.onActivity {
            Assume.assumeTrue("Requires disposable emulator with PIN and no enrolled biometrics", it.isDeviceSecure() && !it.canUseBiometrics())
        }
        ui.onNode(hasText(app.getString(R.string.navigation_more)) and hasClickAction()).performClick()
        ui.onNodeWithTag("open_SECURITY").performScrollTo().performClick()
    }
    private fun saveAndReload(expected: Boolean) {
        ui.onNodeWithTag("current_code").performScrollTo().performTextInput("Ab12")
        ui.onNodeWithTag("save_security").performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText(app.getString(R.string.security_saved)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        val fresh = AppLockController(AppLockStore(app))
        assertTrue(fresh.state.value.enabled)
        assertTrue(fresh.state.value.locked)
        assertEquals(expected, fresh.state.value.biometrics)
        assertEquals(LockResult.SUCCESS, runBlocking { fresh.unlock("Ab12") })
    }
    @Test fun savingWhileUnavailablePreservesEnabledPreference() {
        openSettings(true)
        saveAndReload(true)
    }
    @Test fun savingWhileUnavailablePreservesDisabledPreference() {
        openSettings(false)
        saveAndReload(false)
    }
    @Test fun userCanExplicitlyDisableUnavailableBiometrics() {
        openSettings(true)
        ui.onNode(isToggleable()).assertIsOn().assertIsEnabled().performScrollTo().performClick()
        ui.onNode(isToggleable()).assertIsOff()
        saveAndReload(false)
    }
}
