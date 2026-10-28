package com.personalfinance.tracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.personalfinance.tracker.data.preferences.HomeLayoutStore
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.summary.SummaryService
import com.personalfinance.tracker.ui.summary.ExchangeRatePromptDialog
import com.personalfinance.tracker.ui.theme.FinancePalette
import java.math.BigDecimal
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAllOperations: () -> Unit,
    onOpenGoal: () -> Unit,
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as FinanceApplication).container }
    val repository = container.financeRepository
    val rateRepository = container.rateRepository
    val profile by repository.observeProfile().collectAsState(initial = null)
    val transactions by repository.observeTransactions(null).collectAsState(initial = emptyList())
    val goal by repository.observeGoal().collectAsState(initial = null)
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val layoutStore = remember { HomeLayoutStore(context) }
    val layout by layoutStore.layout.collectAsState()
    var customizing by remember { mutableStateOf(false) }
    val viewModel = remember {
        HomeViewModel(
            repository = repository,
            exchangeRateService = ExchangeRateService(rateRepository),
            summaryService = SummaryService(repository),
        )
    }
    var range by remember { mutableStateOf(viewModel.currentMonthRange()) }
    var resumeVersion by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { range = viewModel.currentMonthRange(); resumeVersion++ } }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val baseCurrency = profile?.baseCurrency ?: "USD"
    val savedRates by rateRepository.ratesFor(range, baseCurrency).collectAsState(initial = emptyMap())
    var state by remember(baseCurrency) { mutableStateOf<HomeUiState?>(null) }
    var showRateDialog by remember { mutableStateOf(false) }

    val financialVisible = layout.any { it.visible && it.id in setOf(HomeBlock.SUMMARY, HomeBlock.EXPENSES) }
    LaunchedEffect(profile, transactions, categories, goal, savedRates, range, resumeVersion, financialVisible) {
        val currentProfile = profile ?: return@LaunchedEffect
        state = viewModel.load(currentProfile.baseCurrency, currentProfile.languageTag)
        showRateDialog = financialVisible && state?.missingCurrencies?.isNotEmpty() == true
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(onClick = { customizing = true }, modifier = Modifier.fillMaxWidth().testTag("home_customize")) { Text(stringResource(R.string.home_customize)) }
            if (layout.none { it.visible }) Text(stringResource(R.string.home_all_hidden))
            val current = state
            val summary = current?.summary
            layout.filter { it.visible }.forEach { setting ->
            when (setting.id) {
            HomeBlock.SUMMARY -> {
            if (current == null || summary == null) {
                Text(
                    stringResource(R.string.home_need_rates, baseCurrency),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { showRateDialog = true }) {
                    Text(stringResource(R.string.exchange_rate_open))
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            stringResource(R.string.home_balance),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            formatSignedBalance(summary.balance.minorUnits, summary.balance.currencyCode),
                            style = MaterialTheme.typography.displaySmall,
                            color = when {
                                summary.balance.minorUnits > 0 -> FinancePalette.income
                                summary.balance.minorUnits < 0 -> FinancePalette.expense
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = FinancePalette.income.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    "${stringResource(R.string.home_income)}: ${formatAmount(summary.income.minorUnits, summary.income.currencyCode)}",
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            Surface(
                                color = FinancePalette.expense.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    "${stringResource(R.string.home_expenses)}: ${formatAmount(summary.expense.minorUnits, summary.expense.currencyCode)}",
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                }
            }

            }
            HomeBlock.QUICK_ACTIONS -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddIncome, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_add_income))
                }
                OutlinedButton(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_add_expense))
                }
            }

            }
            HomeBlock.GOAL -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_goal), style = MaterialTheme.typography.titleMedium)
                    val currentGoal = current?.goal
                    if (currentGoal == null) {
                        Text(stringResource(R.string.home_no_goal))
                    } else {
                        val progress = (currentGoal.saved.minorUnits.toFloat() / currentGoal.target.minorUnits)
                            .coerceIn(0f, 1f)
                        val progressLabel = stringResource(R.string.home_goal_progress, (progress * 100).roundToInt())
                        Text(
                            stringResource(
                                R.string.goal_progress,
                                formatAmount(currentGoal.saved.minorUnits, currentGoal.saved.currencyCode),
                                formatAmount(currentGoal.target.minorUnits, currentGoal.target.currencyCode),
                            ),
                        )
                        Text(progressLabel, style = MaterialTheme.typography.labelLarge, color = FinancePalette.accent)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = progressLabel },
                            color = FinancePalette.accent,
                            trackColor = FinancePalette.accent.copy(alpha = 0.12f),
                        )
                    }
                    OutlinedButton(onClick = onOpenGoal, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.goal_open))
                    }
                }
            }

            }
            HomeBlock.LATEST -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.home_latest), style = MaterialTheme.typography.titleMedium)
                    val latest = current?.latestTransaction
                    if (latest == null) {
                        Text(stringResource(R.string.operation_empty))
                    } else {
                        val title = if (latest.type == TransactionType.INCOME) {
                            stringResource(R.string.home_income)
                        } else {
                            stringResource(R.string.home_expenses)
                        }
                        Text("$title • ${FinanceDateFormat.format(latest.date)}")
                        Text(formatAmount(latest.money.minorUnits, latest.money.currencyCode))
                        if (latest.type == TransactionType.INCOME && latest.comment.isNotBlank()) Text(latest.comment)
                    }
                }
            }

            }
            HomeBlock.EXPENSES -> MonthlyExpensesWidget(current?.expenses.orEmpty(), baseCurrency, profile?.languageTag ?: "en", current == null || current.missingCurrencies.isNotEmpty(), { showRateDialog = true })
            }
            }
            OutlinedButton(onClick = onAllOperations, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_all_operations))
            }
        }
    }

    if (customizing) HomeCustomizationSheet(layout, layoutStore::save, layoutStore::reset, { customizing = false })

    val missingCurrencies = state?.missingCurrencies.orEmpty()
    if (showRateDialog && missingCurrencies.isNotEmpty()) {
        ExchangeRatePromptDialog(
            baseCurrency = baseCurrency,
            sourceCurrencies = missingCurrencies,
            onSave = { entered ->
                scope.launch {
                    viewModel.saveRates(baseCurrency, entered)
                    state = viewModel.load(baseCurrency, profile?.languageTag ?: "en")
                    showRateDialog = state?.missingCurrencies?.isNotEmpty() == true
                }
            },
            onDismiss = { showRateDialog = false },
        )
    }
}

private fun formatAmount(minorUnits: Long, currencyCode: String): String =
    "${BigDecimal.valueOf(minorUnits, 2).setScale(2).toPlainString()} $currencyCode"

private fun formatSignedBalance(minorUnits: Long, currencyCode: String): String {
    val sign = when {
        minorUnits > 0 -> "+"
        minorUnits < 0 -> "−"
        else -> ""
    }
    val amount = BigDecimal.valueOf(minorUnits, 2).abs().setScale(2).toPlainString()
    return "$sign$amount $currencyCode"
}
