package com.personalfinance.tracker.ui.home

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import com.personalfinance.tracker.ui.theme.LocalAppTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.personalfinance.tracker.ui.theme.FinanceButton as Button
import com.personalfinance.tracker.ui.theme.FinanceCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.personalfinance.tracker.ui.theme.FinanceOutlinedButton as OutlinedButton
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
import com.personalfinance.tracker.ui.theme.financeColors
import java.math.BigDecimal
import kotlinx.coroutines.launch
import com.personalfinance.tracker.domain.goal.GoalProgress

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAllOperations: () -> Unit,
    onOpenGoal: () -> Unit,
) {
    val theme = LocalAppTheme.current
    val paired = theme.id in listOf(6,8) && LocalDensity.current.fontScale < 1.3f && LocalConfiguration.current.screenWidthDp >= 360
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
    DisposableEffect(layoutStore) { onDispose { layoutStore.close() } }
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

    val visibleLayout = layout.filter { it.visible }
    val pairedBlocks = if (paired) visibleLayout.map { it.id }.windowed(2)
        .filter { it.toSet() == setOf(HomeBlock.GOAL, HomeBlock.LATEST) }
        .flatten().toSet() else emptySet()
    val financialVisible = layout.any { it.visible && it.id in setOf(HomeBlock.SUMMARY, HomeBlock.EXPENSES) }
    val summaryVisible = layout.any { it.visible && it.id == HomeBlock.SUMMARY }
    LaunchedEffect(profile, transactions, categories, goal, savedRates, range, resumeVersion, financialVisible) {
        val currentProfile = profile ?: return@LaunchedEffect
        state = viewModel.load(currentProfile.baseCurrency, currentProfile.languageTag)
        showRateDialog = financialVisible && ((state?.missingCurrencies?.isNotEmpty() == true) || (summaryVisible && state?.allTimeMissingCurrencies?.isNotEmpty() == true))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(onClick = { customizing = true }, modifier = Modifier.fillMaxWidth().testTag("home_customize")) { Text(stringResource(R.string.home_customize)) }
            if (layout.none { it.visible }) Text(stringResource(R.string.home_all_hidden))
            val current = state
            val summary = current?.summary
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            visibleLayout.forEach { setting ->
            Column(Modifier.fillMaxWidth(if (setting.id in pairedBlocks) .48f else 1f).testTag("home_block_${setting.id.key}")) {
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
                    shape = if(theme.id in listOf(2,9)) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
                    border = if(theme.id == 6) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                    colors = CardDefaults.cardColors(containerColor = when(theme.id) { 8,10,11 -> MaterialTheme.colorScheme.primary; 2,9 -> MaterialTheme.colorScheme.background; else -> MaterialTheme.colorScheme.surface }, contentColor = if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.onSurface),
                ) {
                    Column(modifier = Modifier.padding(horizontal = if(theme.id in listOf(2,9)) 0.dp else 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(if(theme.id == 2) 12.dp else 20.dp)) {
                        Text(
                            stringResource(R.string.home_balance),
                            style = MaterialTheme.typography.titleMedium,
                            color = if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            formatSignedBalance(summary.balance.minorUnits, summary.balance.currencyCode),
                            modifier = Modifier.testTag("home_month_balance"),
                            style = MaterialTheme.typography.displaySmall,
                            color = when {
                                summary.balance.minorUnits > 0 -> if(theme.id in listOf(8,10,11)) Color(0xFFD4F9BA) else financeColors.income
                                summary.balance.minorUnits < 0 -> if(theme.id in listOf(8,10,11)) Color(0xFFFFDAD6) else financeColors.expense
                                else -> if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if(theme.id in listOf(8,10,11)) MaterialTheme.colorScheme.primary else if(theme.id == 2) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    "${stringResource(R.string.home_income)}: ${formatAmount(summary.income.minorUnits, summary.income.currencyCode)}",
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            Surface(
                                color = if(theme.id in listOf(8,10,11)) MaterialTheme.colorScheme.primary else if(theme.id == 2) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    "${stringResource(R.string.home_expenses)}: ${formatAmount(summary.expense.minorUnits, summary.expense.currencyCode)}",
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                        Text(stringResource(R.string.home_total_balance), style = MaterialTheme.typography.titleMedium,
                            color = if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.primary)
                        val total = current.allTimeBalance
                        if (total == null) {
                            Text(stringResource(R.string.home_total_needs_rates), color = if(theme.id in listOf(8,10,11)) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = { showRateDialog = true }, colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.primary)) { Text(stringResource(R.string.exchange_rate_open)) }
                        } else {
                            Text(formatSignedBalance(total.minorUnits, total.currencyCode), modifier = Modifier.testTag("home_total_balance"), style = MaterialTheme.typography.displaySmall,
                                color = when {
                                    total.minorUnits > 0 -> if(theme.id in listOf(8,10,11)) Color(0xFFD4F9BA) else financeColors.income
                                    total.minorUnits < 0 -> if(theme.id in listOf(8,10,11)) Color(0xFFFFDAD6) else financeColors.expense
                                    else -> if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.onSurface
                                })
                            if (current.allTimeUsesConversion) Text(stringResource(R.string.home_total_rate_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = if(theme.id in listOf(8,10,11)) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            }
            HomeBlock.QUICK_ACTIONS -> {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddExpense, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 64.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if(theme.id == 8) financeColors.latest else MaterialTheme.colorScheme.primary, contentColor = if(theme.id == 8) financeColors.latestInk else MaterialTheme.colorScheme.onPrimary)) {
                    Text(stringResource(R.string.home_add_expense))
                }
                Button(onClick = onAddIncome, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 64.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if(theme.id == 8) financeColors.latest else MaterialTheme.colorScheme.primary, contentColor = if(theme.id == 8) financeColors.latestInk else MaterialTheme.colorScheme.onPrimary)) {
                    Text(stringResource(R.string.home_add_income))
                }
            }

            }
            HomeBlock.GOAL -> {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = financeColors.goal, contentColor = financeColors.goalInk)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_goal), style = MaterialTheme.typography.titleMedium)
                    val currentGoal = current?.goal
                    if (currentGoal == null) {
                        Text(stringResource(R.string.home_no_goal))
                    } else {
                        val goalProgress = GoalProgress.calculate(currentGoal.target.minorUnits, currentGoal.saved.minorUnits)
                        val progress = goalProgress.segmentFractions.sum() / 4f
                        val progressLabel = stringResource(R.string.home_goal_progress, goalProgress.percent)
                        Text(
                            stringResource(
                                R.string.goal_progress,
                                formatAmount(currentGoal.saved.minorUnits, currentGoal.saved.currencyCode),
                                formatAmount(currentGoal.target.minorUnits, currentGoal.target.currencyCode),
                            ),
                        )
                        Text(progressLabel, style = MaterialTheme.typography.labelLarge, color = financeColors.goalInk)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = progressLabel },
                            color = financeColors.goalInk,
                            trackColor = financeColors.goalInk.copy(alpha = 0.12f),
                        )
                    }
                    OutlinedButton(onClick = onOpenGoal, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = financeColors.goalInk)) {
                        Text(stringResource(R.string.goal_open))
                    }
                }
            }

            }
            HomeBlock.LATEST -> {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = financeColors.latest, contentColor = financeColors.latestInk)) {
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
            }
            }
            OutlinedButton(onClick = onAllOperations, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_all_operations))
            }
        }
    }

    if (customizing) HomeCustomizationSheet(layout, layoutStore::save, layoutStore::reset, { customizing = false })

    val missingCurrencies = state?.missingCurrencies.orEmpty() + if (summaryVisible) state?.allTimeMissingCurrencies.orEmpty() else emptySet()
    if (showRateDialog && missingCurrencies.isNotEmpty()) {
        ExchangeRatePromptDialog(
            baseCurrency = baseCurrency,
            sourceCurrencies = missingCurrencies,
            onSave = { entered ->
                scope.launch {
                    viewModel.saveRates(baseCurrency, entered)
                    state = viewModel.load(baseCurrency, profile?.languageTag ?: "en")
                    showRateDialog = (state?.missingCurrencies?.isNotEmpty() == true) || (summaryVisible && state?.allTimeMissingCurrencies?.isNotEmpty() == true)
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
