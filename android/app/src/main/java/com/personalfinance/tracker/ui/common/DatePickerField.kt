package com.personalfinance.tracker.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A manually editable DD-MM-YYYY field with a Material date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    val openPickerLabel = stringResource(R.string.date_picker_open)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = supportingText?.let { helper -> ({ Text(helper) }) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Text("▦", color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics {
                    contentDescription = openPickerLabel
                })
            }
        },
    )

    if (showPicker) {
        val selectedDate = FinanceDateFormat.parse(value) ?: LocalDate.now()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(FinanceDateFormat.format(date))
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.date_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState, title = { Text(stringResource(R.string.date_picker_title)) })
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
