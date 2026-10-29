package com.personalfinance.tracker.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.repository.RoomFinanceRepository
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.domain.money.Money
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomFinanceRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomFinanceRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun profileCanBeSavedAndLoaded() = runBlocking {
        val profile = UserProfile(
            name = "Alex",
            languageTag = "en",
            baseCurrency = "USD",
            onboardingComplete = true,
        )

        repository.saveProfile(profile)

        assertEquals(profile, repository.observeProfile().first())
    }

    @Test
    fun transactionsAreOrderedByDateDescending() = runBlocking {
        val older = transaction(LocalDate.of(2026, 1, 2))
        val newer = transaction(LocalDate.of(2026, 1, 5))
        repository.saveTransaction(older)
        repository.saveTransaction(newer)

        assertEquals(listOf(newer.id, older.id), repository.observeTransactions(null).first().map { it.id })
    }

    @Test
    fun sameDayCreationOrderSurvivesEditingAndDateBoundaries() = runBlocking {
        val older = transaction(LocalDate.parse("2026-01-01")).copy(id = "00000000-0000-0000-0000-000000000001", type = TransactionType.EXPENSE)
        val newer = older.copy(id = "ffffffff-ffff-ffff-ffff-ffffffffffff")
        val december = transaction(LocalDate.parse("2025-12-31")).copy(type = TransactionType.EXPENSE)
        val february = transaction(LocalDate.parse("2026-02-01")).copy(type = TransactionType.EXPENSE)
        listOf(older, newer, december, february).forEach { repository.saveTransaction(it) }
        repository.saveTransaction(older.copy(money = Money(2000, "USD")))
        val expected = listOf(february.id, newer.id, older.id, december.id)
        assertEquals(expected, repository.observeTransactions(null).first().map { it.id })
        assertEquals(expected, repository.transactionsBetween(
            com.personalfinance.tracker.domain.model.PeriodRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2027-01-01")), null,
        ).map { it.id })
    }

    @Test
    fun unknownCreationOrderUsesIdTieBreak() = runBlocking {
        val date = LocalDate.parse("2026-01-01")
        val first = transaction(date).copy(id = "00000000-0000-0000-0000-000000000001")
        val last = first.copy(id = "ffffffff-ffff-ffff-ffff-ffffffffffff")
        for (row in listOf(last, first)) {
            database.financeDao().saveTransaction(com.personalfinance.tracker.data.local.FinanceTransactionEntity(
                row.id, row.owner, row.type.name, row.date.toString(), row.money.minorUnits, row.money.currencyCode, row.categoryId,
            ))
        }
        repository.saveTransaction(last.copy(comment = "edited"))
        val rows = repository.observeTransactions(null).first()
        assertEquals(listOf(first.id, last.id), rows.map { it.id })
        assertEquals(listOf(0L, 0L), rows.map { it.createdOrder })
    }

    @Test
    fun restoreOrdersNewRowsAndNeverOverwritesDuplicateMetadata() = runBlocking {
        val original = transaction(LocalDate.parse("2026-01-01")).copy(comment = "original")
        repository.saveTransaction(original)
        val saved = repository.observeTransactions(null).first().single()
        val second = original.copy(id = "00000000-0000-0000-0000-000000000001", comment = "second", createdOrder = 999)
        val third = second.copy(id = "ffffffff-ffff-ffff-ffff-ffffffffffff", comment = "third")
        val records = listOf(original.copy(comment = "duplicate"), second, third, second)
            .map { com.personalfinance.tracker.data.repository.PersonalRestoreRecord(it, null) }
        assertEquals(com.personalfinance.tracker.data.repository.PersonalRestoreSummary(2, 2), repository.restorePersonalTransactions(records))
        val rows = repository.observeTransactions(null).first()
        assertEquals(listOf(third.id, second.id, original.id), rows.map { it.id })
        assertEquals(listOf(3L, 2L, 1L), rows.map { it.createdOrder })
        assertEquals(saved, rows.last())
        assertEquals(listOf("third", "second", "original"), rows.map { it.comment })
    }

    @Test
    fun categoriesRoundTripArchiveStateWithoutFilteringHistory() = runBlocking {
        val category = com.personalfinance.tracker.domain.model.Category("custom", "RU", "EN", true, true, "pets")
        repository.saveCategory(category)
        assertEquals(category, repository.observeCategories().first().single())
    }

    @Test
    fun unsupportedCurrencyCodeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(minorUnits = 100, currencyCode = "JPY")
        }
    }

    private fun transaction(date: LocalDate) = FinanceTransaction(
        id = UUID.randomUUID().toString(),
        owner = "Alex",
        type = TransactionType.INCOME,
        date = date,
        money = Money(minorUnits = 1000, currencyCode = "USD"),
        categoryId = null,
    )
}
