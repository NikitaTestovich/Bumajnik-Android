package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SpreadsheetReportCodecTest {
    @Test fun largeAmountsRemainExactTextWhileFifteenDigitAmountsRemainNumeric() {
        val values = listOf(999_999_999_999_999L, 1_000_000_000_000_000L, Long.MAX_VALUE, Long.MIN_VALUE)
        val records = values.mapIndexed { index, amount -> FinanceTransaction(
            index.toString(), "owner", TransactionType.INCOME, LocalDate.parse("2026-10-03"),
            Money(amount, "USD"), null,
        ) }
        val bytes = SpreadsheetReportCodec.encode(records, "owner", emptyMap())
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes() }
        }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(entries.getValue("xl/worksheets/sheet1.xml")))
        val rows = doc.getElementsByTagNameNS("*", "row")
        values.forEachIndexed { index, value ->
            val cells = (rows.item(index + 1) as org.w3c.dom.Element).getElementsByTagNameNS("*", "c")
            val amount = cells.item(4) as org.w3c.dom.Element
            assertEquals(java.math.BigDecimal.valueOf(value, 2).toPlainString(), amount.textContent)
            assertEquals(if (index == 0) "n" else "inlineStr", amount.getAttribute("t"))
        }
    }

    @Test fun spreadsheetStoresFormulaLikeTextLiterallyAndAmountsAsNumbers() {
        val transaction = FinanceTransaction("=id", "unused", TransactionType.EXPENSE,
            LocalDate.parse("2026-10-03"), Money(12345, "RUB"), "c", "@сумма & <текст>")
        val bytes = SpreadsheetReportCodec.encode(listOf(transaction), "+Иван", mapOf("c" to "-Кафе"))
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes() }
        }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        entries.values.forEach { factory.newDocumentBuilder().parse(ByteArrayInputStream(it)) }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(entries.getValue("xl/worksheets/sheet1.xml")))
        val rows = doc.getElementsByTagNameNS("*", "row")
        val cells = (rows.item(1) as org.w3c.dom.Element).getElementsByTagNameNS("*", "c")
        assertEquals(listOf("+Иван", "=id", "03-10-2026", "-Кафе", "", "123.45", "RUB", "@сумма & <текст>"),
            (0 until cells.length).map { cells.item(it).textContent })
        assertEquals("n", (cells.item(5) as org.w3c.dom.Element).getAttribute("t"))
        listOf(0, 1, 2, 3, 4, 6, 7).forEach { assertEquals("inlineStr", (cells.item(it) as org.w3c.dom.Element).getAttribute("t")) }
        assertFalse(String(entries.getValue("xl/worksheets/sheet1.xml")).contains("<f>"))
    }
}
