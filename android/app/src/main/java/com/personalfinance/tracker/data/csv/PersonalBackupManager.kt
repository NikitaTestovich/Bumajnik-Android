package com.personalfinance.tracker.data.csv

import android.content.ContentResolver
import android.net.Uri
import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.data.repository.PersonalRestoreRecord
import com.personalfinance.tracker.domain.model.FinanceTransaction
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupRestoreResult(val addedCount: Int, val duplicateCount: Int, val errors: List<String>)

class PersonalBackupManager(
    private val repository: FinanceRepository,
    private val readBytes: (Uri) -> ByteArray,
    private val writeBytes: (Uri, ByteArray) -> Unit,
) {
    constructor(contentResolver: ContentResolver, repository: FinanceRepository) : this(
        repository,
        { uri -> contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No input stream") },
        { uri, bytes ->
            val stream = contentResolver.openOutputStream(uri, "wt") ?: error("No output stream")
            stream.use { it.write(bytes) }
        },
    )

    suspend fun export(destination: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            writeBytes(destination, backupBytes())
            ExportResult(true, null)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The screen translates this code; never display provider exception text.
            ExportResult(false, "backup_export_failed")
        }
    }

    suspend fun restore(source: Uri): BackupRestoreResult = withContext(Dispatchers.IO) {
        val bytes = try {
            readBytes(source)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@withContext BackupRestoreResult(0, 0, listOf("backup_read_failed"))
        }
        restoreBytes(bytes)
    }

    suspend fun restoreBytes(bytes: ByteArray): BackupRestoreResult {
        try {
            // The shared codec tolerates invalid UTF-8; backups must not silently change labels.
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes))
        } catch (_: CharacterCodingException) {
            return BackupRestoreResult(0, 0, listOf("backup_invalid_csv"))
        }
        val decoded = FinanceCsvCodec.decode(bytes)
        if (decoded.errors.isNotEmpty()) return BackupRestoreResult(0, 0, listOf("backup_invalid_csv"))
        // Canonical UUID strings prevent case variants from bypassing duplicate detection.
        val records = decoded.records.map { row ->
            PersonalRestoreRecord(
                FinanceTransaction(UUID.fromString(row.recordId).toString(), row.owner, row.type, row.date, row.amount, null, comment = row.comment),
                row.categoryName,
            )
        }
        return try {
            val summary = repository.restorePersonalTransactions(records)
            BackupRestoreResult(summary.addedCount, summary.duplicateCount, emptyList())
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            BackupRestoreResult(0, 0, listOf("backup_restore_failed"))
        }
    }

    suspend fun backupBytes(): ByteArray {
        val profile = checkNotNull(repository.observeProfile().first())
        val records = repository.observeTransactions(null).first()
        val categoryNames = repository.observeCategories().first().associate { category ->
            category.id to if (profile.languageTag == "ru") category.nameRu ?: category.nameEn.orEmpty()
            else category.nameEn ?: category.nameRu.orEmpty()
        }
        return FinanceCsvCodec.encode(records, profile.name, categoryNames)
    }
}
