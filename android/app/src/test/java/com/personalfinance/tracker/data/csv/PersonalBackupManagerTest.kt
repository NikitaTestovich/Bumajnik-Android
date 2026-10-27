package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.domain.money.Money
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PersonalBackupManagerTest {
    private val repository = FakeFinanceRepository()
    private val manager = PersonalBackupManager(repository, { error("No URI reads in byte tests") }, { _, _ -> })
    private val id1 = "00000000-0000-0000-0000-000000000001"
    private val id2 = "00000000-0000-0000-0000-000000000002"
    private val id3 = "00000000-0000-0000-0000-000000000003"
    private val header = "owner,record_id,date,category,income,expense,currency\r\n"

    // Catches streaming writes before validation reaches a later malformed record.
    @Test fun malformedFileRejectsEveryRow() = runBlocking {
        val result = manager.restoreBytes((header + row(id1) + "Alex,$id2,bad,,1.00,,USD\r\n").toByteArray())
        assertTrue(result.errors.isNotEmpty())
        assertEquals(0, result.addedCount)
        assertTrue(repository.observeTransactions(null).first().isEmpty())
        assertTrue(repository.observeCategories().first().isEmpty())
    }

    // Catches UTF-8 replacement characters silently changing restored owner/category labels.
    @Test fun corruptUtf8RejectsWholeBackup() = runBlocking {
        val corruptOwner = byteArrayOf(0xC3.toByte(), 0x28)
        val bytes = (header + row(id1)).toByteArray() + corruptOwner + ",$id2,2026-10-01,Food,,1.00,USD\r\n".toByteArray()
        val result = manager.restoreBytes(bytes)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(repository.observeTransactions(null).first().isEmpty())
    }

    // Catches replacing existing IDs, dropping local rows, or double counting within a file.
    @Test fun restoreMergesAndSkipsExistingAndRepeatedIds() = runBlocking {
        val local = transaction(id1, 900)
        repository.saveTransaction(local)
        val bytes = (header + row(id1) + row(id2) + row(id2)).toByteArray()
        val result = manager.restoreBytes(bytes)
        assertEquals(BackupRestoreResult(1, 2, emptyList()), result)
        assertEquals(2, repository.observeTransactions(null).first().size)
        assertEquals(local.copy(createdOrder = 1), repository.observeTransactions(null).first().single { it.id == id1 })
        assertEquals(BackupRestoreResult(0, 3, emptyList()), manager.restoreBytes(bytes))
    }

    // Catches loss of category labels or creation of a category per restored transaction.
    @Test fun restoreReusesLocalizedCategoriesAndStableCustomCategories() = runBlocking {
        repository.saveCategory(Category("food", "Еда", "Food", false))
        val bytes = (header + row(id1, "Еда") + row(id2, "Кофе")).toByteArray()
        assertTrue(manager.restoreBytes(bytes).errors.isEmpty())
        val custom = repository.observeCategories().first().single { it.isCustom }
        assertEquals("Кофе", custom.nameRu ?: custom.nameEn)
        val transactions = repository.observeTransactions(null).first()
        assertEquals("food", transactions.single { it.id == id1 }.categoryId)
        assertEquals(custom.id, transactions.single { it.id == id2 }.categoryId)
        manager.restoreBytes((header + row(id3, "Кофе")).toByteArray())
        assertEquals(2, repository.observeCategories().first().size)
        assertEquals(custom.id, repository.observeTransactions(null).first().single { it.id == id3 }.categoryId)
    }

    // Catches exporting profile/settings or losing category text/original money.
    @Test fun backupContainsAllPersonalTransactionsWithLabelsOnly() = runBlocking {
        repository.saveProfile(UserProfile("Alex", "ru", "EUR", true))
        repository.saveCategory(Category("food", "Еда", "Food", false))
        repository.saveTransaction(transaction(id1, 1200).copy(categoryId = "food"))
        val decoded = FinanceCsvCodec.decode(manager.backupBytes())
        assertTrue(decoded.errors.isEmpty())
        assertEquals(1, decoded.records.size)
        assertEquals("Еда", decoded.records.single().categoryName)
        assertEquals(Money(1200, "USD"), decoded.records.single().amount)
        assertEquals(id1, decoded.records.single().recordId)
    }

    private fun row(id: String, category: String = "Food") = "Alex,$id,2026-10-01,$category,,1.00,USD\r\n"
    private fun transaction(id: String, amount: Long) = FinanceTransaction(
        id, "Alex", TransactionType.EXPENSE, LocalDate.of(2026, 10, 1), Money(amount, "USD"), null,
    )
}
