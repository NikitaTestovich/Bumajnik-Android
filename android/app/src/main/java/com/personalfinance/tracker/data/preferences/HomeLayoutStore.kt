package com.personalfinance.tracker.data.preferences

import android.content.Context
import com.personalfinance.tracker.ui.home.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One atomic preference value; unknown future entries survive saves by older clients. */
class HomeLayoutStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("home_layout", Context.MODE_PRIVATE)
    private fun tokens() = preferences.getString("blocks", "").orEmpty().split(';').filter { it.isNotEmpty() }
    private fun read() = normalizeHomeLayout(tokens().mapNotNull { token ->
        val parts = token.split(':')
        HomeBlock.entries.find { it.key == parts.first() }?.let { HomeBlockSetting(it, parts.getOrNull(1) != "0") }
    })
    private val mutableLayout = MutableStateFlow(read())
    val layout = mutableLayout.asStateFlow()
    private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "blocks") mutableLayout.value = read()
    }
    init { preferences.registerOnSharedPreferenceChangeListener(listener) }
    fun close() { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    fun save(items: List<HomeBlockSetting>) {
        val normalized = normalizeHomeLayout(items)
        val unknown = tokens().filter { token -> HomeBlock.entries.none { it.key == token.substringBefore(':') } }
        preferences.edit().putString("blocks", (normalized.map { "${it.id.key}:${if (it.visible) 1 else 0}" } + unknown).joinToString(";")).apply()
        mutableLayout.value = normalized
    }
    fun reset() = save(normalizeHomeLayout(emptyList()))
}
