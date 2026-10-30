package com.personalfinance.tracker.data.preferences

import android.content.Context
import com.personalfinance.tracker.ui.theme.AppThemeRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Dedicated local preference, intentionally excluded from financial backups. */
class ThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private fun read() = AppThemeRegistry.resolve((prefs.all["theme_id"] as? Int))
    private val mutableTheme = MutableStateFlow(read())
    val theme = mutableTheme.asStateFlow()
    fun select(id: Int) {
        val selected = AppThemeRegistry.resolve(id)
        prefs.edit().putInt("theme_id", selected.id).apply()
        mutableTheme.value = selected
    }
}
