package com.personalfinance.tracker.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullBackupCaptureTest {
    @Test fun legacyFamilyIdentifiersSurviveDatabaseBackupRestore() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val sample = FamilyTransactionEntity("1-1-1-1-1", "Family", "EXPENSE", "2026-10-01", 200, "USD", "Food", null, "USD", null, "Tea")
            val lower = sample.copy(id = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
            val rows = listOf(sample, lower, lower.copy(id = lower.id.uppercase()))
            db.familyDao().insertFamilyTransactions(rows)
            val snapshot = captureFullBackup(db)
            val decoded = FullBackupCodec.decode(FullBackupCodec.encode(snapshot))
            restoreFullBackup(db, decoded)
            assertEquals(rows.map { it.id }.toSet(), captureFullBackup(db).familyTransactions.map { it.id }.toSet())
        } finally { db.close() }
    }

    @Test fun capturesProfileTransactionsFamilyCategoriesRatesAndGoal() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            db.financeDao().saveProfile(UserProfileEntity(name = "Demo", languageTag = "en", baseCurrency = "USD", onboardingComplete = true))
            db.financeDao().saveCategory(CategoryEntity("food", "Продукты", "Food", false, iconKey = "groceries"))
            db.financeDao().saveTransaction(FinanceTransactionEntity("11111111-1111-1111-1111-111111111111", "Demo", "EXPENSE", "2026-10-01", 100, "USD", "food", "Milk"))
            db.familyDao().insertFamilyTransactions(listOf(FamilyTransactionEntity("22222222-2222-2222-2222-222222222222", "Family", "EXPENSE", "2026-10-01", 200, "USD", "Food", null, "USD", null, "Tea")))
            db.exchangeRateDao().insertRates(listOf(ExchangeRateEntity("2026-10-01", "2026-11-01", "USD", "EUR", "1.2")))
            db.financeDao().saveGoal(SavingsGoalEntity(targetMinorUnits = 1000, targetCurrency = "USD", savedMinorUnits = 200, savedCurrency = "USD"))
            val snapshot = captureFullBackup(db)
            validateFullBackup(snapshot)
            val preferences = BackupPreferences(8, "INCOME", "summary:1;goal:0", false)
            val file = FullBackupFile.encode(snapshot, preferences)
            val (decoded, decodedPreferences) = FullBackupFile.decode(file)
            assertEquals(preferences, decodedPreferences)
            assertEquals(snapshot, decoded)
            assertEquals("Demo", snapshot.profile?.name)
            assertEquals("Milk", snapshot.transactions.single().comment)
            assertEquals("Tea", snapshot.familyTransactions.single().comment)
            assertEquals("groceries", snapshot.categories.single().iconKey)
            assertEquals("1.2", snapshot.rates.single().rateToBase)
            assertEquals(200L, snapshot.goal?.savedMinorUnits)
            db.financeDao().saveProfile(UserProfileEntity(name = "Changed", languageTag = "en", baseCurrency = "USD", onboardingComplete = true))
            val invalid = snapshot.copy(transactions = snapshot.transactions + snapshot.transactions.single())
            assertTrue(runCatching { restoreFullBackup(db, invalid) }.isFailure)
            assertEquals("Changed", captureFullBackup(db).profile?.name)
            restoreFullBackup(db, decoded)
            assertEquals(snapshot, captureFullBackup(db))
        } finally { db.close() }
    }
}
