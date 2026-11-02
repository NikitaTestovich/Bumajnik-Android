package com.personalfinance.tracker.ui.operations

import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationSearchTest {
    private val entries = listOf(
        FinanceTransaction("1", "Demo", TransactionType.EXPENSE, LocalDate.of(2026, 10, 12), Money(12345, "BYN"), "health", "Врач"),
        FinanceTransaction("2", "Demo", TransactionType.EXPENSE, LocalDate.of(2026, 9, 1), Money(7000, "BYN"), "food", "Магазин"),
    )

    @Test fun matchesCommentDateAndDecimalComma() {
        listOf("ВРАЧ", "12-10-2026", "123,45").forEach {
            assertEquals(listOf("1"), filterOperations(entries, it, null).map { row -> row.id })
        }
    }

    @Test fun categoryAndQueryAreCombined() {
        assertEquals(emptyList<String>(), filterOperations(entries, "врач", "food").map { it.id })
        assertEquals(listOf("1"), filterOperations(entries, "", "health").map { it.id })
    }

    @Test fun commentSearchPreservesCommaWhileAmountAcceptsDecimalComma() {
        val item = entries.first().copy(comment = "Покупка, подарок")
        assertEquals(listOf(item), filterOperations(listOf(item), "Покупка, подарок", null))
        assertEquals(listOf(item), filterOperations(listOf(item), "123,45", null))
    }
}
