package com.personalfinance.tracker.data.csv

import android.content.ContentResolver
import android.net.Uri
import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.model.PeriodRange
import kotlinx.coroutines.flow.first

data class ExportResult(val success: Boolean, val message: String?)

class ReportExportManager(
    private val contentResolver: ContentResolver,
    private val repository: FinanceRepository,
) {
    suspend fun exportReport(range: PeriodRange, destination: Uri, format: CsvFormat = CsvFormat.CURRENT, spreadsheet: Boolean = false): ExportResult {
        val profile = repository.observeProfile().first()
            ?: return ExportResult(false, "Profile is not set")
        val records = repository.transactionsBetween(range, null)
        val categories = repository.observeCategories().first().associate { category ->
            val name = if (profile.languageTag == "ru") {
                category.nameRu ?: category.nameEn.orEmpty()
            } else {
                category.nameEn ?: category.nameRu.orEmpty()
            }
            category.id to name
        }
        val data = try {
            if (spreadsheet) SpreadsheetReportCodec.encode(records, profile.name, categories)
            else FinanceCsvCodec.encode(records, profile.name, categories, format)
        } catch (_: CsvExportLimitException) {
            return ExportResult(false, "csv_export_limit")
        }

        return runCatching {
            val output = contentResolver.openOutputStream(destination)
                ?: return ExportResult(false, "Unable to open the selected file")
            output.use { it.write(data) }
            ExportResult(success = true, message = null)
        }.getOrElse { error ->
            ExportResult(success = false, message = error.localizedMessage ?: "Unable to export the report")
        }
    }
}
