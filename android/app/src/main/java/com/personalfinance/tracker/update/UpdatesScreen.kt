package com.personalfinance.tracker.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.BuildConfig
import com.personalfinance.tracker.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun UpdatesScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val checker = remember { GitHubReleaseChecker() }
    val context = LocalContext.current
    val installer = remember(context) { UpdateInstaller(context.applicationContext) }
    var result by remember { mutableStateOf<ReleaseCheckResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var installError by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.family_back)) }
        Text(stringResource(R.string.updates_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.updates_current, BuildConfig.VERSION_NAME))
        UpdateAction(
            available = result is ReleaseCheckResult.Available,
            loading = loading,
            onCheck = { scope.launch { loading = true; result = checker.check(BuildConfig.VERSION_CODE.toLong()); loading = false } },
            onInstall = {
                val selected = result as? ReleaseCheckResult.Available
                if (selected != null) scope.launch {
                    loading = true
                    installError = false
                    try { context.startActivity(installer.prepare(selected.asset)) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { installError = true }
                    finally { loading = false }
                }
            },
        )
        if (loading) Text(stringResource(R.string.updates_checking))
        when (val state = result) {
            ReleaseCheckResult.NoRelease -> Text(stringResource(R.string.updates_no_release))
            ReleaseCheckResult.NoCompatibleUpdate -> Text(stringResource(R.string.updates_no_compatible))
            ReleaseCheckResult.Failed -> Text(stringResource(R.string.updates_error))
            is ReleaseCheckResult.Available -> {
                Text(stringResource(R.string.updates_available, state.versionName))
                UpdateReleaseNotes(state.releaseNotes)
            }
            null -> Unit
        }
        if (installError) Text(stringResource(R.string.updates_install_error), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
internal fun UpdateAction(available: Boolean, loading: Boolean, onCheck: () -> Unit, onInstall: () -> Unit) {
    Button(enabled = !loading, modifier = Modifier.fillMaxWidth(), onClick = if (available) onInstall else onCheck) {
        Text(stringResource(if (available) R.string.updates_install else R.string.updates_check))
    }
}

@Composable
internal fun UpdateReleaseNotes(notes: String) {
    if (notes.isNotBlank()) {
        Text(stringResource(R.string.updates_release_notes), style = MaterialTheme.typography.titleMedium)
        Text(notes)
    }
}
