package com.personalfinance.tracker.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.personalfinance.tracker.ui.theme.FinanceButton as Button
import com.personalfinance.tracker.ui.theme.FinanceCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import com.personalfinance.tracker.ui.theme.FinanceOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.goal.GoalProgress
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.goal.GoalForecastStatus
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.summary.SummaryService
import com.personalfinance.tracker.ui.summary.ExchangeRatePromptDialog
import com.personalfinance.tracker.ui.theme.financeColors
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch

@Composable
fun GoalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as FinanceApplication).container }
    val repository = container.financeRepository
    val profile by repository.observeProfile().collectAsState(initial = null)
    val transactions by repository.observeTransactions(null).collectAsState(initial = emptyList())
    val goal by repository.observeGoal().collectAsState(initial = null)
    val baseCurrency = profile?.baseCurrency ?: "USD"
    val viewModel = remember(repository, container.rateRepository) {
        GoalViewModel(repository, container.rateRepository, SummaryService(repository))
    }
    val scope = rememberCoroutineScope()
    var refreshToken by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<GoalUiState?>(null) }
    var showRateDialog by remember { mutableStateOf(false) }
    var targetText by remember(goal, baseCurrency) {
        mutableStateOf(goal?.takeIf { it.target.currencyCode == baseCurrency }?.target?.toAmountText().orEmpty())
    }
    var savedText by remember(goal, baseCurrency) {
        mutableStateOf(goal?.takeIf { it.saved.currencyCode == baseCurrency }?.saved?.toAmountText().orEmpty())
    }

    LaunchedEffect(profile?.baseCurrency, transactions, goal, refreshToken) {
        if (profile != null) state = viewModel.load(baseCurrency)
    }

    val targetMinor = targetText.toMinorUnits()
    val savedMinor = savedText.toMinorUnits()
    val canSave = targetMinor != null && targetMinor > 0L && savedMinor != null && savedMinor >= 0L
    val current = state

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.goal_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(onClick = onBack) { Text(stringResource(R.string.family_back)) }
            }
            if (current?.goalCurrencyChanged == true) {
                Text(
                    stringResource(R.string.goal_currency_changed, current.goal?.target?.currencyCode.orEmpty(), baseCurrency),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goal_target, baseCurrency)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = savedText,
                onValueChange = { savedText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goal_saved, baseCurrency)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            Button(
                onClick = {
                    val target = targetMinor
                    val saved = savedMinor
                    if (target != null && saved != null) scope.launch {
                        viewModel.saveGoal(target, saved, baseCurrency)
                        refreshToken++
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.goal_save))
            }

            if (current?.goal != null && !current.goalCurrencyChanged) {
                GoalProgressCard(current.goal)
            }
            if (current?.goal == null) {
                Text(stringResource(R.string.goal_create_hint))
            } else if (current.missingCurrencies.isNotEmpty()) {
                Text(stringResource(R.string.goal_need_rates, current.missingCurrencies.sorted().joinToString(), baseCurrency))
                OutlinedButton(onClick = { showRateDialog = true }) {
                    Text(stringResource(R.string.exchange_rate_open))
                }
            } else if (current.goalCurrencyChanged) {
                Text(stringResource(R.string.goal_reenter_hint, baseCurrency))
            } else {
                val forecast = current.forecast
                if (forecast != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.goal_forecast), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.goal_months_used, current.completedMonthCount))
                            when (forecast.status) {
                                GoalForecastStatus.READY -> {
                                    if (forecast.remainingMinorUnits == 0L) {
                                        Text(
                                            stringResource(R.string.goal_reached),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = financeColors.income,
                                        )
                                    } else {
                                        Text(
                                            stringResource(R.string.goal_estimate, forecast.estimatedMonths ?: 0),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            stringResource(R.string.goal_average_surplus, formatMinor(forecast.averageMonthlySurplusMinorUnits ?: 0L), baseCurrency),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                }
                                GoalForecastStatus.NO_COMPLETED_MONTHS -> Text(stringResource(R.string.goal_no_completed_months))
                                GoalForecastStatus.NO_POSITIVE_SURPLUS -> Text(stringResource(R.string.goal_no_positive_surplus))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRateDialog && current?.forecastPeriod != null && current.missingCurrencies.isNotEmpty()) {
        ExchangeRatePromptDialog(
            baseCurrency = baseCurrency,
            sourceCurrencies = current.missingCurrencies,
            onSave = { entered ->
                val range = requireNotNull(current.forecastPeriod)
                showRateDialog = false
                scope.launch {
                    viewModel.saveRates(range, baseCurrency, entered)
                    refreshToken++
                }
            },
            onDismiss = { showRateDialog = false },
        )
    }
}

private fun Money.toAmountText(): String = BigDecimal.valueOf(minorUnits, 2).stripTrailingZeros().toPlainString()

@Composable
internal fun GoalProgressCard(goal: SavingsGoal) {
    val progress = GoalProgress.calculate(goal.target.minorUnits, goal.saved.minorUnits)
    val percentage = stringResource(R.string.goal_percent, progress.percent)
    val amounts = stringResource(R.string.goal_progress, goal.saved.toDisplayAmount(), goal.target.toDisplayAmount())
    val remaining = stringResource(R.string.goal_remaining, formatMinor(progress.remainingMinorUnits), goal.target.currencyCode)
    val next = stringResource(R.string.goal_next_milestone, progress.nextMilestonePercent,
        formatMinor(progress.nextMilestoneMinorUnits), goal.target.currencyCode)
    val status = if (progress.completed) stringResource(R.string.goal_reached) else next
    val description = listOf(percentage, amounts, remaining, status).joinToString(". ")
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = financeColors.goal, contentColor = financeColors.goalInk,
    )) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(percentage, style = MaterialTheme.typography.displaySmall)
            Text(amounts, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().height(52.dp).testTag("goal_segments").semantics {
                contentDescription = description
            }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                progress.segmentFractions.forEach { fraction ->
                    Box(Modifier.weight(1f).height(52.dp).background(financeColors.goalInk.copy(alpha = 0.12f))) {
                        if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).height(52.dp).background(financeColors.goalInk))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, 25, 50, 75, 100).forEach {
                    Text(stringResource(R.string.goal_percent, it.toString()), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(remaining, style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.titleMedium)
        }
    }
}
private fun Money.toDisplayAmount(): String = "${formatMinor(minorUnits)} $currencyCode"
private fun formatMinor(minorUnits: Long): String = BigDecimal.valueOf(minorUnits, 2).setScale(2).toPlainString()

private fun String.toMinorUnits(): Long? = runCatching {
    val amount = trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    require(amount >= BigDecimal.ZERO)
    amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
}.getOrNull()
