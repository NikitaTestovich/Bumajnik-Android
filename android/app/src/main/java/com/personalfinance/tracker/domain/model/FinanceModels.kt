package com.personalfinance.tracker.domain.model

import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class TransactionType { INCOME, EXPENSE }

data class FinanceTransaction(
    val id: String,
    val owner: String,
    val type: TransactionType,
    val date: LocalDate,
    val money: Money,
    val categoryId: String?,
    val comment: String = "",
    val createdOrder: Long = 0,
)

data class UserProfile(
    val name: String,
    val languageTag: String,
    val baseCurrency: String,
    val onboardingComplete: Boolean,
) {
    init {
        require(name.isNotBlank()) { "Profile name must not be blank" }
        require(languageTag in setOf("ru", "en")) { "Unsupported language tag: $languageTag" }
        require(baseCurrency in SupportedCurrencies.codes) { "Unsupported currency code: $baseCurrency" }
    }
}

data class Category(
    val id: String,
    val nameRu: String?,
    val nameEn: String?,
    val isCustom: Boolean,
    val isArchived: Boolean = false,
    val iconKey: String = "other",
) {
    init {
        require(id.isNotBlank()) { "Category id must not be blank" }
        require(!nameRu.isNullOrBlank() || !nameEn.isNullOrBlank()) {
            "At least one localized category name is required"
        }
    }
}

data class CategorySeed(val id: String, val nameRu: String, val nameEn: String)

data class SavingsGoal(val target: Money, val saved: Money) {
    init {
        require(target.currencyCode == saved.currencyCode) { "Goal amounts must use the same currency" }
        require(target.minorUnits > 0) { "Goal target must be positive" }
        require(saved.minorUnits >= 0) { "Saved amount cannot be negative" }
    }
}

data class FamilyTransaction(
    val id: String,
    val owner: String,
    val sourceMoney: Money,
    val date: LocalDate,
    val categoryName: String?,
    val type: TransactionType,
    val importRate: BigDecimal?,
    val importBaseCurrency: String = sourceMoney.currencyCode,
    val comment: String = "",
) {
    init {
        require(owner.isNotBlank()) { "Family record owner must not be blank" }
        require(importRate == null || importRate > BigDecimal.ZERO) { "Import rate must be positive" }
        require(importBaseCurrency in SupportedCurrencies.codes) { "Unsupported import base currency" }
    }
}

data class PeriodRange(val start: LocalDate, val endExclusive: LocalDate) {
    init {
        require(start < endExclusive) { "Period must have a start before its exclusive end" }
    }
}

data class FamilyImportSummary(val addedCount: Int, val duplicateCount: Int)

internal fun requireUuid(value: String) {
    require(runCatching { UUID.fromString(value) }.isSuccess) { "Transaction id must be a UUID" }
}
