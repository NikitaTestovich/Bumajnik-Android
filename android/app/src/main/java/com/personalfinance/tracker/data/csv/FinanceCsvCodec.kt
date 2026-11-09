package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.model.IncomeComment
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.time.FinanceDateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

data class CsvTransactionRow(
    val recordId: String,
    val owner: String,
    val date: LocalDate,
    val type: TransactionType,
    val amount: Money,
    val categoryName: String?,
    val comment: String = "",
)

enum class CsvFormat { CURRENT, LEGACY_1_1 }

data class CsvImportResult(val records: List<CsvTransactionRow>, val errors: List<String>)

class CsvExportLimitException : IllegalArgumentException("CSV exceeds import limits")

object FinanceCsvCodec {
    val legacyHeader: List<String> = listOf("owner", "record_id", "date", "category", "income", "expense", "currency")
    val header: List<String> = legacyHeader + "comment"
    private val headerRow = header.joinToString(",")

    fun encode(
        records: List<FinanceTransaction>,
        owner: String,
        categoryNamesById: Map<String, String> = emptyMap(),
        format: CsvFormat = CsvFormat.CURRENT,
    ): ByteArray {
        if (records.size > 100_000) throw CsvExportLimitException()
        val builder = StringBuilder().append((if (format == CsvFormat.CURRENT) header else legacyHeader).joinToString(",")).append("\r\n")
        var byteCount = builder.toString().toByteArray(StandardCharsets.UTF_8).size
        records.forEach { transaction ->
            require(IncomeComment.isValid(transaction.comment))
            val amount = BigDecimal.valueOf(transaction.money.minorUnits, 2).toPlainString()
            val fields = listOf(
                owner,
                transaction.id,
                FinanceDateFormat.format(transaction.date),
                if (transaction.type == TransactionType.EXPENSE) {
                    transaction.categoryId?.let(categoryNamesById::get).orEmpty()
                } else {
                    ""
                },
                if (transaction.type == TransactionType.INCOME) amount else "",
                if (transaction.type == TransactionType.EXPENSE) amount else "",
                transaction.money.currencyCode,
            )
            val output = if (format == CsvFormat.CURRENT) fields + transaction.comment else fields
            if (output.any { it.length > 65_536 }) throw CsvExportLimitException()
            val row = output.joinToString(",", transform = ::quote) + "\r\n"
            byteCount += row.toByteArray(StandardCharsets.UTF_8).size
            if (byteCount > CSV_MAX_BYTES) throw CsvExportLimitException()
            builder.append(row)
        }
        return builder.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(bytes: ByteArray, checkCancellation: () -> Unit = {}): CsvImportResult {
        if (bytes.size > CSV_MAX_BYTES) return CsvImportResult(emptyList(), listOf("CSV file is too large"))
        val text = bytes.toString(StandardCharsets.UTF_8).removePrefix("\uFEFF")
        val rows = try {
            parseRows(text, checkCancellation)
        } catch (error: IllegalArgumentException) {
            return CsvImportResult(emptyList(), listOf(error.message ?: "Malformed CSV quoting"))
        }
        if (rows.isEmpty()) return CsvImportResult(emptyList(), listOf("CSV file is empty"))
        if (rows.first() != header && rows.first() != legacyHeader) {
            return CsvImportResult(emptyList(), listOf("CSV header must be: $headerRow"))
        }

        val records = mutableListOf<CsvTransactionRow>()
        val errors = mutableListOf<String>()
        rows.drop(1).forEachIndexed { index, fields ->
            if (index % 256 == 0) checkCancellation()
            val rowNumber = index + 2
            val parsed = parseRecord(fields, rowNumber, rows.first().size)
            if (parsed.isSuccess) records += parsed.getOrThrow() else errors += parsed.exceptionOrNull()?.message.orEmpty()
            if (errors.size >= 100) return CsvImportResult(emptyList(), errors)
        }
        return CsvImportResult(records, errors)
    }

    private fun parseRecord(fields: List<String>, rowNumber: Int, columns: Int): Result<CsvTransactionRow> = runCatching {
        require(fields.size == columns) { "Row $rowNumber must contain $columns columns" }
        val owner = fields[0].trim()
        require(owner.isNotEmpty()) { "Row $rowNumber has an empty owner" }
        val id = fields[1].trim()
        require(runCatching { UUID.fromString(id) }.isSuccess) { "Row $rowNumber has an invalid record ID" }
        val date = FinanceDateFormat.parse(fields[2])
            ?: throw IllegalArgumentException("Row $rowNumber has an invalid date")
        val incomeText = fields[4].trim()
        val expenseText = fields[5].trim()
        require((incomeText.isNotEmpty()) xor (expenseText.isNotEmpty())) {
            "Row $rowNumber must have exactly one income or expense amount"
        }
        val type = if (incomeText.isNotEmpty()) TransactionType.INCOME else TransactionType.EXPENSE
        val category = fields[3].ifBlank { null }
        require(type == TransactionType.EXPENSE || category == null) {
            "Row $rowNumber income category must be blank"
        }
        val amountText = if (type == TransactionType.INCOME) incomeText else expenseText
        val minorUnits = runCatching {
            BigDecimal(amountText)
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        }.getOrElse { throw IllegalArgumentException("Row $rowNumber has an invalid amount") }
        require(minorUnits > 0) { "Row $rowNumber amount must be positive" }
        val currency = fields[6].trim()
        val money = runCatching { Money(minorUnits, currency) }.getOrElse {
            throw IllegalArgumentException("Row $rowNumber has an unsupported currency")
        }
        val comment = if (columns == header.size) fields[7] else ""
        require(IncomeComment.isValid(comment)) { "Row $rowNumber comment exceeds 150 characters" }
        CsvTransactionRow(id, owner, date, type, money, category, comment)
    }

    private fun quote(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun parseRows(input: String, checkCancellation: () -> Unit): List<List<String>> {
        if (input.isEmpty()) return emptyList()
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var atFieldStart = true
        var closedQuotedField = false
        var index = 0
        var nextCancellationCheck = 0
        while (index < input.length) {
            if (index >= nextCancellationCheck) { checkCancellation(); nextCancellationCheck = index + 4096 }
            val char = input[index]
            when {
                inQuotes && char == '"' && input.getOrNull(index + 1) == '"' -> {
                    require(field.length < 65_536) { "CSV field is too large" }
                    field.append('"')
                    index++
                }
                char == '"' && inQuotes -> { inQuotes = false; closedQuotedField = true }
                char == '"' && atFieldStart && field.isEmpty() && !closedQuotedField -> inQuotes = true
                char == ',' && !inQuotes -> {
                    require(row.size < 8) { "CSV has too many columns" }
                    row += field.toString()
                    field.setLength(0)
                    atFieldStart = true
                    closedQuotedField = false
                }
                (char == '\r' || char == '\n') && !inQuotes -> {
                    require(row.size < 8) { "CSV has too many columns" }
                    row += field.toString()
                    field.setLength(0)
                    if (row.any(String::isNotEmpty)) {
                        require(rows.size < 100_001) { "CSV has too many rows" }
                        rows += row
                    }
                    row = mutableListOf()
                    atFieldStart = true
                    closedQuotedField = false
                    if (char == '\r' && input.getOrNull(index + 1) == '\n') index++
                }
                else -> {
                    require(!closedQuotedField) { "Malformed CSV: characters after a closing quote" }
                    require(char != '"') { "Malformed CSV: quote inside an unquoted field" }
                    require(field.length < 65_536) { "CSV field is too large" }
                    field.append(char)
                    if (!char.isWhitespace()) atFieldStart = false
                }
            }
            index++
        }
        require(!inQuotes) { "Malformed CSV: unterminated quoted field" }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            require(row.size < 8 && rows.size < 100_001) { "CSV has too many rows or columns" }
            row += field.toString()
            if (row.any(String::isNotEmpty)) rows += row
        }
        return rows
    }
}
