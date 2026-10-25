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
