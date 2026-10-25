package com.personalfinance.tracker.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.R

private data class CurrencyOption(val code: String, val symbol: String)

private val currencies = listOf(
    CurrencyOption("USD", "$"),
    CurrencyOption("EUR", "€"),
    CurrencyOption("RUB", "₽"),
    CurrencyOption("BYN", "Br"),
)

@Composable
fun WelcomeScreen(
    languageTag: String,
    onComplete: (name: String, currencyCode: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.welcome_subtitle),
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
            )
            Text(
                stringResource(R.string.profile_currency),
                modifier = Modifier.align(Alignment.Start).padding(top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currencies.chunked(2).forEach { rowCurrencies ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCurrencies.forEach { option ->
                            FilterChip(
                                selected = selectedCurrency == option.code,
                                onClick = { selectedCurrency = option.code },
                                label = { Text(option.code) },
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { onComplete(name, selectedCurrency) },
                enabled = name.isNotBlank() && selectedCurrency in currencies.map { it.code },
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            ) {
                Text(stringResource(R.string.welcome_continue))
            }
        }
    }
}
