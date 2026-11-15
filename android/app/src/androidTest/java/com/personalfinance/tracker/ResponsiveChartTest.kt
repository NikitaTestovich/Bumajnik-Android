package com.personalfinance.tracker

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.ui.charts.*
import com.personalfinance.tracker.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResponsiveChartTest {
    @get:Rule val ui = createComposeRule()

    @Test fun allFamilyValuesFitWithoutHorizontalScrolling() {
        ui.setContent {
            FinanceTheme(AppThemeRegistry.resolve(2)) {
                val groups = listOf("Income", "Expense").map { group ->
                    FinanceBarGroup(group, listOf("Anna", "Boris", "Clara").map {
                        FinanceBar(it, 12345, financeColors.income)
                    })
                }
                FinanceCharts.GroupedColumnChart(groups, Modifier.width(240.dp).testTag("chart_bounds"))
            }
        }
        val bounds = ui.onNodeWithTag("chart_bounds").fetchSemanticsNode().boundsInRoot
        listOf("Income", "Expense").forEach { group ->
            listOf("Anna", "Boris", "Clara").forEach { owner ->
                val bar = ui.onNodeWithContentDescription("$group, $owner: 123.45").fetchSemanticsNode().boundsInRoot
                assertTrue("$group $owner is clipped", bar.left >= bounds.left && bar.right <= bounds.right)
                assertTrue("$group $owner has no visible width", bar.width > 0f)
            }
        }
    }
}
