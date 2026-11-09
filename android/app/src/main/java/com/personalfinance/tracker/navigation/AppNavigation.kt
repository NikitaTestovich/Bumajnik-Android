package com.personalfinance.tracker.navigation

import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.R
import com.personalfinance.tracker.data.csv.PersonalBackupManager
import com.personalfinance.tracker.data.csv.CreateCsvDocument
import com.personalfinance.tracker.data.csv.OpenCsvDocument
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.ui.operations.OperationsScreen
import com.personalfinance.tracker.ui.operations.TransactionFormScreen
import com.personalfinance.tracker.ui.home.HomeScreen
import com.personalfinance.tracker.ui.onboarding.WelcomeScreen
import com.personalfinance.tracker.ui.onboarding.WelcomeViewModel
import com.personalfinance.tracker.ui.profile.ProfileScreen
import com.personalfinance.tracker.ui.profile.ProfileSection
import com.personalfinance.tracker.ui.profile.ProfileViewModel
import com.personalfinance.tracker.ui.family.FamilyScreen
import com.personalfinance.tracker.ui.goals.GoalScreen
import com.personalfinance.tracker.ui.reports.ReportsScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import java.util.Locale
import kotlinx.coroutines.launch

private const val WelcomeRoute = "welcome"
private const val HomeRoute = "home"
private const val OperationsRoute = "operations"
private const val ReportsRoute = "reports"
private const val MoreRoute = "more"
private const val FamilyRoute = "family"
private const val GoalRoute = "goal"
private const val TransactionFormRoute = "transaction_form/{type}?transactionId={transactionId}"

private data class MainDestination(
    val route: String,
    val labelRes: Int,
)

private val mainDestinations = listOf(
    MainDestination(HomeRoute, R.string.navigation_home),
    MainDestination(OperationsRoute, R.string.navigation_operations),
    MainDestination(ReportsRoute, R.string.navigation_reports),
    MainDestination(MoreRoute, R.string.navigation_more),
)

