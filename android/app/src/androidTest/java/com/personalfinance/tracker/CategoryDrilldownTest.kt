package com.personalfinance.tracker

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalfinance.tracker.ui.charts.CategoryBreakdownItem
import com.personalfinance.tracker.ui.charts.FinanceCharts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDrilldownTest {
    @get:Rule val ui = createComposeRule()
    @Test fun tappingCategoryOpensAndClosesDetails() {
        ui.setContent {
            var open by remember { mutableStateOf(false) }
            FinanceCharts.CategoryBreakdownBars(listOf(CategoryBreakdownItem("Health", 10000, 1f)),
                onCategoryClick = { open = !open }, details = { if (open) Text("Doctor visit") })
        }
        ui.onNodeWithText("Doctor visit").assertDoesNotExist()
        ui.onNode(hasContentDescription("Health", substring = true) and hasClickAction()).performClick()
        ui.onNodeWithText("Doctor visit").assertExists()
        ui.onNode(hasContentDescription("Health", substring = true) and hasClickAction()).performClick()
        ui.onNodeWithText("Doctor visit").assertDoesNotExist()
    }
}
