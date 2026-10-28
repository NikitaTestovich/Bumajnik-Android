package com.personalfinance.tracker.ui.onboarding

import com.personalfinance.tracker.data.repository.DefaultCategories
import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import kotlinx.coroutines.flow.first

enum class WelcomeResult { Saved, InvalidName, InvalidCurrency }

fun resolveInitialLanguage(languageTag: String): String =
    if (languageTag.substringBefore('-').substringBefore('_').equals("ru", ignoreCase = true)) "ru" else "en"

class WelcomeViewModel(
    private val repository: FinanceRepository,
    systemLanguageTag: String,
) {
    val initialLanguage: String = resolveInitialLanguage(systemLanguageTag)

    suspend fun completeOnboarding(name: String, currencyCode: String): WelcomeResult {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return WelcomeResult.InvalidName
        val normalizedCurrency = currencyCode.trim().uppercase()
        if (normalizedCurrency !in SupportedCurrencies.codes) return WelcomeResult.InvalidCurrency

        val language = repository.observeProfile().first()?.languageTag ?: initialLanguage
        val existingCategoryIds = repository.observeCategories().first().mapTo(mutableSetOf()) { it.id }
        DefaultCategories.forLanguage(language).forEach { seed ->
            if (seed.id !in existingCategoryIds) {
                repository.saveCategory(Category(seed.id, seed.nameRu, seed.nameEn, isCustom = false,
                    iconKey = com.personalfinance.tracker.domain.model.CategoryIcons.forStandard(seed.id)))
            }
        }
        repository.saveProfile(
            UserProfile(
                name = normalizedName,
                languageTag = language,
                baseCurrency = normalizedCurrency,
                onboardingComplete = true,
            ),
        )
        return WelcomeResult.Saved
    }
}