@Composable
fun AppNavigation() {
    val themeStore = (LocalContext.current.applicationContext as FinanceApplication).themeStore
    val selectedTheme by themeStore.theme.collectAsState()
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    val repository = remember {
        (context.applicationContext as FinanceApplication).container.financeRepository
    }
    val profile by repository.observeProfile().collectAsState(initial = null)
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val welcomeViewModel = remember(repository) {
        WelcomeViewModel(repository, Locale.getDefault().toLanguageTag())
    }
    val profileViewModel = remember(repository) { ProfileViewModel(repository) }
    val backupManager = remember(repository, context.contentResolver) { PersonalBackupManager(context.contentResolver, repository) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupStatusRes by remember { mutableStateOf<Int?>(null) }
    var restoredCounts by remember { mutableStateOf(0 to 0) }
    var pendingFullBackup by remember { mutableStateOf<Pair<com.personalfinance.tracker.data.backup.FullBackupSnapshot, com.personalfinance.tracker.data.backup.BackupPreferences>?>(null) }
    var backupPasswordMode by remember { mutableStateOf<String?>(null) }
    var backupPasswordError by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var backupPasswordConfirm by remember { mutableStateOf("") }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }
    DisposableEffect(Unit) {
        onDispose { pendingExportPassword?.fill('\u0000') }
    }
    var backupInput by remember { mutableStateOf<ByteArray?>(null) }
    var legacyBackup by remember { mutableStateOf(false) }
    val fullBackupImport = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            try {
                val bytes = com.personalfinance.tracker.data.backup.readFullBackupBytes(context, uri)
                if (com.personalfinance.tracker.data.backup.EncryptedBackup.isEncrypted(bytes)) {
                    backupInput = bytes
                    backupPasswordMode = "import"
                } else {
                    pendingFullBackup = com.personalfinance.tracker.data.backup.decodeFullBackup(bytes)
                    legacyBackup = true
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { backupStatusRes = R.string.full_backup_invalid }
            finally { backupBusy = false }
        }
    }
    val fullBackupExport = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (uri == null) {
            password?.fill('\u0000')
        } else if (password == null) {
            backupStatusRes = R.string.backup_export_failed
        } else {
            scope.launch {
                backupBusy = true
                try {
                    com.personalfinance.tracker.data.backup.exportFullBackup(context, uri, password)
                    backupStatusRes = R.string.full_backup_export_done
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { backupStatusRes = R.string.backup_export_failed }
                finally { password.fill('\u0000'); backupBusy = false }
            }
        }
    }
    val backupExport = rememberLauncherForActivityResult(CreateCsvDocument()) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            try {
                val result = backupManager.export(uri)
                backupStatusRes = if (result.success) R.string.backup_export_done else if (result.message == "csv_export_limit") R.string.csv_export_limit else R.string.backup_export_failed
            } finally { backupBusy = false }
        }
    }
    val backupRestore = rememberLauncherForActivityResult(OpenCsvDocument()) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            try {
                val result = backupManager.restore(uri)
                restoredCounts = result.addedCount to result.duplicateCount
                backupStatusRes = when (result.errors.firstOrNull()) {
                    null -> R.string.backup_restore_done
                    "backup_invalid_csv" -> R.string.backup_invalid_csv
                    "backup_read_failed" -> R.string.backup_read_failed
                    else -> R.string.backup_restore_failed
                }
            } finally { backupBusy = false }
        }
    }
    val backupStatus = backupStatusRes?.let { resource ->
        if (resource == R.string.backup_restore_done) stringResource(resource, restoredCounts.first, restoredCounts.second)
        else stringResource(resource)
    }
    val backupFilename = stringResource(R.string.backup_filename)

    LaunchedEffect(profile?.languageTag) {
        val savedLanguage = profile?.languageTag ?: return@LaunchedEffect
        val activeLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language
        if (activeLanguage != savedLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
        }
    }

    LaunchedEffect(profile, currentRoute) {
        when {
            // A null route can mean the NavHost has not attached its graph yet (for example
            // during locale recreation). Wait for the welcome destination before navigating.
            profile?.onboardingComplete == true && currentRoute == WelcomeRoute -> {
                navController.navigate(HomeRoute) {
                    popUpTo(WelcomeRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
            profile?.onboardingComplete != true && currentRoute != null && currentRoute != WelcomeRoute -> {
                navController.navigate(WelcomeRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    FinanceTheme(selectedTheme) {
        Scaffold(
            bottomBar = {
                if (profile?.onboardingComplete == true && currentRoute != WelcomeRoute && currentRoute != TransactionFormRoute && currentRoute != FamilyRoute && currentRoute != GoalRoute) {
                    NavigationBar {
                        mainDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {},
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = WelcomeRoute,
                modifier = Modifier.padding(paddingValues),
            ) {
                composable(WelcomeRoute) {
                    WelcomeScreen(
                        languageTag = welcomeViewModel.initialLanguage,
                        onComplete = { name, currency ->
                            scope.launch { welcomeViewModel.completeOnboarding(name, currency) }
                        },
                    )
                }
                composable(HomeRoute) {
                    HomeScreen(
                        onAddIncome = { navController.navigate(transactionFormRoute(TransactionType.INCOME)) },
                        onAddExpense = { navController.navigate(transactionFormRoute(TransactionType.EXPENSE)) },
                        onAllOperations = { navController.navigate(OperationsRoute) },
                        onOpenGoal = { navController.navigate(GoalRoute) },
                    )
                }
                composable(OperationsRoute) {
                    OperationsScreen(
                        categories = categories,
                        languageTag = profile?.languageTag ?: "en",
                        onAdd = { type -> navController.navigate(transactionFormRoute(type)) },
                        onEdit = { transaction -> navController.navigate(transactionFormRoute(transaction.type, transaction.id)) },
                    )
                }
                composable(ReportsRoute) { ReportsScreen() }
                composable("more_updates") { com.personalfinance.tracker.update.UpdatesScreen { navController.popBackStack() } }
                composable(
                    route = TransactionFormRoute,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("transactionId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    val transactionType = entry.arguments?.getString("type")
                        ?.let { value -> runCatching { TransactionType.valueOf(value) }.getOrNull() }
                        ?: TransactionType.EXPENSE
                    TransactionFormScreen(
                        transactionId = entry.arguments?.getString("transactionId"),
                        type = transactionType,
                        onSaved = {
                            (context.applicationContext as FinanceApplication).operationsTabStore.select(transactionType)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                    )
                }
                listOf(MoreRoute to ProfileSection.MORE, "more_themes" to ProfileSection.THEMES,
                    "more_security" to ProfileSection.SECURITY, "more_backup" to ProfileSection.BACKUP,
                    "more_categories" to ProfileSection.CATEGORIES).forEach { (route, section) -> composable(route) {
                    val currentProfile = profile
                    if (currentProfile != null) {
                        ProfileScreen(
                            profile = currentProfile,
                            categories = categories,
                            selectedTheme = selectedTheme,
                            onSelectTheme = themeStore::select,
                            onSave = { name, language, currency ->
                                scope.launch { profileViewModel.updateProfile(name, language, currency) }
                            },
                            onAddCategory = { name, language, icon -> profileViewModel.addCustomCategory(name, language, icon) },
                            onEditCategory = profileViewModel::renameCategory,
                            onArchiveCategory = profileViewModel::archiveCategory,
                            onOpenFamily = { navController.navigate(FamilyRoute) },
                            onOpenGoal = { navController.navigate(GoalRoute) },
                            onExportBackup = {
                                backupStatusRes = null
                                backupExport.launch(backupFilename)
                            },
                            onRestoreBackup = {
                                backupStatusRes = null
                                backupRestore.launch(Unit)
                            },
                            backupBusy = backupBusy,
                            backupStatus = backupStatus,
                            section = section,
                            onOpenSection = { target -> navController.navigate("more_${target.name.lowercase()}") },
                            onBack = { navController.popBackStack() },
                            onExportFullBackup = { backupStatusRes = null; backupPasswordMode = "export" },
                            onRestoreFullBackup = { backupStatusRes = null; fullBackupImport.launch(arrayOf("application/octet-stream", "*/*")) },
                        )
                    } else {
                        LoadingScreen()
                    }
                } }
                composable(FamilyRoute) { FamilyScreen(onBack = { navController.popBackStack() }) }
                composable(GoalRoute) { GoalScreen(onBack = { navController.popBackStack() }) }
            }
        }
        backupPasswordMode?.let { mode ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { backupPasswordMode = null; backupPasswordError = false; backupPassword = ""; backupPasswordConfirm = ""; backupInput = null },
                title = { Text(stringResource(if (mode == "export") R.string.full_backup_password_create else R.string.full_backup_password_enter)) },
                text = {
                    Column {
                        Text(stringResource(R.string.full_backup_password_note))
                        if (backupPasswordError) Text(stringResource(R.string.full_backup_password_error), color = MaterialTheme.colorScheme.error)
                        androidx.compose.material3.OutlinedTextField(value = backupPassword, onValueChange = { backupPassword = it }, label = { Text(stringResource(R.string.full_backup_password)) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), singleLine = true)
                        if (mode == "export") androidx.compose.material3.OutlinedTextField(value = backupPasswordConfirm, onValueChange = { backupPasswordConfirm = it }, label = { Text(stringResource(R.string.full_backup_password_confirm)) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), singleLine = true)
                    }
                },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { backupPasswordMode = null; backupPasswordError = false; backupPassword = ""; backupPasswordConfirm = ""; backupInput = null }) { Text(stringResource(R.string.operation_cancel)) } },
                confirmButton = { androidx.compose.material3.TextButton(enabled = backupPassword.isNotEmpty() && (mode != "export" || backupPassword == backupPasswordConfirm), onClick = {
                    val password = backupPassword.toCharArray()
                    backupPassword = ""; backupPasswordConfirm = ""; backupPasswordMode = null
                    if (mode == "export") {
                        pendingExportPassword?.fill('\u0000')
                        pendingExportPassword = password
                        try { fullBackupExport.launch("finance-tracker-backup.ftb") }
                        catch (_: Exception) { pendingExportPassword = null; password.fill('\u0000'); backupStatusRes = R.string.backup_export_failed }
                    } else {
                      val input = backupInput
                      scope.launch {
                        backupBusy = true
                        try {
                            pendingFullBackup = com.personalfinance.tracker.data.backup.decodeFullBackup(checkNotNull(input), password)
                            legacyBackup = false
                            backupInput = null; backupPasswordError = false
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) {
                            backupPasswordError = true; backupPasswordMode = "import"
                        }
                        finally { password.fill('\u0000'); backupBusy = false }
                      }
                    }
                }) { Text(stringResource(android.R.string.ok)) } },
            )
        }
        pendingFullBackup?.let { pending ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingFullBackup = null },
                title = { Text(stringResource(R.string.full_backup_confirm_title)) },
                text = { Text((if (legacyBackup) stringResource(R.string.full_backup_legacy_warning) + "\n\n" else "") + stringResource(R.string.full_backup_confirm_body, pending.first.transactions.size, pending.first.familyTransactions.size)) },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingFullBackup = null }) { Text(stringResource(R.string.operation_cancel)) } },
                confirmButton = { androidx.compose.material3.TextButton(onClick = {
                    pendingFullBackup = null
                    scope.launch {
                        backupBusy = true
                        try {
                            val complete = com.personalfinance.tracker.data.backup.applyFullBackup(context, pending.first, pending.second)
                            backupStatusRes = if (complete) R.string.full_backup_restore_done else R.string.full_backup_settings_failed
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { backupStatusRes = R.string.backup_restore_failed }
                        finally { backupBusy = false }
                    }
                }) { Text(stringResource(R.string.full_backup_restore)) } },
            )
        }
    }
}

private fun transactionFormRoute(type: TransactionType, transactionId: String? = null): String =
    if (transactionId == null) "transaction_form/${type.name}"
    else "transaction_form/${type.name}?transactionId=$transactionId"

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlaceholderScreen(titleRes: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineMedium)
    }
}
