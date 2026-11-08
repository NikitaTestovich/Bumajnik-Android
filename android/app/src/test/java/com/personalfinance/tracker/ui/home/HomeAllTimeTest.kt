package com.personalfinance.tracker.ui.home

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.*
import com.personalfinance.tracker.domain.summary.SummaryService
import java.time.LocalDate
import java.math.BigDecimal
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HomeAllTimeTest {
 @Test fun futureDatedEntriesAreIncludedOnlyInOverallBalance() = runBlocking {
  val repo=FakeFinanceRepository(); val rates=TestRates(); val vm=HomeViewModel(repo,ExchangeRateService(rates),SummaryService(repo)){ LocalDate.of(2026,10,10) }
  repo.saveTransaction(FinanceTransaction("future","Demo",TransactionType.INCOME,LocalDate.of(2027,1,1),Money(5000,"USD"),null))
  val state=vm.load("USD")
  assertEquals(5000L,state.allTimeBalance?.minorUnits)
  assertEquals(0L,state.summary?.balance?.minorUnits)
 }
 @Test fun computesAllPersonalRecordsAndReflectsEditsAndDeletion() = runBlocking {
  val repo=FakeFinanceRepository(); val rates=TestRates(); val vm=HomeViewModel(repo,ExchangeRateService(rates),SummaryService(repo)){ LocalDate.of(2026,10,10) }
  assertEquals(0L,vm.load("USD").allTimeBalance?.minorUnits)
  val income=FinanceTransaction("1","Demo",TransactionType.INCOME,LocalDate.of(2025,1,1),Money(10000,"USD"),null)
  val expense=FinanceTransaction("2","Demo",TransactionType.EXPENSE,LocalDate.of(2026,10,5),Money(3000,"USD"),"groceries")
  repo.saveTransaction(income); repo.saveTransaction(expense)
  assertEquals(7000L,vm.load("USD").allTimeBalance?.minorUnits)
  repo.saveTransaction(expense.copy(money=Money(5000,"USD"))); assertEquals(5000L,vm.load("USD").allTimeBalance?.minorUnits)
  repo.deleteTransaction("1"); assertEquals(-5000L,vm.load("USD").allTimeBalance?.minorUnits)
 }
 @Test fun historicalForeignCurrencyRequiresRateWithoutBlockingCurrentMonth() = runBlocking {
  val repo=FakeFinanceRepository(); val rates=TestRates(); val vm=HomeViewModel(repo,ExchangeRateService(rates),SummaryService(repo)){ LocalDate.of(2026,10,10) }
  repo.saveTransaction(FinanceTransaction("1","Demo",TransactionType.INCOME,LocalDate.of(2025,1,1),Money(1000,"EUR"),null))
  val missing=vm.load("USD"); assertNull(missing.allTimeBalance); assertEquals(setOf("EUR"),missing.allTimeMissingCurrencies); assertEquals(0L,missing.summary?.balance?.minorUnits)
  vm.saveRates("USD",mapOf("EUR" to BigDecimal("2")))
  assertEquals(2000L,vm.load("USD").allTimeBalance?.minorUnits)
 }
 private class TestRates:RateRepository {
  val data=MutableStateFlow<Map<String,BigDecimal>>(emptyMap())
  override fun ratesFor(range:PeriodRange,baseCurrency:String):Flow<Map<String,BigDecimal>> = data
  override suspend fun saveRates(range:PeriodRange,baseCurrency:String,rates:Map<String,BigDecimal>){data.value=rates}
 }
}
