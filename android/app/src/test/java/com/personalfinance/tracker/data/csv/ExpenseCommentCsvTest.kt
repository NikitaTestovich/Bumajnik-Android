package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCommentCsvTest {
    @Test fun expenseCommentRoundTripsWithCsvEscapes() {
        val comment = "Врач, \"анализы\"\nповторно"
        val item = FinanceTransaction("11111111-1111-1111-1111-111111111111", "Demo", TransactionType.EXPENSE, LocalDate.of(2026, 10, 1), Money(1234, "BYN"), "health", comment = comment)
        val bytes = FinanceCsvCodec.encode(listOf(item), "Demo", mapOf("health" to "Здоровье"))
        val result = FinanceCsvCodec.decode(bytes)
        assertTrue(result.errors.isEmpty())
        assertEquals(comment, result.records.single().comment)
    }
}
