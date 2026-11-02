package com.personalfinance.tracker.ui.operations

import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import java.math.BigDecimal

fun filterOperations(entries: List<FinanceTransaction>, query: String, categoryId: String?): List<FinanceTransaction> {
    val needle = query.trim()
    val amountNeedle = needle.replace(',', '.')
    return entries.filter { entry ->
        (categoryId == null || entry.categoryId == categoryId) &&
            (needle.isEmpty() || entry.comment.contains(needle, ignoreCase = true) ||
                FinanceDateFormat.format(entry.date).contains(needle) ||
                BigDecimal.valueOf(entry.money.minorUnits, 2).toPlainString().contains(amountNeedle))
    }
}
