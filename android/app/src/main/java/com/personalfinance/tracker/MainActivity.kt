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
import androidx.lifecycle.lifecycleScope
import com.personalfinance.tracker.navigation.AppNavigation
import com.personalfinance.tracker.security.AppLockController
import com.personalfinance.tracker.security.LockResult
import com.personalfinance.tracker.ui.security.AppLockScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    val appLock: AppLockController get() = (application as FinanceApplication).appLock
    private val handler = Handler(Looper.getMainLooper())
    private val lockAfterTimeout = Runnable { if (!appLock.state.value.locked) appLock.lockNow() }
    private var biometricGeneration: Long? = null
    private var recoveryGeneration: Long? = null
    private lateinit var biometricPrompt: BiometricPrompt
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                biometricGeneration = null
                appLock.lockNow()
                biometricPrompt.cancelAuthentication()
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
            biometricGeneration = savedInstanceState.getLong("biometricGeneration", -1).takeIf { it == appLock.generation }
            recoveryGeneration = savedInstanceState.getLong("recoveryGeneration", -1).takeIf { it == appLock.generation }
        }
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val epoch = biometricGeneration ?: return
                    biometricGeneration = null
                    lifecycleScope.launch { appLock.biometricSucceeded(epoch) }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricGeneration = null
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                        Toast.makeText(this@MainActivity, errString, Toast.LENGTH_LONG).show()
                }
            })
        ContextCompat.registerReceiver(this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        lifecycleScope.launch {
            appLock.state.collect { state ->
                if (state.enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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

    private fun authenticateBiometric() {
        if (!appLock.state.value.biometrics || !canUseBiometrics()) {
            Toast.makeText(this, R.string.security_biometrics_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        biometricGeneration = appLock.generation
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
        recoveryGeneration = appLock.generation
        credentialLauncher.launch(intent)
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(lockAfterTimeout)
        appLock.foreground()
        if (appLock.state.value.enabled && getSystemService(KeyguardManager::class.java).isDeviceLocked) appLock.lockNow()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("authSession", appLock.instanceId)
        outState.putLong("biometricGeneration", biometricGeneration ?: -1)
        outState.putLong("recoveryGeneration", recoveryGeneration ?: -1)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            appLock.background()
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
