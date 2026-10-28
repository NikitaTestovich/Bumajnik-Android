package com.personalfinance.tracker.ui.profile

import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.domain.model.CategoryIcons
import com.personalfinance.tracker.domain.model.categoryNameAvailable
import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import kotlinx.coroutines.flow.first

class ProfileViewModel(private val repository: FinanceRepository) {
    suspend fun updateProfile(name: String, languageTag: String, baseCurrency: String): Boolean {
        val normalizedName = name.trim()
        val normalizedLanguage = languageTag.substringBefore('-').lowercase()
        val currency = baseCurrency.trim().uppercase()
        if (normalizedName.isEmpty() || normalizedLanguage !in setOf("ru", "en") || currency !in SupportedCurrencies.codes) {
            return false
        }
        val current = repository.observeProfile().first() ?: return false
        repository.saveProfile(current.copy(name = normalizedName, languageTag = normalizedLanguage, baseCurrency = currency))
        return true
    }

    suspend fun addCustomCategory(name: String, languageTag: String, iconKey: String = "other"): Boolean {
        val normalizedName = name.trim()
        if (!categoryNameAvailable(normalizedName, repository.observeCategories().first())) return false
        val id = "custom-${java.util.UUID.randomUUID()}"
        val normalizedLanguage = languageTag.substringBefore('-').lowercase()
        if (normalizedLanguage !in setOf("ru", "en")) return false
        repository.saveCategory(
            Category(id, normalizedName, normalizedName, true, iconKey = CategoryIcons.resolve(iconKey)),
        )
        return true
    }

    suspend fun renameCategory(id: String, name: String, iconKey: String): Boolean {
        val categories = repository.observeCategories().first()
        val category = categories.firstOrNull { it.id == id && !it.isArchived } ?: return false
        if (!categoryNameAvailable(name, categories, id)) return false
        repository.saveCategory(category.copy(nameRu = name.trim(), nameEn = name.trim(), iconKey = CategoryIcons.resolve(iconKey)))
        return true
    }

    suspend fun archiveCategory(id: String): Boolean {
        val category = repository.observeCategories().first().firstOrNull { it.id == id } ?: return false
        repository.saveCategory(category.copy(isArchived = true))
        return true
    }
}
