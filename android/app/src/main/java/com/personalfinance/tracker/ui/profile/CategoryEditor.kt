package com.personalfinance.tracker.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    var name by remember(category?.id) { mutableStateOf(category?.label(language).orEmpty()) }
    var icon by remember(category?.id) { mutableStateOf(CategoryIcons.resolve(category?.iconKey ?: "other")) }
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
                CategoryIcons.keys.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { key ->
                            FilterChip(selected = icon == key, onClick = { icon = key },
                                modifier = Modifier.weight(1f).testTag("category_icon_$key"),
                                leadingIcon = { CategoryIcon(key) }, label = { Text(labels[CategoryIcons.keys.indexOf(key)]) })
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
