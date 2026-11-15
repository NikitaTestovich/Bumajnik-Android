package com.personalfinance.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.ui.reports.ReportExpenseDetail
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportExpenseDetailTest {
    @get:Rule val ui = createComposeRule()

    @Test fun dateAndAmountShareRowWithCommentBelow() {
        ui.setContent { ReportExpenseDetail("14-11-2026", "30,00 BYN", "Анализы", null) }
        val date = ui.onNodeWithText("14-11-2026").fetchSemanticsNode().boundsInRoot
        val amount = ui.onNodeWithText("30,00 BYN").fetchSemanticsNode().boundsInRoot
        val comment = ui.onNodeWithText("Анализы").fetchSemanticsNode().boundsInRoot
        assertTrue(amount.left > date.left)
        assertTrue(amount.top == date.top)
        assertTrue(comment.top >= date.bottom)
        ui.onNodeWithTag("expense_detail_separator").assertExists()
    }

    @Test fun blankCommentIsNotDisplayedAndOriginalCurrencyRemainsVisible() {
        ui.setContent { ReportExpenseDetail("14-11-2026", "30,00 BYN", "", "10,00 USD") }
        ui.onNodeWithTag("expense_detail_comment").assertDoesNotExist()
        ui.onNodeWithText("10,00 USD").assertExists()
    }

    @Test fun largeAmountDoesNotHideDateOnNarrowScreen() {
        val amount = "92233720368547758.07 BYN"
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                androidx.compose.foundation.layout.Box(Modifier.width(200.dp)) {
                    ReportExpenseDetail("14-11-2026", amount, "", null)
                }
            }
        }
        val date = ui.onNodeWithText("14-11-2026").fetchSemanticsNode().boundsInRoot
        assertTrue("date must remain visible", date.width > 0f)
        ui.onNodeWithText(amount).assertIsDisplayed()
    }
}
