package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A viewing report; unlike CSV, this is not an interchange or backup format. */
object SpreadsheetReportCodec {
    fun encode(records: List<FinanceTransaction>, owner: String, categories: Map<String, String>): ByteArray {
        val sheet = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        fun row(values: List<Pair<String, Boolean>>) {
            sheet.append("<row>")
            values.forEach { (value, numeric) ->
                if (numeric) sheet.append("<c t=\"n\"><v>").append(value).append("</v></c>")
                else sheet.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(value)).append("</t></is></c>")
            }
            sheet.append("</row>")
        }
        row(FinanceCsvCodec.header.map { it to false })
        records.forEach { transaction ->
            val amount = BigDecimal.valueOf(transaction.money.minorUnits, 2).toPlainString()
            // Excel retains at most 15 significant decimal digits in numeric cells.
            // Preserve larger minor-unit values exactly as literal text.
            val numericAmount = transaction.money.minorUnits.toString().removePrefix("-").length <= 15
            row(listOf(owner, transaction.id, FinanceDateFormat.format(transaction.date),
                if (transaction.type == TransactionType.EXPENSE) transaction.categoryId?.let(categories::get).orEmpty() else "")
                .map { it to false } + listOf(
                if (transaction.type == TransactionType.INCOME) amount to numericAmount else "" to false,
                if (transaction.type == TransactionType.EXPENSE) amount to numericAmount else "" to false,
                transaction.money.currencyCode to false, transaction.comment to false))
        }
        sheet.append("</sheetData></worksheet>")
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            fun entry(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Report" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            entry("xl/worksheets/sheet1.xml", sheet.toString())
        }
        return bytes.toByteArray()
    }

    private fun xml(value: String): String = buildString {
        value.codePoints().forEach { cp ->
            when (cp) {
                38 -> append("&amp;")
                60 -> append("&lt;")
                62 -> append("&gt;")
                9, 10, 13 -> appendCodePoint(cp)
                in 0x20..0xD7FF, in 0xE000..0xFFFD, in 0x10000..0x10FFFF -> appendCodePoint(cp)
            }
        }
    }
}
