package com.personalfinance.tracker.data.preferences

import android.content.Context
import com.personalfinance.tracker.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local navigation preference, independent of financial data and appearance. */
class OperationsTabStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("operations", Context.MODE_PRIVATE)
    private val mutableType = MutableStateFlow(
        TransactionType.entries.firstOrNull { it.name == preferences.all["selected_type"] } ?: TransactionType.EXPENSE,
    )
    val selectedType = mutableType.asStateFlow()

    fun select(type: TransactionType) {
        preferences.edit().putString("selected_type", type.name).apply()
        mutableType.value = type
    }
}
