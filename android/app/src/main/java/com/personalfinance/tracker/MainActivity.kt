package com.personalfinance.tracker

import android.os.Bundle
import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.personalfinance.tracker.navigation.AppNavigation
import com.personalfinance.tracker.security.AppLockController
import com.personalfinance.tracker.security.LockResult
import com.personalfinance.tracker.security.BiometricEntryPolicy
import com.personalfinance.tracker.ui.security.AppLockScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    val appLock: AppLockController get() = (application as FinanceApplication).appLock
    private val handler = Handler(Looper.getMainLooper())
    private val lockAfterTimeout = Runnable { if (!appLock.state.value.locked) appLock.lockNow() }
    private var recoveryGeneration: Long? = null
    private lateinit var biometricPrompt: BiometricPrompt
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                appLock.lockNow()
                cancelOutstandingPrompt()
            }
        }
    }
    private val credentialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val epoch = recoveryGeneration
        recoveryGeneration = null
        if (result.resultCode == Activity.RESULT_OK && epoch != null) lifecycleScope.launch {
            val outcome = appLock.recoverAfterDeviceCredential(epoch)
            Toast.makeText(this@MainActivity,
                if (outcome == LockResult.SUCCESS) R.string.security_recovered else R.string.security_unavailable,
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState?.getString("authSession") == appLock.instanceId) {
            recoveryGeneration = savedInstanceState.getLong("recoveryGeneration", -1).takeIf { it == appLock.generation }
        }
        // Set the presentation phase before Compose can draw the first locked frame.
        val initialLock = appLock.state.value
        if (initialLock.locked) appLock.biometricEntry.beginSession(
            appLock.generation, initialLock.biometrics, canUseBiometrics())
        biometricPrompt = createBiometricPrompt(appLock.biometricEntry.platformRequest)
        ContextCompat.registerReceiver(this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        lifecycleScope.launch {
            appLock.state.collect { state ->
                if (state.enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (state.locked) {
                    appLock.biometricEntry.beginSession(appLock.generation, state.biometrics, canUseBiometrics())
                    maybeStartPendingPrompt()
                }
            }
        }
        setContent {
            val lock by appLock.state.collectAsState()
            if (lock.locked) FinanceTheme {
                AppLockScreen(appLock, ::authenticateBiometric, ::recoverAppCode, ::finish)
            } else AppNavigation()
        }
    }

    fun isDeviceSecure(): Boolean = getSystemService(KeyguardManager::class.java).isDeviceSecure
    fun canUseBiometrics(): Boolean = BiometricManager.from(this)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    // A replacement prompt is registered only after the previous framework request is terminal.
    // Configuration recreation reattaches to that still-outstanding request instead.
    private fun createBiometricPrompt(request: BiometricEntryPolicy.PlatformRequest?) =
        BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (request == null) return
                    lifecycleScope.launch {
                        appLock.biometricSucceeded(request.generation, request.attempt)
                        appLock.biometricEntry.terminal(request.attempt)
                        maybeStartPendingPrompt()
                    }
                }
                override fun onAuthenticationFailed() {
                    if (request == null || appLock.biometricEntry.activeAttempt != request.attempt) return
                    // Invalidate before cancelAuthentication can dispatch another callback.
                    appLock.biometricEntry.fallback(request.attempt)
                    biometricPrompt.cancelAuthentication()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (request == null) return
                    appLock.biometricEntry.fallback(request.attempt)
                    appLock.biometricEntry.terminal(request.attempt)
                    maybeStartPendingPrompt()
                }
            })

    private fun cancelOutstandingPrompt() {
        if (appLock.biometricEntry.platformRequest != null) biometricPrompt.cancelAuthentication()
    }

    private fun maybeStartPendingPrompt() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
            getSystemService(KeyguardManager::class.java).isDeviceLocked) return
        if (appLock.state.value.locked && appLock.biometricEntry.phase.value == BiometricEntryPolicy.Phase.PROMPT_PENDING)
            authenticateBiometric()
    }

    private fun authenticateBiometric() {
        if (!appLock.state.value.locked) return
        val available = appLock.state.value.biometrics && canUseBiometrics()
        appLock.biometricEntry.beginSession(appLock.generation, appLock.state.value.biometrics, available)
        if (!available) return
        val attempt = appLock.biometricEntry.beginAttempt() ?: return
        if (!canUseBiometrics()) {
            appLock.biometricEntry.fallback(attempt)
            appLock.biometricEntry.terminal(attempt)
            return
        }
        biometricPrompt = createBiometricPrompt(appLock.biometricEntry.platformRequest)
        biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.security_locked_title))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText(getString(R.string.security_use_code)).build())
    }
    @Suppress("DEPRECATION")
    fun recoverAppCode() {
        val intent = getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent(
            getString(R.string.security_recovery_title), getString(R.string.security_recovery_description))
        if (intent == null) {
            Toast.makeText(this, R.string.security_need_screen_lock, Toast.LENGTH_LONG).show()
            return
        }
        // Cover financial content before handing control to the system credential activity.
        appLock.lockNow()
        cancelOutstandingPrompt()
        recoveryGeneration = appLock.generation
        credentialLauncher.launch(intent)
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(lockAfterTimeout)
        appLock.foreground()
        if (appLock.state.value.enabled && getSystemService(KeyguardManager::class.java).isDeviceLocked) appLock.lockNow()
        maybeStartPendingPrompt()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("authSession", appLock.instanceId)
        outState.putLong("recoveryGeneration", recoveryGeneration ?: -1)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        // ON_START is dispatched after onStart returns. The first locked-state emission can
        // also arrive earlier, so retry once the Lifecycle is actually STARTED.
        maybeStartPendingPrompt()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Unlocking keyguard can restore focus without another onResume callback.
        if (hasFocus) maybeStartPendingPrompt()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            appLock.background()
            cancelOutstandingPrompt()
            if (appLock.state.value.enabled) handler.postDelayed(lockAfterTimeout, 30_000)
        }
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacks(lockAfterTimeout)
        unregisterReceiver(screenOffReceiver)
        if (isFinishing) appLock.lockNow()
        super.onDestroy()
    }
}
