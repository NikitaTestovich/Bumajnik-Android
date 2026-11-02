package com.personalfinance.tracker.ui.charts

import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChartPaletteTest {
    @Test fun adjacentSegmentsIncludingCircularSeamHaveDifferentColors() {
        for (count in 2..40) {
            val indices = chartColorIndices(count, 4)
            for (i in indices.indices) assertNotEquals(indices[i], indices[(i + 1) % count])
        }
    }
}
