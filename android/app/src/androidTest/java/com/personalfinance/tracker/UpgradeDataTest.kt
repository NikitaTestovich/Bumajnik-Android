package com.personalfinance.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.security.LockResult
import com.personalfinance.tracker.ui.profile.ProfileViewModel
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit two-stage test: seed with the preserved 1.1 APK, install 1.15 with -r, then verify. Emulator only. */
@RunWith(AndroidJUnit4::class)
class UpgradeDataTest {
    @Test fun preservesEveryStoreAcrossApkUpdate() = runBlocking {
        val phase = InstrumentationRegistry.getArguments().getString("upgradePhase")
        assumeTrue("Seed with preserved 1.1 test APK, then run this verifier explicitly", phase == "verify")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
        val container = app.container
        val repository = container.financeRepository
        val date = LocalDate.of(2026, 10, 1)
        val range = PeriodRange(date, date.plusMonths(1))
        val profile = UserProfile("Demo Alex", "en", "USD", true)
        val category = Category("groceries", "Продукты", "Groceries", false)
        val custom = Category("custom-demo", "Хобби", "Hobbies", true)
        val goal = SavingsGoal(Money(1_000_000, "USD"), Money(200_000, "USD"))
        val personal = listOf(
            FinanceTransaction("11111111-1111-4111-8111-111111111111", profile.name, TransactionType.INCOME, date, Money(300_000, "USD"), null),
            FinanceTransaction("22222222-2222-4222-8222-222222222222", profile.name, TransactionType.EXPENSE, date, Money(40_000, "USD"), category.id),
        )
        val family = listOf(
            FamilyTransaction("33333333-3333-4333-8333-333333333333", "Demo Sam", Money(200_000, "USD"), date, null, TransactionType.INCOME, null),
            FamilyTransaction("44444444-4444-4444-8444-444444444444", "Demo Sam", Money(30_000, "USD"), date, "Продукты", TransactionType.EXPENSE, null),
        )
        assertEquals(profile, repository.observeProfile().first())
        assertEquals(personal.toSet(), repository.observeTransactions(null).first().toSet())
        val expectedGroceries = category.copy(iconKey = "groceries")
        assertEquals(setOf(expectedGroceries, custom), repository.observeCategories().first().toSet())
        assertEquals(goal, repository.observeGoal().first())
        assertEquals(family.toSet(), container.familyRepository.observeFamilyTransactions(range).first().toSet())
        assertEquals(BigDecimal("1.1"), container.rateRepository.ratesFor(range, "USD").first().getValue("EUR"))
        assertEquals(3, container.database.openHelper.readableDatabase.version)
        assertTrue(app.appLock.state.value.enabled)
        assertEquals(LockResult.SUCCESS, app.appLock.unlock("Ab12"))
        assertTrue(ProfileViewModel(repository).renameCategory("groceries", "Food upgraded", "dining"))
        assertTrue(ProfileViewModel(repository).archiveCategory("groceries"))
        assertEquals("Food upgraded", repository.observeCategories().first().first { it.id == "groceries" }.nameEn)
        assertTrue(repository.observeCategories().first().first { it.id == "groceries" }.isArchived)
        assertEquals("groceries", repository.observeTransactions(null).first().first { it.type == TransactionType.EXPENSE }.categoryId)
    }
}
