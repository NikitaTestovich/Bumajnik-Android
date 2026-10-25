package com.personalfinance.tracker.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.R
import java.math.BigDecimal

@Composable
fun ExchangeRatePromptDialog(
    baseCurrency: String,
    sourceCurrencies: Set<String>,
    onSave: (Map<String, BigDecimal>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sortedCurrencies = sourceCurrencies.sorted()
    val values = remember(baseCurrency, sortedCurrencies) {
        mutableStateMapOf<String, String>().apply {
            sortedCurrencies.forEach { put(it, "") }
        }
    }
    val parsed = sortedCurrencies.mapNotNull { currency ->
        val value = values[currency]?.trim()?.replace(',', '.')?.toBigDecimalOrNull()
        if (value != null && value > BigDecimal.ZERO) currency to value else null
    }.toMap()
    val canSave = parsed.size == sortedCurrencies.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exchange_rate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.exchange_rate_description, baseCurrency))
                sortedCurrencies.forEach { currency ->
                    OutlinedTextField(
                        value = values[currency].orEmpty(),
                        onValueChange = { values[currency] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.exchange_rate_label, currency, baseCurrency)) },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (canSave) onSave(parsed) }, enabled = canSave) {
                Text(stringResource(R.string.exchange_rate_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(start = 4.dp)) {
                Text(stringResource(R.string.operation_cancel))
            }
        },
    )
}
