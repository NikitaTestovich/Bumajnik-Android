package com.personalfinance.tracker.data.repository

import com.personalfinance.tracker.domain.model.CategorySeed

object DefaultCategories {
    private data class Names(val id: String, val ru: String, val en: String)

    private val categories = listOf(
        Names("groceries", "Продукты", "Groceries"),
        Names("housing_utilities", "Жильё и коммунальные услуги", "Housing and utilities"),
        Names("transport", "Транспорт", "Transport"),
        Names("communications", "Связь", "Communications"),
        Names("healthcare", "Здоровье", "Healthcare"),
        Names("education", "Образование", "Education"),
        Names("clothing", "Одежда", "Clothing"),
        Names("cafes_restaurants", "Кафе и рестораны", "Cafes and restaurants"),
        Names("entertainment", "Развлечения", "Entertainment"),
        Names("household_goods", "Бытовые товары", "Household goods"),
        Names("other", "Другое", "Other"),
    )

    fun forLanguage(languageTag: String): List<CategorySeed> = categories.map { names ->
        CategorySeed(id = names.id, nameRu = names.ru, nameEn = names.en)
    }
}
