package com.personalfinance.tracker.ui.home

enum class HomeBlock(val key: String) { SUMMARY("summary"), QUICK_ACTIONS("quick_actions"), GOAL("goal"), LATEST("latest"), EXPENSES("expenses") }
data class HomeBlockSetting(val id: HomeBlock, val visible: Boolean)
fun normalizeHomeLayout(saved: List<HomeBlockSetting>): List<HomeBlockSetting> =
    saved.distinctBy { it.id } + HomeBlock.entries.filter { id -> saved.none { it.id == id } }
        .map { HomeBlockSetting(it, true) }
fun moveHomeBlock(items: List<HomeBlockSetting>, id: HomeBlock, toIndex: Int): List<HomeBlockSetting> {
    val source = items.indexOfFirst { it.id == id }
    if (source < 0) return items
    return items.toMutableList().apply { val item = removeAt(source); add(toIndex.coerceIn(0, size), item) }
}
