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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val scope = rememberCoroutineScope()
    val viewModel = remember {
        HomeViewModel(
            repository = repository,
            exchangeRateService = ExchangeRateService(rateRepository),
            summaryService = SummaryService(repository),
        )
    }
    val range = remember { viewModel.currentMonthRange() }
    val baseCurrency = profile?.baseCurrency ?: "USD"
    val savedRates by rateRepository.ratesFor(range, baseCurrency).collectAsState(initial = emptyMap())
    var state by remember(baseCurrency) { mutableStateOf<HomeUiState?>(null) }
    var showRateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profile, transactions, goal, savedRates) {
        val currentProfile = profile ?: return@LaunchedEffect
        state = viewModel.load(currentProfile.baseCurrency)
        if (state?.missingCurrencies?.isNotEmpty() == true) showRateDialog = true
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val current = state
            val summary = current?.summary
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddIncome, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_add_income))
                }
                OutlinedButton(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_add_expense))
                }
            }

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
                    }
                }
            }

            OutlinedButton(onClick = onAllOperations, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_all_operations))
            }
        }
    }

    val missingCurrencies = state?.missingCurrencies.orEmpty()
    if (showRateDialog && missingCurrencies.isNotEmpty()) {
        ExchangeRatePromptDialog(
            baseCurrency = baseCurrency,
            sourceCurrencies = missingCurrencies,
            onSave = { entered ->
                scope.launch {
                    viewModel.saveRates(baseCurrency, entered)
                    state = viewModel.load(baseCurrency)
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
