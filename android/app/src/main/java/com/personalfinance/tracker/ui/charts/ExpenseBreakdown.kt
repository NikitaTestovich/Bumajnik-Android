package com.personalfinance.tracker.ui.charts

import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import com.personalfinance.tracker.domain.summary.MissingExchangeRatesException
import java.math.BigDecimal
import java.math.RoundingMode

data class ExpenseSlice(val categoryId: String?, val name: String, val iconKey: String, val minorUnits: Long, val share: Double)

fun expenseBreakdown(transactions: List<FinanceTransaction>, categories: List<Category>, languageTag: String, baseCurrency: String, rates: Map<String, BigDecimal>): List<ExpenseSlice> {
    require(baseCurrency in SupportedCurrencies.codes)
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val missing = expenses.map { it.money.currencyCode }.filter { it != baseCurrency && it !in rates }.toSet()
    if (missing.isNotEmpty()) throw MissingExchangeRatesException(missing)
    val amounts = linkedMapOf<String?, Long>()
    var total = 0L
    expenses.forEach {
        val rate = if (it.money.currencyCode == baseCurrency) BigDecimal.ONE else rates.getValue(it.money.currencyCode)
        require(rate > BigDecimal.ZERO)
        val amount = BigDecimal.valueOf(it.money.minorUnits).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact()
        require(amount >= 0)
        amounts[it.categoryId] = Math.addExact(amounts[it.categoryId] ?: 0, amount)
        total = Math.addExact(total, amount)
    }
    val lookup = categories.associateBy { it.id }
    return amounts.map { (id, amount) ->
        val category = lookup[id]
        val name = if (languageTag == "ru") category?.nameRu ?: category?.nameEn else category?.nameEn ?: category?.nameRu
        ExpenseSlice(id, name ?: if (languageTag == "ru") "Без категории" else "Uncategorized", category?.iconKey ?: "other", amount, if (total == 0L) 0.0 else amount.toDouble() / total)
    }.sortedWith(compareByDescending<ExpenseSlice> { it.minorUnits }.thenBy { it.categoryId ?: "" })
}
