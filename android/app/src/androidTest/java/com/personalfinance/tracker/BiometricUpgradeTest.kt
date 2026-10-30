package com.personalfinance.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.pm.PackageInfoCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.security.LockResult
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit emulator-only seed on 1.15, install -r 1.17, verify, force-stop, verify again. */
@RunWith(AndroidJUnit4::class)
class BiometricUpgradeTest {
    @Test fun enrolledFingerprintAuthenticatesRetainedUpgradeProtection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("emulatorFingerDriver") == "true")
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = instrumentation.targetContext.applicationContext as FinanceApplication
        assertTrue(app.appLock.state.value.enabled)
        assertTrue(app.appLock.state.value.biometrics)
        assertTrue(app.appLock.state.value.locked)
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertTrue("Enrolled fingerprint required", it.canUseBiometrics()) }
            val deadline = android.os.SystemClock.elapsedRealtime() + 15000
            while (app.appLock.biometricEntry.phase.value != com.personalfinance.tracker.security.BiometricEntryPolicy.Phase.PROMPT_ACTIVE && android.os.SystemClock.elapsedRealtime() < deadline) android.os.SystemClock.sleep(100)
            assertEquals(com.personalfinance.tracker.security.BiometricEntryPolicy.Phase.PROMPT_ACTIVE, app.appLock.biometricEntry.phase.value)
            android.os.SystemClock.sleep(1000)
            instrumentation.sendStatus(2, android.os.Bundle().apply { putString("stream", "BIOMETRIC_FINGER:1\n") })
            while (app.appLock.state.value.locked && android.os.SystemClock.elapsedRealtime() < deadline) android.os.SystemClock.sleep(100)
            assertFalse("Actual platform fingerprint must unlock upgraded app", app.appLock.state.value.locked)
            assertTrue(app.appLock.state.value.biometrics)
        }
    }
    @Test fun biometricPreferenceAndPopulatedDataSurviveUpdate() = runBlocking {
        val phase = InstrumentationRegistry.getArguments().getString("biometricUpgradePhase")
        assumeTrue("Run explicitly on disposable emulator", phase in listOf("seed", "verify"))
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) { "Disposable emulator required" }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
        val repo = app.container.financeRepository
        val profile = UserProfile("Upgrade Biometric Demo", "en", "USD", true)
        val transaction = FinanceTransaction("77777777-7777-4777-8777-777777777777", profile.name,
            TransactionType.INCOME, LocalDate.of(2026, 10, 1), Money(123456, "USD"), null, createdOrder = 1)
        val goal = SavingsGoal(Money(1000000, "USD"), Money(123456, "USD"))
        val standardCategory = Category("groceries", "Продукты", "Groceries", false, iconKey = "groceries")
        val category = Category("release-hobby", "Хобби", "Hobbies", true, iconKey = "home")
        val expense = FinanceTransaction("77777777-7777-4777-8777-777777777778", profile.name, TransactionType.EXPENSE, transaction.date, Money(4567, "USD"), category.id, createdOrder = 2)
        val range = PeriodRange(transaction.date, transaction.date.plusMonths(1))
        val family = FamilyTransaction("66666666-6666-4666-8666-666666666666", "Demo Sam", Money(4500, "EUR"), transaction.date, null, TransactionType.INCOME, null)
        val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
        if (phase == "seed") {
            assertEquals("1.15", packageInfo.versionName)
            assertEquals(3L, PackageInfoCompat.getLongVersionCode(packageInfo))
            app.appLock.recoverAfterDeviceCredential(app.appLock.generation)
            app.container.database.clearAllTables()
            repo.saveProfile(profile)
            repo.saveTransaction(transaction)
            repo.saveGoal(goal)
            repo.saveCategory(standardCategory)
            repo.saveCategory(category)
            repo.saveTransaction(expense)
            app.container.familyRepository.saveImportBatch(listOf(family))
            app.container.rateRepository.saveRates(range, "USD", mapOf("EUR" to java.math.BigDecimal("1.1")))
            app.getSharedPreferences("appearance", 0).edit().clear().commit()
            app.getSharedPreferences("operations", 0).edit().clear().commit()
            assertEquals(LockResult.SUCCESS, app.appLock.enable("Ab12", true, true))
        } else {
            assertEquals("1.17", packageInfo.versionName)
            assertEquals(4L, PackageInfoCompat.getLongVersionCode(packageInfo))
            assertTrue("New process must lock", app.appLock.state.value.locked)
        }
        assertTrue(app.appLock.state.value.enabled)
        assertTrue(app.appLock.state.value.biometrics)
        assertEquals(profile, repo.observeProfile().first())
        assertEquals(setOf(transaction, expense), repo.observeTransactions(null).first().toSet())
        assertEquals(goal, repo.observeGoal().first())
        if (phase == "verify") {
            assertEquals(standardCategory, repo.observeCategories().first().single { it.id == standardCategory.id })
            assertEquals(category, repo.observeCategories().first().single { it.id == category.id })
            assertEquals(listOf(family), app.container.familyRepository.observeFamilyTransactions(range).first())
            assertEquals(java.math.BigDecimal("1.1"), app.container.rateRepository.ratesFor(range, "USD").first()["EUR"])
            val appearance = app.getSharedPreferences("appearance", 0)
            if (!appearance.contains("release_verified")) {
                assertEquals(2, app.themeStore.theme.value.id)
                assertEquals(TransactionType.EXPENSE, app.operationsTabStore.selectedType.value)
                assertTrue(appearance.getBoolean("category_icon_labels", true))
                app.themeStore.select(9)
                app.operationsTabStore.select(TransactionType.INCOME)
                appearance.edit().putBoolean("category_icon_labels", false).putBoolean("release_verified", true).commit()
            } else {
                assertEquals(9, app.themeStore.theme.value.id)
                assertEquals(TransactionType.INCOME, app.operationsTabStore.selectedType.value)
                assertFalse(appearance.getBoolean("category_icon_labels", true))
            }
            assertEquals(LockResult.WRONG_CODE, app.appLock.unlock("0000"))
            assertTrue(app.appLock.state.value.locked)
            assertEquals(LockResult.SUCCESS, app.appLock.unlock("Ab12"))
            assertTrue(app.appLock.state.value.biometrics)
        }
    }
}
