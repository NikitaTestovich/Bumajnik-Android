package com.personalfinance.tracker.data.csv

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.repository.RoomFamilyRepository
import com.personalfinance.tracker.data.repository.RoomFinanceRepository
import com.personalfinance.tracker.domain.model.FamilyTransaction
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyImportManagerTest {
    private lateinit var database: AppDatabase
    private lateinit var family: RoomFamilyRepository
    private lateinit var personal: RoomFinanceRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        family = RoomFamilyRepository(database)
        personal = RoomFinanceRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun familyImportDeduplicatesRecordsAndKeepsThemOutOfPersonalLedger() = runBlocking {
        val id = UUID.randomUUID().toString()
        val row = FamilyTransaction(
            id = id, owner = "Wife", sourceMoney = Money(1250, "EUR"),
            date = LocalDate.parse("2026-10-01"), categoryName = "Food",
            type = TransactionType.EXPENSE, importRate = BigDecimal("1.08"), importBaseCurrency = "USD",
        )

        family.saveImportBatch(listOf(row))
        val second = family.saveImportBatch(listOf(row))

        assertEquals(1, second.duplicateCount)
        assertEquals(listOf(id), family.observeFamilyTransactions(
            com.personalfinance.tracker.domain.model.PeriodRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02")),
        ).first().map { it.id })
        assertEquals(emptyList<String>(), personal.observeTransactions(null).first().map { it.id })
    }

    @Test
    fun deletingImportedOwnerRowsLeavesOtherOwnersAndPersonalTransactionsUntouched() = runBlocking {
        val wife = familyRow("Wife")
        val husband = familyRow("Husband")
        family.saveImportBatch(listOf(wife, husband))
        personal.saveTransaction(
            FinanceTransaction(
                id = UUID.randomUUID().toString(), owner = "Wife", type = TransactionType.INCOME,
                date = wife.date, money = Money(5_000, "USD"), categoryId = null,
            ),
        )

        assertEquals(1, family.deleteImportedRecordsForOwner("Wife"))
        val remaining = family.observeFamilyTransactions(
            com.personalfinance.tracker.domain.model.PeriodRange(wife.date, wife.date.plusDays(1)),
        ).first()
        assertEquals(listOf("Husband"), remaining.map { it.owner })
        assertEquals(1, personal.observeTransactions(null).first().size)
    }

    private fun familyRow(owner: String) = FamilyTransaction(
        id = UUID.randomUUID().toString(), owner = owner, sourceMoney = Money(1_000, "USD"),
        date = LocalDate.parse("2026-10-01"), categoryName = "Food",
        type = TransactionType.EXPENSE, importRate = BigDecimal.ONE, importBaseCurrency = "USD",
    )
}
