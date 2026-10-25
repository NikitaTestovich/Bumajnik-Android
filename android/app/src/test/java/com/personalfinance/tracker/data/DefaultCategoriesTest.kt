package com.personalfinance.tracker.data

import com.personalfinance.tracker.data.repository.DefaultCategories
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.ui.profile.ProfileViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DefaultCategoriesTest {
    @Test
    fun returnsElevenStableIdsAndLocalizedNames() {
        val russian = DefaultCategories.forLanguage("ru-RU")
        val english = DefaultCategories.forLanguage("en-US")

        assertEquals(11, russian.size)
        assertEquals(russian.map { it.id }, english.map { it.id })
        assertNotEquals(russian.map { it.nameRu }, english.map { it.nameEn })
        assertEquals("Продукты", russian.first().nameRu)
        assertEquals("Groceries", english.first().nameEn)
    }

    @Test
    fun unsupportedLanguageUsesEnglishNames() {
        val categories = DefaultCategories.forLanguage("fr-FR")

        assertEquals("Groceries", categories.first().nameEn)
        assertEquals(11, categories.size)
    }

    @Test
    fun changingProfileLanguageDoesNotRewriteCustomCategoryNames() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveProfile(com.personalfinance.tracker.domain.model.UserProfile("Alex", "ru", "RUB", true))
        val custom = Category("custom-bike", "Велосипед", null, true)
        repository.saveCategory(custom)
        val viewModel = ProfileViewModel(repository)

        viewModel.updateProfile(name = "Alex", languageTag = "en", baseCurrency = "RUB")

        assertEquals(custom, repository.observeCategories().first().single())
    }
}
