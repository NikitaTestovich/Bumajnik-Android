package com.personalfinance.tracker.ui.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.R

@Composable
internal fun AmountCalculatorDialog(initial: String, onDismiss: () -> Unit, onApply: (String) -> Unit) {
    var expression by rememberSaveable { mutableStateOf(initial.takeIf { it.matches(Regex("[0-9]+([.,][0-9]+)?")) }.orEmpty()) }
    var showError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calculator_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(expression.ifEmpty { "0" }, style = MaterialTheme.typography.headlineSmall)
                listOf(listOf("7", "8", "9", "÷"), listOf("4", "5", "6", "×"),
                    listOf("1", "2", "3", "−"), listOf("C", "0", ",", "+"), listOf("⌫", "=")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { key ->
                            TextButton(onClick = {
                                if (key == "=") {
                                    val result = calculateAmount(expression)
                                    if (result == null) showError = true else { expression = result; showError = false }
                                } else {
                                    expression = when (key) {
                                        "C" -> ""
                                        "⌫" -> expression.dropLast(1)
                                        "−" -> (expression + "-").take(120)
                                        else -> (expression + key).take(120)
                                    }
                                    showError = false
                                }
                            }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) { Text(key) }
                        }
                    }
                }
                if (showError) Text(stringResource(R.string.calculator_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            val result = calculateAmount(expression)
            if (result == null) showError = true else onApply(result)
        }) { Text(stringResource(R.string.calculator_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.operation_cancel)) } },
    )
}
