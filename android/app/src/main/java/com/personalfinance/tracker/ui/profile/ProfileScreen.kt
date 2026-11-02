package com.personalfinance.tracker.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.personalfinance.tracker.ui.theme.FinanceButton as Button
import com.personalfinance.tracker.ui.theme.FinanceCard as Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.personalfinance.tracker.ui.categories.CategoryIcon
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.UserProfile

private val supportedCurrencies = listOf("USD", "EUR", "RUB", "BYN")
enum class ProfileSection { ALL, MORE, THEMES, SECURITY, BACKUP, CATEGORIES, UPDATES }

@Composable
fun ProfileScreen(
    profile: UserProfile,
    categories: List<Category>,
    onSave: (name: String, languageTag: String, baseCurrency: String) -> Unit,
    onAddCategory: suspend (name: String, languageTag: String, iconKey: String) -> Boolean,
    onEditCategory: suspend (id: String, name: String, iconKey: String) -> Boolean,
    onArchiveCategory: suspend (id: String) -> Boolean,
    onOpenFamily: () -> Unit,
    onOpenGoal: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    selectedTheme: com.personalfinance.tracker.ui.theme.AppTheme = com.personalfinance.tracker.ui.theme.AppThemeRegistry.default,
    onSelectTheme: (Int) -> Unit = {},
    backupBusy: Boolean = false,
    backupStatus: String? = null,
    section: ProfileSection = ProfileSection.ALL,
    onOpenSection: (ProfileSection) -> Unit = {},
    onBack: () -> Unit = {},
    onExportFullBackup: () -> Unit = {},
    onRestoreFullBackup: () -> Unit = {},
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var language by remember(profile.languageTag) { mutableStateOf(profile.languageTag) }
    var currency by remember(profile.baseCurrency) { mutableStateOf(profile.baseCurrency) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var removingCategory by remember { mutableStateOf<Category?>(null) }
    val scope = rememberCoroutineScope()
    var currencyMenu by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (section != ProfileSection.ALL && section != ProfileSection.MORE) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.family_back)) }
            }
            if (section == ProfileSection.ALL || section == ProfileSection.MORE) {
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
            if (section == ProfileSection.MORE) {
                listOf(ProfileSection.THEMES to R.string.themes_title, ProfileSection.SECURITY to R.string.security_title,
                    ProfileSection.BACKUP to R.string.backup_title, ProfileSection.CATEGORIES to R.string.profile_categories,
                    ProfileSection.UPDATES to R.string.updates_title).forEach { (target, label) ->
                    Button(onClick = { onOpenSection(target) }, modifier = Modifier.fillMaxWidth().testTag("open_${target.name}")) { Text(stringResource(label)) }
                }
            }
            }
            if (section == ProfileSection.ALL || section == ProfileSection.THEMES) com.personalfinance.tracker.ui.theme.ThemePicker(selectedTheme, onSelectTheme)
            if (section == ProfileSection.ALL || section == ProfileSection.SECURITY) {
                Text(stringResource(R.string.security_title), style = MaterialTheme.typography.titleLarge)
                com.personalfinance.tracker.ui.security.SecuritySettings()
            }
            if (section == ProfileSection.ALL || section == ProfileSection.BACKUP) {
            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.full_backup_unencrypted))
            Button(onClick = onExportFullBackup, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.full_backup_export))
            }
            Button(onClick = onRestoreFullBackup, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.full_backup_restore))
            }
            Text(stringResource(R.string.backup_description))
            Button(onClick = onExportBackup, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_export))
            }
            Button(onClick = onRestoreBackup, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_restore))
            }
            if (backupBusy) Text(stringResource(R.string.backup_working))
            backupStatus?.let { Text(it) }
            }

            if (section == ProfileSection.ALL || section == ProfileSection.CATEGORIES) {
            Text(stringResource(R.string.profile_categories), style = MaterialTheme.typography.titleLarge)
            categories.filterNot { it.isArchived }.forEach { category ->
                val label = if (language == "ru") category.nameRu ?: category.nameEn.orEmpty()
                else category.nameEn ?: category.nameRu.orEmpty()
                Card(modifier = Modifier.fillMaxWidth().testTag("category_row_${category.id}"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryIcon(category.iconKey)
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = if (category.isCustom) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    )
                    }
                    Row {
                        TextButton(onClick = { editingCategory = category; editorOpen = true }, modifier = Modifier.testTag("category_edit_${category.id}")) { Text(stringResource(R.string.category_edit)) }
                        TextButton(onClick = { removingCategory = category }, modifier = Modifier.testTag("category_delete_${category.id}")) { Text(stringResource(R.string.category_delete)) }
                    }
                }
            }
            Button(
                onClick = { editingCategory = null; editorOpen = true },
                modifier = Modifier.fillMaxWidth().testTag("category_add"),
            ) {
                Text(stringResource(R.string.profile_add_category))
            }
            }
        }
    }
    if (editorOpen) CategoryEditor(editingCategory, language, { editorOpen = false }) { name, icon ->
        editingCategory?.let { onEditCategory(it.id, name, icon) } ?: onAddCategory(name, language, icon)
    }
    removingCategory?.let { category ->
        AlertDialog(onDismissRequest = { removingCategory = null }, title = { Text(stringResource(R.string.category_delete)) },
            text = { Text(stringResource(R.string.category_delete_message)) },
            confirmButton = { TextButton(modifier = Modifier.testTag("category_delete_confirm"), onClick = { scope.launch { onArchiveCategory(category.id); removingCategory = null } }) { Text(stringResource(R.string.category_delete)) } },
            dismissButton = { TextButton(onClick = { removingCategory = null }) { Text(stringResource(R.string.operation_cancel)) } })
    }
}
