package com.personalfinance.tracker.ui.profile

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.CategoryIcons
import com.personalfinance.tracker.domain.model.label
import com.personalfinance.tracker.ui.categories.CategoryIcon
import kotlinx.coroutines.launch

@Composable
fun CategoryEditor(category: Category?, language: String, onDismiss: () -> Unit, onSave: suspend (String, String) -> Boolean) {
    var name by rememberSaveable(category?.id) { mutableStateOf(category?.label(language).orEmpty()) }
    var icon by rememberSaveable(category?.id) { mutableStateOf(CategoryIcons.resolve(category?.iconKey ?: "other")) }
    val context = LocalContext.current
    val preferences = remember(context) { context.applicationContext.getSharedPreferences("appearance", 0) }
    var showLabels by remember(preferences) { mutableStateOf(preferences.all["category_icon_labels"] as? Boolean ?: true) }
    var error by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val labels = stringArrayResource(R.array.category_icon_names)
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (category == null) R.string.profile_new_category else R.string.category_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it; error = false }, Modifier.fillMaxWidth().testTag("category_editor_name"),
                    label = { Text(stringResource(R.string.profile_new_category)) }, isError = error, singleLine = true)
                if (error) Text(stringResource(R.string.category_invalid_name), color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.category_choose_icon))
                Text(stringResource(R.string.category_icon_hint), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().testTag("category_icon_labels").toggleable(
                    value = showLabels, role = Role.Switch, onValueChange = {
                        showLabels = it
                        preferences.edit().putBoolean("category_icon_labels", it).apply()
                    }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.category_icon_labels), Modifier.weight(1f))
                    Switch(checked = showLabels, onCheckedChange = null)
                }
                CategoryIcons.keys.chunked(if (showLabels) 2 else 4).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { key ->
                            val label = labels[CategoryIcons.keys.indexOf(key)]
                            val selected = icon == key
                            Surface(modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("category_icon_$key")
                                .selectable(selected, role = Role.RadioButton, onClick = { icon = key })
                                .semantics { contentDescription = label },
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                                    CategoryIcon(key)
                                    if (showLabels) Text(label, Modifier.clearAndSetSemantics { testTag = "category_icon_label_$key" },
                                        style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), modifier = Modifier.testTag("category_editor_save"), onClick = {
            scope.launch { busy = true; if (onSave(name, icon)) onDismiss() else error = true; busy = false }
        }) { Text(stringResource(R.string.profile_save)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.operation_cancel)) } })
}
