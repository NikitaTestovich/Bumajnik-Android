package com.personalfinance.tracker.data.csv

import android.net.Uri
import com.personalfinance.tracker.data.repository.FamilyRepository
import com.personalfinance.tracker.domain.model.FamilyTransaction
import com.personalfinance.tracker.domain.model.FamilyImportSummary
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import java.math.BigDecimal
import java.time.LocalDate

data class FamilyImportPreview(
    val ownerNames: Set<String>,
    val sourceCurrencies: Set<String>,
    val records: List<CsvTransactionRow>,
)

data class FamilyImportResult(
    val addedCount: Int,
    val duplicateCount: Int,
    val rejectedCount: Int,
    val errors: List<String>,
)

class FamilyCsvValidationException(val validationErrors: List<String>) :
    IllegalArgumentException(validationErrors.joinToString("\n"))

class FamilyImportManager(
    private val repository: FamilyRepository,
    private val readBytes: (Uri) -> ByteArray,
) {
    fun preview(uri: Uri): FamilyImportPreview = previewBytes(readBytes(uri))

    fun previewBytes(bytes: ByteArray): FamilyImportPreview {
        val decoded = FinanceCsvCodec.decode(bytes)
        if (decoded.errors.isNotEmpty()) throw FamilyCsvValidationException(decoded.errors)
        if (decoded.records.isEmpty()) throw FamilyCsvValidationException(listOf("CSV has no transactions"))
        return FamilyImportPreview(
            ownerNames = decoded.records.mapTo(sortedSetOf()) { it.owner },
            sourceCurrencies = decoded.records.mapTo(sortedSetOf()) { it.amount.currencyCode },
            records = decoded.records,
        )
    }

    suspend fun import(
        preview: FamilyImportPreview,
        baseCurrency: String,
        rates: Map<String, BigDecimal>,
    ): FamilyImportResult {
        val errors = validate(preview, baseCurrency, rates)
        if (errors.isNotEmpty()) {
            return FamilyImportResult(0, 0, errors.size, errors)
        }
        val unique = preview.records.distinctBy(CsvTransactionRow::recordId)
        val imported = unique.map { row ->
            FamilyTransaction(
                id = row.recordId,
                owner = row.owner,
                sourceMoney = row.amount,
                date = row.date,
                categoryName = row.categoryName,
                type = row.type,
                importRate = if (row.amount.currencyCode == baseCurrency) BigDecimal.ONE else rates.getValue(row.amount.currencyCode),
                importBaseCurrency = baseCurrency,
                comment = row.comment,
            )
        }
        val summary = repository.saveImportBatch(imported)
        val duplicateCount = summary.duplicateCount + preview.records.size - unique.size
        return FamilyImportResult(summary.addedCount, duplicateCount, 0, emptyList())
    }

    private fun validate(
        preview: FamilyImportPreview,
        baseCurrency: String,
        rates: Map<String, BigDecimal>,
    ): List<String> = buildList {
        if (baseCurrency !in SupportedCurrencies.codes) add("Unsupported base currency: $baseCurrency")
        preview.records.forEachIndexed { index, row ->
            if (!com.personalfinance.tracker.domain.model.IncomeComment.isValid(row.comment))
                add("Row ${index + 2} has an invalid comment")
            if (row.owner.isBlank()) add("Row ${index + 2} has an empty owner")
            if (row.amount.minorUnits <= 0) add("Row ${index + 2} amount must be positive")
            if (!runCatching { LocalDate.parse(row.date.toString()) }.isSuccess) add("Row ${index + 2} has an invalid date")
            if (row.amount.currencyCode !in SupportedCurrencies.codes) add("Row ${index + 2} has an unsupported currency")
        }
        val needed = preview.sourceCurrencies - baseCurrency
        needed.filter { it !in rates }.forEach { add("Missing conversion rate for $it") }
        needed.filter { it in rates && rates.getValue(it) <= BigDecimal.ZERO }
            .forEach { add("Conversion rate for $it must be positive") }
    }
}
