package com.personalfinance.tracker.ui.family

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.R
import com.personalfinance.tracker.data.csv.FamilyCsvValidationException
import com.personalfinance.tracker.data.csv.OpenCsvDocument
import com.personalfinance.tracker.data.csv.FamilyImportManager
import com.personalfinance.tracker.data.csv.FamilyImportPreview
import com.personalfinance.tracker.data.csv.FamilyReportCalculator
import com.personalfinance.tracker.ui.common.DatePickerField
import com.personalfinance.tracker.ui.charts.CategoryBreakdownItem
import com.personalfinance.tracker.ui.charts.FinanceBar
import com.personalfinance.tracker.ui.charts.FinanceBarGroup
import com.personalfinance.tracker.ui.charts.FinanceCharts
import com.personalfinance.tracker.ui.theme.FinancePalette
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import com.personalfinance.tracker.ui.summary.ExchangeRatePromptDialog
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun FamilyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as FinanceApplication).container }
    val profile by container.financeRepository.observeProfile().collectAsState(initial = null)
    val categories by container.financeRepository.observeCategories().collectAsState(initial = emptyList())
    val allRange = remember { PeriodRange(LocalDate.of(1, 1, 1), LocalDate.of(9999, 12, 31)) }
    val allRows by container.familyRepository.observeFamilyTransactions(allRange).collectAsState(initial = emptyList())
    val allPersonalTransactions by container.financeRepository.observeTransactions(null).collectAsState(initial = emptyList())
    val firstFamilyDate = allRows.minOfOrNull { it.date }
    val firstPersonalDate = allPersonalTransactions.minOfOrNull { it.date }
    val firstDate = listOfNotNull(firstFamilyDate, firstPersonalDate).minOrNull()
    val today = remember { LocalDate.now() }
    val defaultStart = firstDate?.takeIf { it < today.withDayOfMonth(1) } ?: today.withDayOfMonth(1)
    var range by remember { mutableStateOf(PeriodRange(defaultStart, today.plusDays(1))) }
    var startText by remember { mutableStateOf(FinanceDateFormat.format(range.start)) }
    var endText by remember { mutableStateOf(FinanceDateFormat.format(range.endExclusive.minusDays(1))) }
    val rows by container.familyRepository.observeFamilyTransactions(range).collectAsState(initial = emptyList())
    val personalTransactions = allPersonalTransactions.filter { it.date >= range.start && it.date < range.endExclusive }
    val baseCurrency = profile?.baseCurrency ?: "USD"
    val language = profile?.languageTag ?: "en"
    val savedRates by container.rateRepository.ratesFor(range, baseCurrency).collectAsState(initial = emptyMap())
    val missingRates = FamilyReportCalculator.missingCurrencies(rows, personalTransactions, baseCurrency, savedRates)
    val categoryNamesById = categories.associate { category ->
        category.id to if (profile?.languageTag == "ru") category.nameRu ?: category.nameEn.orEmpty()
        else category.nameEn ?: category.nameRu.orEmpty()
    }
    val report = if (missingRates.isEmpty()) FamilyReportCalculator.calculate(
        rows, personalTransactions, baseCurrency, savedRates, categoryNamesById,
        if (profile?.languageTag == "ru") "Без категории" else "Uncategorized",
    ) else null
    val scope = rememberCoroutineScope()
    val manager = remember(container.familyRepository, context) {
        FamilyImportManager(container.familyRepository) { uri ->
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Cannot open selected CSV file")
        }
    }
    var preview by remember { mutableStateOf<FamilyImportPreview?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var rateDialogMode by remember { mutableStateOf<RateDialogMode?>(null) }
    var importRates by remember { mutableStateOf<Map<String, BigDecimal>>(emptyMap()) }
    var dateErrorRes by remember { mutableStateOf<Int?>(null) }
    var ownerToClear by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(OpenCsvDocument()) { uri ->
        if (uri != null) {
            importError = null
            importMessage = null
            preview = null
            try {
                val parsed = manager.preview(uri)
                preview = parsed
                val missing = parsed.sourceCurrencies - baseCurrency
                if (missing.isEmpty()) {
                    scope.launch { finishImport(parsed, baseCurrency, emptyMap(), manager) { result ->
                        importMessage = context.getString(R.string.family_import_done, result.addedCount, result.duplicateCount)
                        importError = result.errors.takeIf { it.isNotEmpty() }?.joinToString()
                    } }
                } else rateDialogMode = RateDialogMode.Import(missing)
            } catch (error: FamilyCsvValidationException) {
                importError = error.validationErrors.joinToString("\n")
            } catch (error: Exception) {
                importError = error.localizedMessage ?: "Invalid CSV file"
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.family_title), style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onBack) { Text(stringResource(R.string.family_back)) }
            }
            OutlinedButton(onClick = { picker.launch(Unit) }) {
                Text(stringResource(R.string.family_import))
            }
            preview?.let { selected ->
                Text(stringResource(
                    R.string.family_import_preview,
                    selected.ownerNames.joinToString(),
                    selected.sourceCurrencies.joinToString(),
                    selected.records.size,
                ))
            }
            importMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            importError?.let { Text(stringResource(R.string.family_import_error, it), color = MaterialTheme.colorScheme.error) }

            Text(stringResource(R.string.family_period), style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    value = startText, onValueChange = { startText = it; dateErrorRes = null },
                    modifier = Modifier.fillMaxWidth(), label = stringResource(R.string.family_from),
                )
                DatePickerField(
                    value = endText, onValueChange = { endText = it; dateErrorRes = null },
                    modifier = Modifier.fillMaxWidth(), label = stringResource(R.string.family_to),
                )
            }
            Button(onClick = {
                when (val validation = FamilyPeriodValidator.validate(startText, endText, firstDate)) {
                    FamilyPeriodValidationResult.INVALID_DATE_FORMAT -> dateErrorRes = R.string.family_invalid_date
                    FamilyPeriodValidationResult.BEFORE_FIRST_RECORD -> dateErrorRes = R.string.family_before_first_record
                    FamilyPeriodValidationResult.START_AFTER_END -> dateErrorRes = R.string.family_start_after_end
                    is FamilyPeriodValidationResult.Valid -> {
                        dateErrorRes = null
                        range = PeriodRange(validation.start, validation.endInclusive.plusDays(1))
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.family_apply)) }
            dateErrorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

            if (rows.isEmpty() && personalTransactions.isEmpty()) {
                Text(stringResource(R.string.family_no_records))
            } else if (missingRates.isNotEmpty()) {
                Text(stringResource(R.string.family_need_rates, missingRates.sorted().joinToString(), baseCurrency))
                OutlinedButton(onClick = { rateDialogMode = RateDialogMode.Rebase(missingRates) }) {
                    Text(stringResource(R.string.exchange_rate_open))
                }
            } else if (report != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.family_totals), style = MaterialTheme.typography.titleLarge)
                        val balance = Math.subtractExact(report.incomeMinorUnits, report.expensesMinorUnits)
                        Text(stringResource(R.string.family_balance), style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${if (balance > 0) "+" else if (balance < 0) "−" else ""}${BigDecimal.valueOf(balance, 2).abs().setScale(2).toPlainString()} $baseCurrency",
                            style = MaterialTheme.typography.displaySmall,
                            color = when {
                                balance > 0 -> FinancePalette.income
                                balance < 0 -> FinancePalette.expense
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Text(stringResource(R.string.home_income), style = MaterialTheme.typography.labelLarge)
                        Text("${amount(report.incomeMinorUnits)} $baseCurrency",
                            style = MaterialTheme.typography.headlineSmall, color = FinancePalette.income)
                        Text(stringResource(R.string.home_expenses), style = MaterialTheme.typography.labelLarge)
                        Text("${amount(report.expensesMinorUnits)} $baseCurrency",
                            style = MaterialTheme.typography.headlineSmall, color = FinancePalette.expense)
                    }
                }
                Text(stringResource(R.string.family_by_owner), style = MaterialTheme.typography.titleLarge)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(baseCurrency, style = MaterialTheme.typography.labelLarge)
                        FinanceCharts.GroupedColumnChart(groups = listOf(
                            FinanceBarGroup(stringResource(R.string.chart_income), report.byOwner.map { (owner, totals) ->
                                FinanceBar(owner, totals.incomeMinorUnits, FinancePalette.income)
                            }),
                            FinanceBarGroup(stringResource(R.string.chart_expense), report.byOwner.map { (owner, totals) ->
                                FinanceBar(owner, totals.expensesMinorUnits, FinancePalette.expense)
                            }),
                        ), barWidth = 72.dp)
                        Text(stringResource(R.string.family_chart_scroll), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.family_by_category), style = MaterialTheme.typography.titleMedium)
                        Text(baseCurrency, style = MaterialTheme.typography.labelLarge)
                        if (report.expensesByCategory.isEmpty()) Text(stringResource(R.string.reports_no_expenses))
                        else FinanceCharts.CategoryBreakdownBars(items = report.expensesByCategory.entries
                            .sortedByDescending { it.value }
                            .map { (category, value) ->
                                CategoryBreakdownItem(category, value,
                                    if (report.expensesMinorUnits <= 0L) 0f
                                    else (value.toDouble() / report.expensesMinorUnits.toDouble()).toFloat())
                            })
                    }
                }
            }
            // Keep removal available even if the selected period contains no imported records.
            if (allRows.isNotEmpty()) {
                Text(stringResource(R.string.family_imported_members), style = MaterialTheme.typography.titleMedium)
                allRows.map { it.owner }.distinct().sorted().forEach { owner ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(owner, style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { ownerToClear = owner }) {
                                Text(stringResource(R.string.family_clear_member))
                            }
                        }
                    }
                }
            }
        }
    }

    ownerToClear?.let { owner ->
        AlertDialog(
            onDismissRequest = { ownerToClear = null },
            title = { Text(stringResource(R.string.family_clear_title)) },
            text = { Text(stringResource(R.string.family_clear_message, owner)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.familyRepository.deleteImportedRecordsForOwner(owner)
                        importMessage = context.getString(R.string.family_clear_done, owner)
                        ownerToClear = null
                    }
                }) { Text(stringResource(R.string.family_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { ownerToClear = null }) { Text(stringResource(R.string.operation_cancel)) }
            },
        )
    }

    when (val mode = rateDialogMode) {
        is RateDialogMode.Import -> ExchangeRatePromptDialog(
            baseCurrency = baseCurrency, sourceCurrencies = mode.currencies,
            onSave = { entered ->
                importRates = entered
                rateDialogMode = null
                val selected = preview
                if (selected != null) scope.launch {
                    finishImport(selected, baseCurrency, entered, manager) { result ->
                        importMessage = context.getString(R.string.family_import_done, result.addedCount, result.duplicateCount)
                        importError = result.errors.takeIf { it.isNotEmpty() }?.joinToString()
                    }
                }
            }, onDismiss = { rateDialogMode = null },
        )
        is RateDialogMode.Rebase -> ExchangeRatePromptDialog(
            baseCurrency = baseCurrency,
            sourceCurrencies = mode.currencies,
            onSave = { entered -> scope.launch {
                container.rateRepository.saveRates(range, baseCurrency, savedRates + entered)
                rateDialogMode = null
            } },
            onDismiss = { rateDialogMode = null },
        )
        null -> Unit
    }
}

private suspend fun finishImport(
    preview: FamilyImportPreview,
    baseCurrency: String,
    rates: Map<String, BigDecimal>,
    manager: FamilyImportManager,
    onResult: (com.personalfinance.tracker.data.csv.FamilyImportResult) -> Unit,
) {
    runCatching { manager.import(preview, baseCurrency, rates) }
        .onSuccess(onResult)
        .onFailure { throw it }
}

private sealed interface RateDialogMode {
    val currencies: Set<String>
    data class Import(override val currencies: Set<String>) : RateDialogMode
    data class Rebase(override val currencies: Set<String>) : RateDialogMode
}

private fun amount(minorUnits: Long) = BigDecimal.valueOf(minorUnits, 2).setScale(2).toPlainString()
