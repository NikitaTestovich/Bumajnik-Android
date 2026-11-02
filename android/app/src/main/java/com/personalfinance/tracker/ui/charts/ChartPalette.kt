package com.personalfinance.tracker.ui.charts

fun chartColorIndices(count: Int, paletteSize: Int): List<Int> {
    require(count >= 0 && paletteSize >= 3)
    return List(count) { index ->
        if (index == count - 1 && count > 1 && index % paletteSize == 0) 1 else index % paletteSize
    }
}
