package com.personalfinance.tracker.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.UserProfile

private val supportedCurrencies = listOf("USD", "EUR", "RUB", "BYN")

@Composable
fun ProfileScreen(
    profile: UserProfile,
    categories: List<Category>,
    onSave: (name: String, languageTag: String, baseCurrency: String) -> Unit,
    onAddCategory: (name: String, languageTag: String) -> Unit,
    onOpenFamily: () -> Unit,
    onOpenGoal: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    backupBusy: Boolean = false,
    backupStatus: String? = null,
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var language by remember(profile.languageTag) { mutableStateOf(profile.languageTag) }
    var currency by remember(profile.baseCurrency) { mutableStateOf(profile.baseCurrency) }
    var categoryName by remember { mutableStateOf("") }
    var currencyMenu by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
            )
            Text(stringResource(R.string.profile_language), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = language == "ru", onClick = { language = "ru" }, label = { Text("Русский") })
                FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("English") })
            }
            Text(stringResource(R.string.profile_currency), style = MaterialTheme.typography.titleMedium)
            Column {
                TextButton(onClick = { currencyMenu = true }) {
                    Text(currency, style = MaterialTheme.typography.bodyLarge)
                }
                DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                    supportedCurrencies.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = { currency = code; currencyMenu = false },
                        )
                    }
                }
            }
            Button(
                onClick = { onSave(name, language, currency) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_save))
            }
            Button(onClick = onOpenFamily, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.family_open))
            }
            Button(onClick = onOpenGoal, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.goal_open))
            }
            com.personalfinance.tracker.ui.security.SecuritySettings()
            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.backup_description))
            Button(onClick = onExportBackup, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_export))
            }
            Button(onClick = onRestoreBackup, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_restore))
            }
            if (backupBusy) Text(stringResource(R.string.backup_working))
            backupStatus?.let { Text(it) }

            Text(stringResource(R.string.profile_categories), style = MaterialTheme.typography.titleLarge)
            categories.forEach { category ->
                val label = if (language == "ru") category.nameRu ?: category.nameEn.orEmpty()
                else category.nameEn ?: category.nameRu.orEmpty()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = if (category.isCustom) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_new_category)) },
                singleLine = true,
            )
            Button(
                onClick = {
                    onAddCategory(categoryName, language)
                    categoryName = ""
                },
                enabled = categoryName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_add_category))
            }
        }
    }
}
