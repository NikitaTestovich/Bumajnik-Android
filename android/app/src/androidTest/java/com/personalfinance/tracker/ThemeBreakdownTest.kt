package com.personalfinance.tracker
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.personalfinance.tracker.ui.theme.*
import com.personalfinance.tracker.ui.home.MonthlyExpensesWidget
import com.personalfinance.tracker.ui.charts.ExpenseSlice
import org.junit.*
class ThemeBreakdownTest {
 @get:Rule val ui = createComposeRule()
 @Before fun emulatorOnly() { Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu","goldfish")) }
 @Test fun analyticsAndOtherThemesShowRingAndSelectableCards() {
  val theme=mutableStateOf(AppThemeRegistry.resolve(9))
  ui.setContent { FinanceTheme(theme.value) { Column(Modifier.verticalScroll(rememberScrollState())) { MonthlyExpensesWidget(listOf(ExpenseSlice("food","Food","food",7500,.75),ExpenseSlice("travel","Travel","transport",2500,.25)),"USD","en") } } }
  ui.onNodeWithTag("expense_ring").assertExists()
  ui.onNodeWithTag("expense_slice_food").performScrollTo().performClick().assertIsSelected()
  ui.onNodeWithTag("expense_center_name").assertTextEquals("Food")
  ui.onNodeWithTag("expense_slice_food").performScrollTo().performClick().assertIsNotSelected()
  listOf(2,6,8).forEach { id ->
   ui.runOnIdle { theme.value=AppThemeRegistry.resolve(id) }
   ui.onNodeWithTag("expense_ring").assertExists()
   ui.onNodeWithTag("expense_slice_travel").performScrollTo().performClick().assertIsSelected()
   ui.onNodeWithTag("expense_center_name").assertTextEquals("Travel")
   ui.onNodeWithTag("expense_slice_travel").performScrollTo().performClick().assertIsNotSelected()
  }
 }
}
