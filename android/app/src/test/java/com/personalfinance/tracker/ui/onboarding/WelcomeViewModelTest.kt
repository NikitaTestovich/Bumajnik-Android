package com.personalfinance.tracker.ui.onboarding

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.data.repository.DefaultCategories
import com.personalfinance.tracker.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeViewModelTest {
    @Test
    fun resolvesRussianAndEnglishDeviceLocalesAndFallsBackToEnglish() {
        assertEquals("ru", resolveInitialLanguage("ru-RU"))
        assertEquals("en", resolveInitialLanguage("en-US"))
        assertEquals("en", resolveInitialLanguage("fr-FR"))
    }

    @Test
    fun trimsNameAndPersistsProfileAndDefaultCategoriesOnce() = runBlocking {
        val repository = FakeFinanceRepository()
        val viewModel = WelcomeViewModel(repository, "ru-RU")

        assertEquals(WelcomeResult.Saved, viewModel.completeOnboarding("  Анна  ", "BYN"))
        assertEquals(UserProfile("Анна", "ru", "BYN", true), repository.observeProfile().first())
        val expected = DefaultCategories.forLanguage("ru").map { Triple(it.id, it.nameRu, it.nameEn) }
        val actual = repository.observeCategories().first().map { Triple(it.id, it.nameRu, it.nameEn) }
        assertEquals(expected, actual)

        viewModel.completeOnboarding("Анна", "BYN")
        assertEquals(11, repository.observeCategories().first().size)
    }

    @Test
    fun rejectsBlankNamesAndUnsupportedCurrencyWithoutSaving() = runBlocking {
        val repository = FakeFinanceRepository()
        val viewModel = WelcomeViewModel(repository, "en-US")

        assertEquals(WelcomeResult.InvalidName, viewModel.completeOnboarding("   ", "USD"))
        assertEquals(WelcomeResult.InvalidCurrency, viewModel.completeOnboarding("Alex", "JPY"))
        assertFalse(repository.observeProfile().first() != null)
        assertTrue(repository.observeCategories().first().isEmpty())
    }
}
