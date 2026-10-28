package com.personalfinance.tracker.ui.operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.summary.PeriodSummary
import com.personalfinance.tracker.domain.summary.SummaryService
import com.personalfinance.tracker.ui.summary.ExchangeRatePromptDialog
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun OperationsScreen(
    categories: List<Category>,
    languageTag: String,
    onAdd: (TransactionType) -> Unit,
    onEdit: (FinanceTransaction) -> Unit,
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as FinanceApplication).container }
    val repository = container.financeRepository
    val rateRepository = container.rateRepository
    val viewModel = remember(repository) { OperationsViewModel(repository) }
    val selectedType by viewModel.selectedType.collectAsState()
    val pendingDeleteId by viewModel.pendingDeleteIdFlow.collectAsState()
    val profile by repository.observeProfile().collectAsState(initial = null)
    val transactions by remember(selectedType) { viewModel.transactions(selectedType) }
        .collectAsState(initial = emptyList())
    val allTransactions by repository.observeTransactions(null).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val monthStart = remember { today.withDayOfMonth(1) }
    val period = remember(monthStart) { com.personalfinance.tracker.domain.model.PeriodRange(monthStart, monthStart.plusMonths(1)) }
    val baseCurrency = profile?.baseCurrency ?: "USD"
    val savedRates by rateRepository.ratesFor(period, baseCurrency).collectAsState(initial = emptyMap())
    val exchangeRateService = remember(rateRepository) { ExchangeRateService(rateRepository) }
    val summaryService = remember(repository) { SummaryService(repository) }
    var summary by remember(baseCurrency) { mutableStateOf<PeriodSummary?>(null) }
    var missingCurrencies by remember(baseCurrency) { mutableStateOf<Set<String>>(emptySet()) }
    var showRateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profile, allTransactions, savedRates) {
        val currentProfile = profile ?: return@LaunchedEffect
        val currencies = allTransactions.asSequence()
            .filter { it.date >= period.start && it.date < period.endExclusive }
            .map { it.money.currencyCode }
            .toSet()
        val rates = exchangeRateService.getRates(period, currentProfile.baseCurrency, currencies)
        missingCurrencies = currencies - rates.keys
        summary = if (missingCurrencies.isEmpty()) {
            summaryService.summarize(period, currentProfile.baseCurrency, rates)
        } else {
            showRateDialog = true
            null
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Button(
                    onClick = { onAdd(selectedType) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
                ) {
                    Text("＋  ${stringResource(R.string.operation_add)}", style = MaterialTheme.typography.titleMedium)
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.operations_title), style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { viewModel.selectType(TransactionType.EXPENSE) },
                        label = { Text(stringResource(R.string.operation_expenses)) },
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { viewModel.selectType(TransactionType.INCOME) },
                        label = { Text(stringResource(R.string.operation_income)) },
                    )
                }
                Text(stringResource(R.string.operation_month_total), style = MaterialTheme.typography.titleMedium)
                val currentSummary = summary
                val missing = missingCurrencies
                if (missing.isNotEmpty()) {
                    Text(
                        stringResource(R.string.operation_need_rates, baseCurrency),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = { showRateDialog = true }) {
                        Text(stringResource(R.string.exchange_rate_open))
                    }
                } else if (currentSummary == null) {
                    Text("0.00 $baseCurrency", style = MaterialTheme.typography.headlineSmall)
                } else {
                    val total = if (selectedType == TransactionType.INCOME) currentSummary.income
                    else currentSummary.expense
                    Text("${formatAmount(total.minorUnits)} $baseCurrency", style = MaterialTheme.typography.headlineSmall)
                }
                if (transactions.isEmpty()) {
                    Text(stringResource(R.string.operation_empty), modifier = Modifier.padding(top = 16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(transactions, key = { it.id }) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                categoryLabel = categories.firstOrNull { it.id == transaction.categoryId }
                                    ?.let { category ->
                                        if (languageTag == "ru") category.nameRu ?: category.nameEn.orEmpty()
                                        else category.nameEn ?: category.nameRu.orEmpty()
                                    },
                                onEdit = { onEdit(transaction) },
                                onDelete = { viewModel.requestDelete(transaction.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.operation_delete_title)) },
            text = { Text(stringResource(R.string.operation_delete_message)) },
            confirmButton = {
                TextButton(onClick = { scope.launch { viewModel.confirmDelete() } }) {
                    Text(stringResource(R.string.operation_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.operation_cancel)) }
            },
        )
    }

    if (showRateDialog && missingCurrencies.isNotEmpty()) {
        ExchangeRatePromptDialog(
            baseCurrency = baseCurrency,
            sourceCurrencies = missingCurrencies,
            onSave = { entered ->
                scope.launch {
                    val current = exchangeRateService.getRates(period, baseCurrency, missingCurrencies)
                    exchangeRateService.saveRates(period, baseCurrency, current + entered)
                }
                showRateDialog = false
            },
            onDismiss = { showRateDialog = false },
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: FinanceTransaction,
    categoryLabel: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(FinanceDateFormat.format(transaction.date))
                if (transaction.type == TransactionType.INCOME && transaction.comment.isNotBlank()) Text(transaction.comment)
                if (!categoryLabel.isNullOrBlank()) Text(categoryLabel, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("${formatAmount(transaction.money.minorUnits)} ${transaction.money.currencyCode}")
                Row {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.operation_edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.operation_delete)) }
                }
            }
        }
    }
}

private fun formatAmount(minorUnits: Long): String =
    BigDecimal.valueOf(minorUnits, 2).setScale(2).toPlainString()
