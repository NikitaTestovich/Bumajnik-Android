package com.personalfinance.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.security.LockResult
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run only on a disposable emulator: seed on 1.17, install 1.20 over it, then verify. */
@RunWith(AndroidJUnit4::class)
class Upgrade117To120Test {
    @Test fun retainFinancialDataAndPreferencesAcrossUpgrade() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val phase = InstrumentationRegistry.getArguments().getString("upgrade117Phase")
        val testBiometric = InstrumentationRegistry.getArguments().getString("upgradeBiometric") == "true"
        assumeTrue(phase == "seed" || phase == "verify")
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = instrumentation.targetContext.applicationContext as FinanceApplication
        val repository = app.container.financeRepository
        val date = LocalDate.of(2026, 10, 1)
        val range = PeriodRange(date, date.plusMonths(1))
        if (phase == "seed") {
            app.container.database.clearAllTables()
            app.appLock.recoverAfterDeviceCredential(app.appLock.generation)
            repository.saveProfile(UserProfile("Upgrade Demo", "en", "BYN", true))
            repository.saveCategory(Category("groceries", "Продукты", "Groceries", false, iconKey = "groceries"))
            repository.saveTransaction(FinanceTransaction("11111111-1111-4111-8111-111111111111", "Upgrade Demo", TransactionType.EXPENSE, date, Money(12345, "BYN"), "groceries"))
            repository.saveGoal(SavingsGoal(Money(500000, "BYN"), Money(20000, "BYN")))
            app.container.familyRepository.saveImportBatch(listOf(FamilyTransaction("22222222-2222-4222-8222-222222222222", "Family Demo", Money(1000, "USD"), date, "Groceries", TransactionType.EXPENSE, BigDecimal("3"), "BYN")))
            app.container.rateRepository.saveRates(range, "BYN", mapOf("USD" to BigDecimal("3")))
            app.themeStore.select(8)
            app.operationsTabStore.select(TransactionType.INCOME)
            assertTrue(app.getSharedPreferences("home_layout", 0).edit().putString("blocks", "summary:1;goal:0").commit())
            assertTrue(app.getSharedPreferences("appearance", 0).edit().putBoolean("category_icon_labels", false).commit())
            assertEquals(LockResult.SUCCESS, app.appLock.enable("Ab12", testBiometric, true))
        } else {
            assertEquals("Upgrade Demo", repository.observeProfile().first()?.name)
            assertEquals(12345L, repository.observeTransactions(null).first().single().money.minorUnits)
            assertEquals("groceries", repository.observeCategories().first().single().iconKey)
            assertEquals(20000L, repository.observeGoal().first()?.saved?.minorUnits)
            assertEquals("Family Demo", app.container.familyRepository.observeFamilyTransactions(range).first().single().owner)
            assertEquals(BigDecimal("3"), app.container.rateRepository.ratesFor(range, "BYN").first()["USD"])
            assertEquals(8, app.themeStore.theme.value.id)
            assertEquals(TransactionType.INCOME, app.operationsTabStore.selectedType.value)
            assertEquals("summary:1;goal:0", app.getSharedPreferences("home_layout", 0).getString("blocks", ""))
            assertFalse(app.getSharedPreferences("appearance", 0).getBoolean("category_icon_labels", true))
            assertTrue(app.appLock.state.value.enabled)
            assertEquals(testBiometric, app.appLock.state.value.biometrics)
            assertEquals(LockResult.SUCCESS, app.appLock.unlock("Ab12"))
        }
    }
}
