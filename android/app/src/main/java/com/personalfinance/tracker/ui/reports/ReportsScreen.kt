package com.personalfinance.tracker.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.personalfinance.tracker.ui.theme.FinanceButton as Button
import com.personalfinance.tracker.ui.theme.FinanceCard as Card
import androidx.compose.material3.MaterialTheme
import com.personalfinance.tracker.ui.theme.FinanceOutlinedButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.R
import com.personalfinance.tracker.data.csv.ExportResult
import com.personalfinance.tracker.data.csv.CreateCsvDocument
import com.personalfinance.tracker.data.csv.ReportExportManager
import com.personalfinance.tracker.data.csv.CsvFormat
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.money.ExchangeRateService
import com.personalfinance.tracker.domain.summary.SummaryService
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import com.personalfinance.tracker.ui.charts.CategoryBreakdownItem
import com.personalfinance.tracker.ui.charts.FinanceBar
import com.personalfinance.tracker.ui.charts.FinanceBarGroup
import com.personalfinance.tracker.ui.charts.FinanceCharts
import com.personalfinance.tracker.ui.theme.financeColors
import com.personalfinance.tracker.ui.common.DatePickerField
import com.personalfinance.tracker.ui.summary.ExchangeRatePromptDialog
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as FinanceApplication).container }
    val repository = container.financeRepository
    val profile by repository.observeProfile().collectAsState(initial = null)
    val transactions by repository.observeTransactions(null).collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val today = remember { LocalDate.now() }
    val todayExclusive = remember { today.plusDays(1) }
    val fallback = remember { PeriodRange(today.withDayOfMonth(1), todayExclusive) }
    var selectedRange by remember { mutableStateOf(fallback) }
    var initialized by remember { mutableStateOf(false) }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<ReportUiState?>(null) }
    var showRateDialog by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var dateError by remember { mutableStateOf<Int?>(null) }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var pendingStart by rememberSaveable { mutableStateOf("") }
    var pendingEndExclusive by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val rateRepository = container.rateRepository
    val baseCurrency = profile?.baseCurrency ?: "USD"
    val languageTag = profile?.languageTag ?: "en"
    val exchangeRateService = remember(rateRepository) { ExchangeRateService(rateRepository) }
    val viewModel = remember(repository, rateRepository) {
        ReportsViewModel(repository, exchangeRateService, SummaryService(repository))
    }
    val exportManager = remember(context, repository) {
        ReportExportManager(context.contentResolver, repository)
    }
    val savedRates by rateRepository.ratesFor(selectedRange, baseCurrency).collectAsState(initial = emptyMap())
    val createDocument = rememberLauncherForActivityResult(CreateCsvDocument()) { uri ->
        if (uri != null) {
            // The external picker may recreate this screen. Export the request the
            // user launched, not the newly initialized screen's range.
            // Reports always use the current format, including restored requests.
            val requestRange = runCatching {
                PeriodRange(LocalDate.parse(pendingStart), LocalDate.parse(pendingEndExclusive))
            }.getOrNull()
            scope.launch {
                exportResult = if (requestRange != null)
                    exportManager.exportReport(requestRange, uri, CsvFormat.CURRENT)
                else ExportResult(false, context.getString(R.string.reports_export_request_lost))
            }
        }
    }

    LaunchedEffect(profile, transactions, savedRates, selectedRange, initialized) {
        if (!initialized) {
            selectedRange = viewModel.defaultRange()
            startText = FinanceDateFormat.format(selectedRange.start)
            endText = FinanceDateFormat.format(selectedRange.endExclusive.minusDays(1))
            initialized = true
            return@LaunchedEffect
        }
        val currentProfile = profile ?: return@LaunchedEffect
        state = viewModel.load(selectedRange, currentProfile.baseCurrency, currentProfile.languageTag)
        showRateDialog = state?.missingCurrencies?.isNotEmpty() == true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.reports_period), style = MaterialTheme.typography.titleMedium)
            BoxWithConstraints {
                val minimumRowWidth = 360.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = if (maxWidth < minimumRowWidth) 1 else 2,
                ) {
                    DatePickerField(
                        value = startText,
                        onValueChange = { startText = it; exportResult = null; dateError = null },
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.reports_from),
                        supportingText = stringResource(R.string.reports_date_format),
                    )
                    DatePickerField(
                        value = endText,
                        onValueChange = { endText = it; exportResult = null; dateError = null },
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.reports_to),
                        supportingText = stringResource(R.string.reports_date_format),
                    )
                }
            }
            Button(
                onClick = {
                    val start = FinanceDateFormat.parse(startText)
                    val endInclusive = FinanceDateFormat.parse(endText)
                    val firstDate = state?.firstTransactionDate
                    if (start == null || endInclusive == null) {
                        dateError = R.string.reports_error_invalid_dates
                    } else if (firstDate != null && start < firstDate) {
                        dateError = R.string.reports_error_before_first
                    } else if (start > endInclusive) {
                        dateError = R.string.reports_error_range
                    } else {
                        dateError = null
                        selectedRange = PeriodRange(start, endInclusive.plusDays(1))
                        exportResult = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reports_apply))
            }

            val current = state
            dateError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            current?.error?.let { error ->
                val message = when (error) {
                    ReportError.BeforeFirstTransaction -> stringResource(
                        R.string.reports_error_before_first,
                        current.firstTransactionDate?.let(FinanceDateFormat::format).orEmpty(),
                    )
                    ReportError.InvalidRange -> stringResource(R.string.reports_error_range)
                }
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            if (current?.missingCurrencies?.isNotEmpty() == true) {
                Text(
                    stringResource(R.string.reports_need_rates, current.missingCurrencies.sorted().joinToString()),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { showRateDialog = true }) {
                    Text(stringResource(R.string.exchange_rate_open))
                }
            } else if (current?.summary != null) {
                val summary = current.summary
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.reports_comparison), style = MaterialTheme.typography.titleLarge)
                        Text(baseCurrency, style = MaterialTheme.typography.labelLarge)
                        if (current.firstTransactionDate != null) {
                            FinanceCharts.GroupedColumnChart(
                                groups = listOf(
                                    FinanceBarGroup(stringResource(R.string.chart_income), listOf(
                                        FinanceBar(stringResource(R.string.chart_selected_period), summary.income.minorUnits, financeColors.income),
                                        FinanceBar(stringResource(R.string.chart_previous_period), current.previousSummary?.income?.minorUnits ?: 0L, financeColors.income.copy(alpha = 0.45f)),
                                    )),
                                    FinanceBarGroup(stringResource(R.string.chart_expense), listOf(
                                        FinanceBar(stringResource(R.string.chart_selected_period), summary.expense.minorUnits, financeColors.expense),
                                        FinanceBar(stringResource(R.string.chart_previous_period), current.previousSummary?.expense?.minorUnits ?: 0L, financeColors.expense.copy(alpha = 0.45f)),
                                    )),
                                ),
                            )
                        }
                        Text(stringResource(R.string.reports_current_balance), style = MaterialTheme.typography.titleMedium)
                        Text(formatSignedAmount(summary.balance.minorUnits, baseCurrency), style = MaterialTheme.typography.headlineSmall)
                    }
                }
                current.previousSummary?.let { previous ->
                    val previousRange = current.previousRange
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.reports_comparison_previous), style = MaterialTheme.typography.titleLarge)
                            if (previousRange != null) {
                                Text(stringResource(
                                    R.string.reports_previous_period_range,
                                    FinanceDateFormat.format(previousRange.start),
                                    FinanceDateFormat.format(previousRange.endExclusive.minusDays(1)),
                                ))
                            }
                            Text(stringResource(R.string.reports_delta_income,
                                formatDeltaAmount(summary.income.minorUnits, previous.income.minorUnits, baseCurrency),
                                changePercent(summary.income.minorUnits, previous.income.minorUnits, context.getString(R.string.reports_new_value))))
                            Text(stringResource(R.string.reports_delta_expense,
                                formatDeltaAmount(summary.expense.minorUnits, previous.expense.minorUnits, baseCurrency),
                                changePercent(summary.expense.minorUnits, previous.expense.minorUnits, context.getString(R.string.reports_new_value))))
                            Text(stringResource(R.string.reports_delta_balance,
                                formatDeltaAmount(summary.balance.minorUnits, previous.balance.minorUnits, baseCurrency),
                                changePercent(summary.balance.minorUnits, previous.balance.minorUnits, context.getString(R.string.reports_new_value))))
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.reports_categories), style = MaterialTheme.typography.titleLarge)
                        if (current.categories.isEmpty()) {
                            Text(stringResource(R.string.reports_no_expenses))
                        } else {
                            Text(baseCurrency, style = MaterialTheme.typography.labelLarge)
                            FinanceCharts.CategoryBreakdownBars(
                                items = current.categories.map { category ->
                                    CategoryBreakdownItem(
                                        label = category.name,
                                        iconKey = category.iconKey,
                                        valueMinorUnits = category.minorUnits,
                                        share = if (summary.expense.minorUnits <= 0L) 0f
                                        else (category.minorUnits.toDouble() / summary.expense.minorUnits.toDouble()).toFloat(),
                                    )
                                },
                                onCategoryClick = { index ->
                                    val key = current.categories[index].categoryId ?: "__uncategorized__"
                                    expandedCategory = if (expandedCategory == key) null else key
                                },
                                details = { index ->
                                val category = current.categories[index]
                                val key = category.categoryId ?: "__uncategorized__"
                                if (expandedCategory == key) category.details.forEach { detail ->
                                    val transaction = detail.transaction
                                    Column(Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 8.dp)) {
                                        Text(FinanceDateFormat.format(transaction.date))
                                        if (transaction.comment.isNotBlank()) Text(transaction.comment)
                                        Text("${formatAmount(detail.convertedMinorUnits)} $baseCurrency")
                                        if (transaction.money.currencyCode != baseCurrency) Text("${formatAmount(transaction.money.minorUnits)} ${transaction.money.currencyCode}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                },
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    pendingStart = selectedRange.start.toString()
                    pendingEndExclusive = selectedRange.endExclusive.toString()
                    createDocument.launch("finance_report_${selectedRange.start}_${selectedRange.endExclusive.minusDays(1)}.csv")
                },
                enabled = current != null && current.error == null && current.missingCurrencies.isEmpty() && dateError == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reports_export_csv))
            }
            exportResult?.let { result ->
                Text(
                    text = if (result.success) stringResource(R.string.reports_export_success)
                    else stringResource(R.string.reports_export_error, result.message.orEmpty()),
                    color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            if (current?.firstTransactionDate != null) {
                Text(stringResource(R.string.reports_first_transaction, FinanceDateFormat.format(current.firstTransactionDate)))
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
                    viewModel.saveRates(selectedRange, baseCurrency, entered)
                    state = viewModel.load(selectedRange, baseCurrency, languageTag)
                    showRateDialog = state?.missingCurrencies?.isNotEmpty() == true
                }
            },
            onDismiss = { showRateDialog = false },
        )
    }
}

private fun formatAmount(minorUnits: Long): String =
    BigDecimal.valueOf(minorUnits, 2).setScale(2).toPlainString()

private fun formatSignedAmount(minorUnits: Long, currencyCode: String): String {
    val sign = when { minorUnits > 0 -> "+"; minorUnits < 0 -> "−"; else -> "" }
    return "$sign${BigDecimal.valueOf(minorUnits, 2).abs().setScale(2).toPlainString()} $currencyCode"
}

private fun formatDeltaAmount(current: Long, previous: Long, currencyCode: String): String {
    val delta = BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous))
    val sign = when { delta > BigDecimal.ZERO -> "+"; delta < BigDecimal.ZERO -> "−"; else -> "" }
    return "$sign${delta.abs().movePointLeft(2).setScale(2).toPlainString()} $currencyCode"
}

private fun changePercent(current: Long, previous: Long, newValue: String): String {
    if (previous == 0L) return if (current == 0L) "0%" else newValue
    val change = BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous))
    val percent = change.multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(kotlin.math.abs(previous)), 1, java.math.RoundingMode.HALF_UP)
    return "${if (percent > BigDecimal.ZERO) "+" else ""}${percent.toPlainString()}%"
}
