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

    @Test fun longValuesAndRussianAndEnglishLabelsStayWithinNarrowWidth() {
        ui.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.6f),
            ) {
                FinanceTheme(AppThemeRegistry.resolve(2)) {
                    val groups = listOf("Доходы", "Expenses").map { group ->
                        FinanceBarGroup(group, listOf(FinanceBar("Alexander Александров", 987654321012345, financeColors.income)))
                    }
                    FinanceCharts.GroupedColumnChart(groups, Modifier.width(200.dp).testTag("narrow_chart"))
                }
            }
        }
        val bounds = ui.onNodeWithTag("narrow_chart").fetchSemanticsNode().boundsInRoot
        listOf("Доходы", "Expenses").forEach { group ->
            val bar = ui.onNodeWithContentDescription("$group, Alexander Александров: 9876543210123.45").fetchSemanticsNode().boundsInRoot
            assertTrue("$group exceeds chart width", bar.left >= bounds.left && bar.right <= bounds.right)
            assertTrue("$group has no visible width", bar.width > 0f)
        }
    }
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
