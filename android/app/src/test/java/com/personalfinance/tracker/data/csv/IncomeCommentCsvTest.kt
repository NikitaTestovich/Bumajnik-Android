package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.*
import com.personalfinance.tracker.domain.money.Money
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class IncomeCommentCsvTest {
    private fun income(comment: String) = FinanceTransaction("11111111-1111-1111-1111-111111111111", "Demo", TransactionType.INCOME, LocalDate.of(2026,10,1), Money(100,"USD"), null, comment = comment)
    @Test fun currentCsvPreservesIncomeComment() {
        val comment = "Salary, \"bonus\"\n😀"
        val bytes = FinanceCsvCodec.encode(listOf(income(comment)), "Demo")
        assertTrue(bytes.toString(Charsets.UTF_8).startsWith("owner,record_id,date,category,income,expense,currency,comment\r\n"))
        assertEquals(comment, FinanceCsvCodec.decode(bytes).records.single().comment)
    }
    @Test fun legacyExportPassesUnchangedOriginalReader() {
        val bytes = FinanceCsvCodec.encode(listOf(income("Salary")), "Demo", format = CsvFormat.LEGACY_1_1)
        assertTrue(Legacy11CsvCodec.decode(bytes).errors.isEmpty())
        assertEquals("", FinanceCsvCodec.decode(bytes).records.single().comment)
        assertEquals(7, bytes.toString(Charsets.UTF_8).lineSequence().first().split(',').size)
    }
    @Test fun unicodeLimitAndInvalidImportAreStrict() {
        assertTrue(IncomeComment.isValid("😀".repeat(150)))
        assertFalse(IncomeComment.isValid("😀".repeat(151)))
        assertTrue(IncomeComment.isValid(""))
        val valid = FinanceCsvCodec.encode(listOf(income("x".repeat(150))), "Demo").toString(Charsets.UTF_8)
        assertFalse(FinanceCsvCodec.decode(valid.replace("x".repeat(150), "x".repeat(151)).toByteArray()).errors.isEmpty())
    }
}
