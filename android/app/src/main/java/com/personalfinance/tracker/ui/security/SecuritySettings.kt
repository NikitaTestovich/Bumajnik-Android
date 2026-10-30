package com.personalfinance.tracker.ui.security

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.personalfinance.tracker.ui.theme.FinanceOutlinedButton as OutlinedButton
import com.personalfinance.tracker.ui.theme.FinanceButton as Button
import com.personalfinance.tracker.ui.theme.FinanceCard as Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.MainActivity
import com.personalfinance.tracker.R
import com.personalfinance.tracker.security.CodeVerifier
import com.personalfinance.tracker.security.LockResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecuritySettings() {
    val activity = LocalContext.current.findMainActivity() ?: return
    val controller = activity.appLock
    val state by controller.state.collectAsState()
    var currentCode by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var useBiometrics by remember(state.biometrics) { mutableStateOf(state.biometrics) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val deviceSecure = activity.isDeviceSecure()
    val biometricAvailable = activity.canUseBiometrics()
    LaunchedEffect(controller) { while (true) { controller.refresh(); delay(500) } }

    fun submit(disable: Boolean = false) {
        if (!disable && (!state.enabled || newCode.isNotEmpty())) {
            if (!CodeVerifier.isValid(newCode)) { status = R.string.security_code_length; return }
            if (newCode != confirmation) { status = R.string.security_codes_differ; return }
        }
        busy = true; status = null
        scope.launch {
            val result = when {
                disable -> controller.disable(currentCode)
                !state.enabled -> controller.enable(newCode, useBiometrics, activity.isDeviceSecure())
                else -> controller.update(currentCode, newCode.takeIf { it.isNotEmpty() }, useBiometrics)
            }
            status = result.messageResource() ?: if (disable) R.string.security_disabled_done else R.string.security_saved
            currentCode = ""
            if (result == LockResult.SUCCESS) { newCode = ""; confirmation = "" }
            busy = false
        }
    }

    Card(Modifier.fillMaxWidth().testTag("security_settings")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.security_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (state.enabled) R.string.security_enabled_description else R.string.security_disabled_description))
            if (!deviceSecure) Text(stringResource(R.string.security_need_screen_lock), color = MaterialTheme.colorScheme.error)
            if (state.enabled) CodeField(currentCode, { currentCode = it; status = null }, R.string.security_current_code, "current_code", !busy)
            CodeField(newCode, { newCode = it; status = null }, R.string.security_new_code, "new_code", !busy)
            Text(stringResource(if (state.enabled) R.string.security_keep_code_hint else R.string.security_code_length), style = MaterialTheme.typography.bodySmall)
            CodeField(confirmation, { confirmation = it; status = null }, R.string.security_confirm_code, "confirm_code", !busy)
            if (biometricAvailable || state.biometrics) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.security_biometrics), Modifier.weight(1f))
                    Switch(useBiometrics, { useBiometrics = it }, enabled = !busy && (biometricAvailable || useBiometrics))
                    // Sensor availability controls prompting, never the persisted choice.
                }
            } else Text(stringResource(R.string.security_biometrics_unavailable), style = MaterialTheme.typography.bodySmall)
            if (state.retrySeconds > 0) Text(stringResource(R.string.security_wait, state.retrySeconds))
            status?.let { Text(stringResource(it)) }
            Button(onClick = { submit() }, enabled = !busy && deviceSecure && state.retrySeconds == 0,
                modifier = Modifier.fillMaxWidth().testTag("save_security")) {
                Text(stringResource(if (state.enabled) R.string.security_save else R.string.security_enable))
            }
            if (state.enabled) {
                OutlinedButton(onClick = { submit(disable = true) }, enabled = !busy && state.retrySeconds == 0,
                    modifier = Modifier.fillMaxWidth().testTag("disable_security")) {
                    Text(stringResource(R.string.security_disable))
                }
                TextButton(onClick = activity::recoverAppCode, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.security_forgot))
                }
            }
        }
    }
}

private tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
