package com.personalfinance.tracker.ui.theme

/** Stable local appearance IDs; never derive IDs from list positions. */
data class AppTheme(val id: Int, val nameRes: Int, val dark: Boolean)
class ThemeRegistry(val available: List<AppTheme>, defaultId: Int) {
    val default = available.single { it.id == defaultId }
    init { require(available.map { it.id }.distinct().size == available.size) }
    fun resolve(id: Int?): AppTheme = available.firstOrNull { it.id == id } ?: default
}
val AppThemeRegistry = ThemeRegistry(listOf(
    AppTheme(2, com.personalfinance.tracker.R.string.theme_journal, false),
    AppTheme(6, com.personalfinance.tracker.R.string.theme_graphite, true),
    AppTheme(8, com.personalfinance.tracker.R.string.theme_mosaic, false),
    AppTheme(9, com.personalfinance.tracker.R.string.theme_analytics, true),
    AppTheme(10, com.personalfinance.tracker.R.string.theme_amber, false),
    AppTheme(11, com.personalfinance.tracker.R.string.theme_forest, false),
    AppTheme(12, com.personalfinance.tracker.R.string.theme_coral, false),
), 2)
