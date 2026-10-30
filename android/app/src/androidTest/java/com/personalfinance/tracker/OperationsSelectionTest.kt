package com.personalfinance.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class OperationsSelectionTest {
 @get:Rule val ui = createEmptyComposeRule()
 private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
 private var scenario: ActivityScenario<MainActivity>? = null
 private var prepared = false
 @Before fun prepare() {
  Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
  prepared = true
  runBlocking { app.appLock.recoverAfterDeviceCredential(app.appLock.generation) }
  app.container.database.clearAllTables()
  app.operationsTabStore.select(TransactionType.EXPENSE)
  runBlocking {
   app.container.financeRepository.saveProfile(UserProfile("Operations Demo", "en", "USD", true))
   app.container.financeRepository.saveCategory(Category("groceries", "Продукты", "Groceries", false, iconKey = "groceries"))
  }
  scenario = ActivityScenario.launch(MainActivity::class.java)
  waitText(R.string.navigation_operations)
 }
 @After fun cleanup() {
  if (!prepared) return
  scenario?.close()
  app.operationsTabStore.select(TransactionType.EXPENSE)
  app.container.database.clearAllTables()
 }
 private fun waitText(id: Int) {
  ui.waitUntil(10000) { ui.onAllNodesWithText(app.getString(id)).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
 }
 private fun click(id: Int) { ui.onNodeWithText(app.getString(id)).performClick() }
 private fun selected(id: Int) { ui.onNodeWithText(app.getString(id)).assertIsSelected() }
 @Test fun tabsFillEqualWidthAndSelectionSurvivesNavigationAndRecreation() {
  click(R.string.navigation_operations)
  val expense = ui.onNodeWithText(app.getString(R.string.operation_expenses)).fetchSemanticsNode().boundsInRoot
  val income = ui.onNodeWithText(app.getString(R.string.operation_income)).fetchSemanticsNode().boundsInRoot
  assertEquals(expense.width, income.width, 1f)
  val root = ui.onRoot().fetchSemanticsNode().boundsInRoot
  assertTrue("Tabs span available row", income.right > root.right * .9f)
  click(R.string.operation_income)
  click(R.string.navigation_home)
  click(R.string.navigation_operations)
  selected(R.string.operation_income)
  scenario!!.recreate()
  waitText(R.string.navigation_operations)
  if (ui.onAllNodesWithText(app.getString(R.string.operation_month_total)).fetchSemanticsNodes().isEmpty()) click(R.string.navigation_operations)
  selected(R.string.operation_income)
 }
 @Test fun homeSavesSelectTheirTypeAndCancelPreservesSelection() {
  click(R.string.navigation_operations)
  click(R.string.operation_expenses)
  click(R.string.navigation_home)
  click(R.string.home_add_income)
  waitText(R.string.operation_amount)
  ui.onNodeWithText(app.getString(R.string.operation_cancel)).performScrollTo().performClick()
  click(R.string.navigation_operations)
  selected(R.string.operation_expenses)
  click(R.string.navigation_home)
  saveFromHome(R.string.home_add_income, "10")
  click(R.string.navigation_operations)
  selected(R.string.operation_income)
  click(R.string.navigation_home)
  saveFromHome(R.string.home_add_expense, "3", expense = true)
  click(R.string.navigation_operations)
  selected(R.string.operation_expenses)
  assertEquals(2, runBlocking { app.container.financeRepository.observeTransactions(null).first().size })
 }
 @Test fun operationsAddEditAndCancelRetainCorrectType() {
  click(R.string.navigation_operations)
  for (income in listOf(true, false)) {
   val tab = if (income) R.string.operation_income else R.string.operation_expenses
   click(tab)
   ui.onNodeWithText("＋  ${app.getString(R.string.operation_add)}").performClick()
   saveForm("12", !income)
   selected(tab)
   ui.onNodeWithText(app.getString(R.string.operation_edit)).performClick()
   waitText(R.string.operation_amount)
   ui.onNodeWithText(app.getString(R.string.operation_amount)).performTextReplacement("14")
   ui.onNodeWithText(app.getString(R.string.operation_save)).performScrollTo().performClick()
   waitText(R.string.operation_month_total)
   selected(tab)
   ui.onNodeWithText(app.getString(R.string.operation_edit)).performClick()
   ui.onNodeWithText(app.getString(R.string.operation_cancel)).performScrollTo().performClick()
   selected(tab)
  }
  assertTrue(runBlocking { app.container.financeRepository.observeTransactions(null).first().all { it.money.minorUnits == 1400L } })
 }
 @Test fun systemBackFromHomeIncomeAndExpenseKeepsPreviousSelectionAndRecords() {
  for (income in listOf(true, false)) {
   val previousTab = if (income) R.string.operation_expenses else R.string.operation_income
   click(R.string.navigation_operations)
   click(previousTab)
   val before = records()
   click(R.string.navigation_home)
   click(if (income) R.string.home_add_income else R.string.home_add_expense)
   waitText(R.string.operation_amount)
   ui.onNodeWithText(app.getString(R.string.operation_amount)).performTextInput("93")
   systemBackFromForm()
   waitText(R.string.navigation_operations)
   click(R.string.navigation_operations)
   selected(previousTab)
   assertEquals(before, records())
   click(R.string.navigation_home)
  }
 }
 @Test fun systemBackFromOperationsAddAndEditKeepsPreviousSelectionAndRecords() {
  click(R.string.navigation_operations)
  for (income in listOf(true, false)) {
   val tab = if (income) R.string.operation_income else R.string.operation_expenses
   click(tab)
   ui.onNodeWithText("＋  ${app.getString(R.string.operation_add)}").performClick()
   saveForm("12", !income)
   val before = records()
   ui.onNodeWithText("＋  ${app.getString(R.string.operation_add)}").performClick()
   waitText(R.string.operation_amount)
   ui.onNodeWithText(app.getString(R.string.operation_amount)).performTextInput("93")
   systemBackFromForm()
   waitText(R.string.operation_month_total)
   selected(tab)
   assertEquals(before, records())
   ui.onNodeWithText(app.getString(R.string.operation_edit)).performClick()
   waitText(R.string.operation_amount)
   ui.onNodeWithText(app.getString(R.string.operation_amount)).performTextReplacement("94")
   systemBackFromForm()
   waitText(R.string.operation_month_total)
   selected(tab)
   assertEquals(before, records())
  }
 }
 private fun records() = runBlocking { app.container.financeRepository.observeTransactions(null).first() }
 private fun systemBackFromForm() {
  // Close the IME first: the following real BACK key must pop the form, not just its keyboard.
  androidx.test.espresso.Espresso.closeSoftKeyboard()
  InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
  ui.waitForIdle()
 }
 private fun saveFromHome(id: Int, amount: String, expense: Boolean = false) {
  click(id)
  saveForm(amount, expense)
 }
 private fun saveForm(amount: String, expense: Boolean) {
  waitText(R.string.operation_amount)
  ui.onNodeWithText(app.getString(R.string.operation_amount)).performTextInput(amount)
  if (expense) {
   ui.onNodeWithTag("expense_category").performScrollTo().performClick()
   ui.onNodeWithText("Groceries").performClick()
  }
  ui.onNodeWithText(app.getString(R.string.operation_save)).performScrollTo().performClick()
  waitText(R.string.navigation_operations)
 }
}
