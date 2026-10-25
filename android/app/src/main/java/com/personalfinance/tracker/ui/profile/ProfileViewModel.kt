package com.personalfinance.tracker.ui.profile

import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.UserProfile
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

    suspend fun addCustomCategory(name: String, languageTag: String): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return false
        val id = "custom-${java.util.UUID.randomUUID()}"
        val normalizedLanguage = languageTag.substringBefore('-').lowercase()
        if (normalizedLanguage !in setOf("ru", "en")) return false
        repository.saveCategory(
            if (normalizedLanguage == "ru") Category(id, normalizedName, null, true)
            else Category(id, null, normalizedName, true),
        )
        return true
    }
}
