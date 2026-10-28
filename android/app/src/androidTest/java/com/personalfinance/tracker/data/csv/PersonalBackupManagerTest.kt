package com.personalfinance.tracker.data.csv

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.repository.RoomFinanceRepository
import com.personalfinance.tracker.data.repository.RoomFamilyRepository
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonalBackupManagerTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomFinanceRepository
    private lateinit var manager: PersonalBackupManager
    private lateinit var file: File
    private val id1 = "00000000-0000-0000-0000-000000000001"
    private val id2 = "00000000-0000-0000-0000-000000000002"
    private val header = "owner,record_id,date,category,income,expense,currency\r\n"

    @Before fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomFinanceRepository(database)
        manager = PersonalBackupManager(repository,
            { uri -> context.contentResolver.openInputStream(uri)!!.use { it.readBytes() } },
            { uri, bytes -> context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) } },
        )
        repository.saveProfile(UserProfile("Alex", "en", "USD", true))
        file = File.createTempFile("personal-backup", ".csv", context.cacheDir)
    }

    @After fun tearDown() { database.close(); file.delete() }

    @Test fun restoreKeepsArchivedCategoryAndAvoidsRenamedIdCollision() = runBlocking {
        val archived = Category("archived", "Food", "Food", true, true, "dining")
        repository.saveCategory(archived)
        assertTrue(manager.restoreBytes((header + row(id1, "Food")).toByteArray()).errors.isEmpty())
        assertEquals(archived, repository.observeCategories().first().single())
        assertEquals(archived.id, repository.observeTransactions(null).first().single().categoryId)
        val generated = com.personalfinance.tracker.data.repository.restoredCategory("Travel", emptyList())
        repository.saveCategory(generated.copy(nameRu = "Renamed", nameEn = "Renamed"))
        assertTrue(manager.restoreBytes((header + row(id2, "Travel")).toByteArray()).errors.isEmpty())
        val restored = repository.observeTransactions(null).first().first { it.id == id2 }
        assertNotEquals(generated.id, restored.categoryId)
        assertEquals("Renamed", repository.observeCategories().first().first { it.id == generated.id }.nameEn)
    }

    @Test fun repositoriesRejectInvalidCommentsBeforeWriting() = runBlocking {
        val income = FinanceTransaction(id1, "Demo", TransactionType.INCOME, LocalDate.of(2026,10,1), Money(100,"USD"), null, comment = "😀".repeat(151))
        assertTrue(runCatching { repository.saveTransaction(income) }.isFailure)
        assertTrue(runCatching { repository.restorePersonalTransactions(listOf(com.personalfinance.tracker.data.repository.PersonalRestoreRecord(income, null))) }.isFailure)
        val family = RoomFamilyRepository(database)
        assertTrue(runCatching { family.saveImportBatch(listOf(FamilyTransaction(id = id1, owner = "Demo", type = TransactionType.INCOME, date = income.date, sourceMoney = income.money, categoryName = null, importRate = null, comment = income.comment))) }.isFailure)
        assertTrue(repository.observeTransactions(null).first().isEmpty())
    }

    @Test fun malformedFileLeavesRoomUnchanged() = runBlocking {
        val result = manager.restoreBytes((header + row(id1) + "Alex,$id2,invalid,,1.00,,USD\r\n").toByteArray())
        assertTrue(result.errors.isNotEmpty())
        assertTrue(repository.observeTransactions(null).first().isEmpty())
        assertTrue(repository.observeCategories().first().isEmpty())
    }

    @Test fun repeatedRestoreMergesWithoutReplacingAndPreservesCategories() = runBlocking {
        repository.saveCategory(Category("food", "Еда", "Food", false))
        val local = FinanceTransaction(id1, "Alex", TransactionType.INCOME, LocalDate.of(2026, 1, 1), Money(900, "EUR"), null)
        repository.saveTransaction(local)
        val bytes = (header + row(id1) + row(id2, "Кофе") + row(id2, "Кофе")).toByteArray()
        assertEquals(BackupRestoreResult(1, 2, emptyList()), manager.restoreBytes(bytes))
        assertEquals(local, repository.observeTransactions(null).first().single { it.id == id1 })
        val custom = repository.observeCategories().first().single { it.isCustom }
        assertEquals(custom.id, repository.observeTransactions(null).first().single { it.id == id2 }.categoryId)
        assertEquals(BackupRestoreResult(0, 3, emptyList()), manager.restoreBytes(bytes))
        assertEquals(2, repository.observeCategories().first().size)
    }

    @Test fun writeFailureRollsBackTransactionsAndNewCategories() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_restore BEFORE INSERT ON finance_transactions WHEN NEW.id = '$id2' BEGIN SELECT RAISE(ABORT, 'write failure'); END",
        )
        val result = manager.restoreBytes((header + row(id1, "New category") + row(id2, "Other category")).toByteArray())
        assertTrue(result.errors.isNotEmpty())
        assertEquals(0, result.addedCount)
        assertTrue(repository.observeTransactions(null).first().isEmpty())
        assertTrue(repository.observeCategories().first().isEmpty())
    }

    @Test fun uriBackupExcludesFamilyAndRestoreDoesNotChangeFamilyProfileOrGoal() = runBlocking {
        val family = RoomFamilyRepository(database)
        val familyRow = FamilyTransaction(id2, "Family", Money(100, "USD"), LocalDate.of(2026, 1, 1), "Food", TransactionType.EXPENSE, BigDecimal.ONE)
        family.saveImportBatch(listOf(familyRow))
        val goal = SavingsGoal(Money(1000, "USD"), Money(200, "USD"))
        repository.saveGoal(goal)
        repository.saveCategory(Category("food", "Еда", "Food", false))
        manager.restoreBytes((header + row(id1)).toByteArray())
        // Exercise actual content-resolver I/O as well as both URI entry points.
        val uri = Uri.fromFile(file)
        assertTrue(manager.export(uri).success)
        val exported = FinanceCsvCodec.decode(file.readBytes())
        assertEquals(listOf(id1), exported.records.map { it.recordId })
        assertEquals("Food", exported.records.single().categoryName)
        assertEquals("food", repository.observeTransactions(null).first().single().categoryId)
        assertEquals(BackupRestoreResult(0, 1, emptyList()), manager.restore(uri))
        assertEquals(listOf(familyRow), family.observeFamilyTransactions(PeriodRange(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1))).first())
        assertEquals(UserProfile("Alex", "en", "USD", true), repository.observeProfile().first())
        assertEquals(goal, repository.observeGoal().first())
    }

    private fun row(id: String, category: String = "Food") = "Alex,$id,2026-10-01,$category,,1.00,USD\r\n"
}
