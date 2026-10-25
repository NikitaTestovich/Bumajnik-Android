package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCsvCodecTest {
    @Test
    fun encodeUsesExactHeaderAndKeepsStableIdsAndCurrencies() {
        val id = UUID.randomUUID().toString()
        val bytes = FinanceCsvCodec.encode(
            listOf(
                FinanceTransaction(
                    id = id,
                    owner = "Alex",
                    type = TransactionType.INCOME,
                    date = LocalDate.parse("2026-10-03"),
                    money = Money(12345, "USD"),
                    categoryId = null,
                ),
            ),
            owner = "Alex",
        )

        val csv = String(bytes, StandardCharsets.UTF_8)
        assertTrue(csv.startsWith("owner,record_id,date,category,income,expense,currency\r\n"))
        assertTrue(csv.contains("Alex,$id,03-10-2026,,123.45,,USD"))
        assertFalse(csv.startsWith("\uFEFF"))
    }

    @Test
    fun roundTripPreservesQuotesCommasAndRussianText() {
        val id = UUID.randomUUID().toString()
        val transaction = FinanceTransaction(
            id = id,
            owner = "Иван, \"старший\"",
            type = TransactionType.EXPENSE,
            date = LocalDate.parse("2026-10-20"),
            money = Money(990, "RUB"),
            categoryId = "custom-category",
        )
        val csv = String(
            FinanceCsvCodec.encode(listOf(transaction), transaction.owner, mapOf("custom-category" to "Кафе, \"Лес\"")),
            StandardCharsets.UTF_8,
        )

        assertTrue(csv.contains("\"Иван, \"\"старший\"\"\""))
        assertTrue(csv.contains("\"Кафе, \"\"Лес\"\"\""))
        val decoded = FinanceCsvCodec.decode(csv.toByteArray(StandardCharsets.UTF_8))
        assertTrue(decoded.errors.isEmpty())
        assertEquals(1, decoded.records.size)
        val row = decoded.records.single()
        assertEquals(id, row.recordId)
        assertEquals(transaction.owner, row.owner)
        assertEquals("Кафе, \"Лес\"", row.categoryName)
        assertEquals(TransactionType.EXPENSE, row.type)
        assertEquals(Money(990, "RUB"), row.amount)
    }

    @Test
    fun incomeDecodesWithBlankCategoryAndDayMonthYearDate() {
        val id = UUID.randomUUID().toString()
        val csv = "owner,record_id,date,category,income,expense,currency\r\n" +
            "Alex,$id,01-10-2026,,10.00,,EUR\r\n"

        val decoded = FinanceCsvCodec.decode(csv.toByteArray(StandardCharsets.UTF_8))

        assertTrue(decoded.errors.isEmpty())
        assertEquals(LocalDate.parse("2026-10-01"), decoded.records.single().date)
        assertEquals(TransactionType.INCOME, decoded.records.single().type)
        assertEquals(null, decoded.records.single().categoryName)
        assertEquals(Money(1000, "EUR"), decoded.records.single().amount)
    }

    @Test
    fun importsLegacyIsoDate() {
        val id = UUID.randomUUID().toString()
        val csv = "owner,record_id,date,category,income,expense,currency\r\n" +
            "Alex,$id,2026-10-01,,10.00,,EUR\r\n"

        val decoded = FinanceCsvCodec.decode(csv.toByteArray(StandardCharsets.UTF_8))

        assertTrue(decoded.errors.isEmpty())
        assertEquals(LocalDate.parse("2026-10-01"), decoded.records.single().date)
    }

    @Test
    fun malformedQuotePlacementRejectsTheWholeFile() {
        val id = UUID.randomUUID().toString()
        val csv = "owner,record_id,date,category,income,expense,currency\r\n" +
            "\"Alex\"suffix,$id,2026-10-01,,10.00,,EUR\r\n"

        val decoded = FinanceCsvCodec.decode(csv.toByteArray(StandardCharsets.UTF_8))

        assertTrue("malformed quoting must be reported", decoded.errors.isNotEmpty())
        assertTrue("a malformed file must not yield importable records", decoded.records.isEmpty())
    }
}
