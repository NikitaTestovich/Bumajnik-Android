package com.personalfinance.tracker

import android.os.Process
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.preferences.OperationsTabStore
import com.personalfinance.tracker.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Invoke seed and verify separately, with force-stop between them. */
class OperationsProcessRestartTest {
 @Test fun selectedTabSurvivesNewProcess() {
  val phase = InstrumentationRegistry.getArguments().getString("operationsRestartPhase")
  assumeTrue(phase == "seed" || phase == "verify")
  check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
  val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
  val marker = app.getSharedPreferences("synthetic-operations-restart", 0)
  if (phase == "seed") {
   ActivityScenario.launch(MainActivity::class.java).use {
    app.operationsTabStore.select(TransactionType.INCOME)
   }
   assertTrue(marker.edit().putInt("seedPid", Process.myPid()).commit())
  } else {
   try {
    val seedPid = marker.getInt("seedPid", -1)
    assertTrue(seedPid > 0)
    assertNotEquals(seedPid, Process.myPid())
    assertEquals(TransactionType.INCOME, app.operationsTabStore.selectedType.value)
    assertEquals(TransactionType.INCOME, OperationsTabStore(app).selectedType.value)
   } finally {
    ActivityScenario.launch(MainActivity::class.java).use { app.operationsTabStore.select(TransactionType.EXPENSE) }
    marker.edit().clear().commit()
   }
  }
 }
}
