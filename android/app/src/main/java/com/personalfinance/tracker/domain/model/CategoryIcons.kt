package com.personalfinance.tracker.domain.model

/** Stable persisted keys; unknown/old values always render the bundled fallback. */
object CategoryIcons {
    val keys = listOf("groceries", "home", "transport", "phone", "health", "education", "clothing", "dining", "entertainment", "household_goods", "travel", "gifts", "pets", "sport", "beauty", "children", "insurance", "taxes", "tools", "other", "bank_card", "airplane", "headphones", "bicycle", "coffee", "plant", "computer", "clock", "scissors", "umbrella")
    fun resolve(key: String): String = key.takeIf { it in keys } ?: "other"
    fun forStandard(id: String): String = resolve(when (id) {
        "housing_utilities" -> "home"
        "communications" -> "phone"
        "healthcare" -> "health"
        "cafes_restaurants" -> "dining"
        else -> id
    })
}

fun Category.label(languageTag: String): String =
    if (languageTag == "ru") nameRu ?: nameEn.orEmpty() else nameEn ?: nameRu.orEmpty()

fun normalizedCategoryName(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)

fun categoryNameAvailable(name: String, categories: List<Category>, exceptId: String? = null): Boolean {
    val normalized = normalizedCategoryName(name)
    return normalized.isNotEmpty() && categories.none { category ->
        !category.isArchived && category.id != exceptId && listOfNotNull(category.nameRu, category.nameEn)
            .any { normalizedCategoryName(it) == normalized }
    }
}
