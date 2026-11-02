package com.personalfinance.tracker.ui.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.personalfinance.tracker.ui.theme.FinanceButton as Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.personalfinance.tracker.domain.model.IncomeComment
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.R
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import com.personalfinance.tracker.ui.common.DatePickerField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: String?,
    type: TransactionType,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as FinanceApplication).container.financeRepository }
    val viewModel = remember(repository) { TransactionFormViewModel(repository) }
    val profile by repository.observeProfile().collectAsState(initial = null)
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var draft by remember(transactionId, type) {
        mutableStateOf(
            TransactionDraft(
                id = transactionId,
                owner = "",
                type = type,
                dateText = FinanceDateFormat.format(java.time.LocalDate.now()),
                amountText = "",
                currencyCode = profile?.baseCurrency ?: "USD",
                categoryId = null,
            ),
        )
    }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var errorResult by remember { mutableStateOf<TransactionSaveResult?>(null) }
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId, type, profile?.name, profile?.baseCurrency) {
        val currentProfile = profile ?: return@LaunchedEffect
        val loaded = viewModel.load(transactionId, type, currentProfile.baseCurrency)
        if (loaded != null) draft = loaded else errorResult = TransactionSaveResult.NotFound
        isReady = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(
                    if (transactionId == null) R.string.operation_new_title else R.string.operation_edit_title,
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
            DatePickerField(
                value = draft.dateText,
                onValueChange = { draft = draft.copy(dateText = it); errorResult = null },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.operation_date),
                supportingText = stringResource(R.string.operation_date_format),
            )
            OutlinedTextField(
                value = draft.amountText,
                onValueChange = { draft = draft.copy(amountText = it); errorResult = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.operation_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.currencyCode,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.operation_currency)) },
                readOnly = true,
                singleLine = true,
            )
            run {
                OutlinedTextField(
                    value = draft.comment,
                    onValueChange = { draft = draft.copy(comment = it); errorResult = null },
                    modifier = Modifier.fillMaxWidth().testTag(if (draft.type == TransactionType.INCOME) "income_comment" else "expense_comment"),
                    label = { Text(stringResource(R.string.operation_comment)) },
                    supportingText = { Text("${IncomeComment.length(draft.comment)}/${IncomeComment.MAX_LENGTH}") },
                    isError = !IncomeComment.isValid(draft.comment),
                    minLines = 2,
                )
            }
            if (draft.type == TransactionType.EXPENSE) {
                Text(stringResource(R.string.operation_category), style = MaterialTheme.typography.titleMedium)
                val selected = categories.firstOrNull { it.id == draft.categoryId }
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selected?.categoryLabel(profile?.languageTag ?: "en")
                            ?: stringResource(R.string.operation_select_category),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("expense_category"),
                        label = { Text(stringResource(R.string.operation_category)) },
                        leadingIcon = { selected?.let { com.personalfinance.tracker.ui.categories.CategoryIcon(it.iconKey) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        categories.filter { !it.isArchived || (transactionId != null && it.id == draft.categoryId) }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.categoryLabel(profile?.languageTag ?: "en")) },
                                leadingIcon = { com.personalfinance.tracker.ui.categories.CategoryIcon(category.iconKey) },
                                onClick = {
                                    draft = draft.copy(categoryId = category.id)
                                    errorResult = null
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Text(stringResource(R.string.operation_category_profile_hint), style = MaterialTheme.typography.bodySmall)
                if (categories.none { !it.isArchived }) Text(stringResource(R.string.category_none_active), color = MaterialTheme.colorScheme.error)
            }
            errorResult?.let { result ->
                val message = when (result) {
                    TransactionSaveResult.CategoryRequired -> R.string.operation_error_category
                    TransactionSaveResult.InvalidDate -> R.string.operation_error_date
                    TransactionSaveResult.InvalidAmount -> R.string.operation_error_amount
                    TransactionSaveResult.InvalidCurrency -> R.string.operation_error_currency
                    TransactionSaveResult.InvalidComment -> R.string.operation_error_comment
                    TransactionSaveResult.NotFound -> R.string.operation_error_not_found
                    TransactionSaveResult.Saved -> return@let
                }
                Text(stringResource(message), color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.operation_cancel))
                }
                Button(
                    onClick = {
                        scope.launch {
                            val result = viewModel.save(
                                transactionId = transactionId,
                                type = draft.type,
                                owner = draft.owner.ifBlank { profile?.name.orEmpty() },
                                dateText = draft.dateText,
                                amountText = draft.amountText,
                                currencyCode = draft.currencyCode,
                                categoryId = draft.categoryId,
                                comment = draft.comment,
                            )
                            withContext(Dispatchers.Main.immediate) {
                                if (result == TransactionSaveResult.Saved) onSaved() else errorResult = result
                            }
                        }
                    },
                    enabled = isReady && profile != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.operation_save))
                }
            }
        }
    }
}

private fun Category.categoryLabel(languageTag: String): String =
    if (languageTag == "ru") nameRu ?: nameEn.orEmpty() else nameEn ?: nameRu.orEmpty()
