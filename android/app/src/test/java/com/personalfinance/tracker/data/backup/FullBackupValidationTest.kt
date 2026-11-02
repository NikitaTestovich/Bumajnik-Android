package com.personalfinance.tracker.data.backup

import com.personalfinance.tracker.data.local.*
import org.junit.Assert.*
import org.junit.Test

class FullBackupValidationTest {
    private val row = FinanceTransactionEntity("11111111-1111-1111-1111-111111111111", "Demo", "EXPENSE", "2026-10-01", 123, "BYN", "food", "Milk")
    private val snapshot = FullBackupSnapshot(
        profile = UserProfileEntity(name = "Demo", languageTag = "ru", baseCurrency = "BYN", onboardingComplete = true),
        categories = listOf(CategoryEntity("food", "Продукты", "Food", false)),
        transactions = listOf(row), familyTransactions = emptyList(), batches = emptyList(), rates = emptyList(), goal = null,
    )

    @Test fun validSnapshotPasses() { validateFullBackup(snapshot) }
    @Test fun snapshotSerializationPreservesContentsAndRejectsTruncation() {
        val batch = FamilyImportBatchEntity("33333333-3333-3333-3333-333333333333", 42, 1)
        val family = FamilyTransactionEntity("22222222-2222-2222-2222-222222222222", "Partner", "EXPENSE", "2026-10-02", 100, "USD", "Food", "1.5", "BYN", batch.id, "Tea")
        val complete = snapshot.copy(familyTransactions = listOf(family), batches = listOf(batch),
            rates = listOf(ExchangeRateEntity("2026-10-01", "2026-11-01", "BYN", "USD", "3.1")),
            goal = SavingsGoalEntity(targetMinorUnits = 1000, targetCurrency = "BYN", savedMinorUnits = 200, savedCurrency = "BYN"))
        val encoded = FullBackupCodec.encode(complete)
        assertEquals(complete, FullBackupCodec.decode(encoded))
        assertTrue(runCatching { FullBackupCodec.decode(encoded.copyOf(encoded.size - 1)) }.isFailure)
        val prefs = BackupPreferences(8, "INCOME", "summary:1", false)
        val fullFile = FullBackupFile.encode(complete, prefs)
        val (restoredSnapshot, restoredPreferences) = FullBackupFile.decode(fullFile)
        assertEquals(complete, restoredSnapshot)
        assertEquals(prefs, restoredPreferences)
        assertTrue(runCatching { FullBackupFile.decode(fullFile.copyOf(fullFile.size - 1)) }.isFailure)
    }
    @Test fun invalidOrDuplicateTransactionsAreRejected() {
        assertTrue(runCatching { validateFullBackup(snapshot.copy(transactions = listOf(row, row))) }.isFailure)
        assertTrue(runCatching { validateFullBackup(snapshot.copy(transactions = listOf(row.copy(date = "bad")))) }.isFailure)
        assertTrue(runCatching { validateFullBackup(snapshot.copy(transactions = listOf(row.copy(currencyCode = "XYZ")))) }.isFailure)
        assertTrue(runCatching { validateFullBackup(snapshot.copy(transactions = listOf(row.copy(categoryId = "missing")))) }.isFailure)
    }
    @Test fun rejectsBrokenFamilyBatchReferences() {
        val family = FamilyTransactionEntity("22222222-2222-2222-2222-222222222222", "Demo", "EXPENSE", "2026-10-02", 100, "USD", "Food", "1", "BYN", "missing")
        assertTrue(runCatching { validateFullBackup(snapshot.copy(familyTransactions = listOf(family))) }.isFailure)
    }

    @Test fun backupPreservesPreviouslyAcceptedFamilyRecordIdsExactly() {
        val short = FamilyTransactionEntity("1-1-1-1-1", "Demo", "EXPENSE", "2026-10-02", 100, "USD", "Food", "1", "BYN", null)
        val mixed = short.copy(id = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
        val upper = mixed.copy(id = mixed.id.uppercase())
        val existing = snapshot.copy(familyTransactions = listOf(short, mixed, upper))
        assertEquals(existing, FullBackupCodec.decode(FullBackupCodec.encode(existing)))
    }
}
