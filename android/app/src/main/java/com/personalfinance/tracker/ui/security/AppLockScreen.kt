package com.personalfinance.tracker.ui.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.R
import com.personalfinance.tracker.security.AppLockController
import com.personalfinance.tracker.security.CodeVerifier
import com.personalfinance.tracker.security.LockResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(controller: AppLockController, onBiometric: () -> Unit, onRecover: () -> Unit, onExit: () -> Unit) {
    val state by controller.state.collectAsState()
    val phase by controller.biometricEntry.phase.collectAsState()
    val platformReady by controller.biometricEntry.platformReady.collectAsState()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler { onExit() }
    LaunchedEffect(controller) { while (true) { controller.refresh(); delay(500) } }
    Surface(Modifier.fillMaxSize().testTag("app_lock_screen")) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.security_locked_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.security_locked_description))
            if (phase == com.personalfinance.tracker.security.BiometricEntryPolicy.Phase.CODE) {
                CodeField(code, { code = it; error = null }, R.string.security_code, "unlock_code", !busy)
                if (state.storageError) Text(stringResource(R.string.security_storage_error), color = MaterialTheme.colorScheme.error)
                if (state.retrySeconds > 0) Text(stringResource(R.string.security_wait, state.retrySeconds), color = MaterialTheme.colorScheme.error)
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            val result = controller.unlock(code)
                            code = ""; busy = false
                            error = result.messageResource()
                        }
                    },
                    enabled = !busy && !state.storageError && state.retrySeconds == 0 && CodeVerifier.isValid(code),
                    modifier = Modifier.fillMaxWidth().testTag("unlock_button"),
                ) { Text(stringResource(R.string.security_unlock)) }
            }
            if (state.biometrics && phase == com.personalfinance.tracker.security.BiometricEntryPolicy.Phase.CODE) OutlinedButton(onClick = onBiometric, enabled = !busy && platformReady, modifier = Modifier.fillMaxWidth().testTag("biometric_retry_button")) {
                Text(stringResource(R.string.security_use_biometrics))
            }
            TextButton(onClick = onRecover, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.security_forgot))
            }
        }
    }
}

@Composable
internal fun CodeField(value: String, onChange: (String) -> Unit, label: Int, tag: String, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(stringResource(label)) }, singleLine = true, enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false))
}

internal fun LockResult.messageResource(): Int? = when (this) {
    LockResult.SUCCESS -> null
    LockResult.WRONG_CODE -> R.string.security_wrong_code
    LockResult.INVALID_CODE -> R.string.security_code_length
    LockResult.WAIT -> R.string.security_too_many_attempts
    LockResult.UNAVAILABLE -> R.string.security_unavailable
}
