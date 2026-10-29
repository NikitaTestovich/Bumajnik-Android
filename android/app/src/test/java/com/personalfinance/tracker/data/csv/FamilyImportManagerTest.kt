package com.personalfinance.tracker.data.csv

import android.net.Uri
import com.personalfinance.tracker.data.repository.FamilyRepository
import com.personalfinance.tracker.domain.model.FamilyImportSummary
import com.personalfinance.tracker.domain.model.FamilyTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyImportManagerTest {
    @Test fun incomeCommentIsPersistedInFamilySnapshot() = runBlocking {
        val repository = RecordingFamilyRepository()
        val manager = FamilyImportManager(repository) { error("unused") }
        val csv = "owner,record_id,date,category,income,expense,currency,comment\r\nDemo,${uuid()},01-10-2026,,1.00,,USD,Salary\r\n"
        val preview = manager.previewBytes(csv.toByteArray())
        assertEquals(1, manager.import(preview, "USD", emptyMap()).addedCount)
        assertEquals("Salary", repository.writes.single().single().comment)
    }
    @Test
    fun malformedFileIsRejectedWithoutWritingAnyRows() {
        val repository = RecordingFamilyRepository()
        val manager = FamilyImportManager(repository) { error("previewBytes should not read a URI") }
        val malformed = "owner,record_id,date,category,income,expense,currency\r\n\"Alex\"bad,${uuid()},2026-10-01,,1.00,,USD\r\n"

        val result = runCatching { manager.previewBytes(malformed.toByteArray()) }

        assertTrue(result.isFailure)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun repeatedRecordIdsAreReportedAsDuplicatesAndWrittenOnce() = runBlocking {
        val repository = RecordingFamilyRepository()
        val manager = FamilyImportManager(repository) { error("previewBytes should not read a URI") }
        val id = uuid()
        val csv = csv("Alex,$id,2026-10-01,Food,,1.00,USD", "Alex,$id,2026-10-01,Food,,1.00,USD")

        val preview = manager.previewBytes(csv.toByteArray())
        val result = manager.import(preview, "USD", emptyMap())

        assertEquals(1, result.addedCount)
        assertEquals(1, result.duplicateCount)
        assertEquals(listOf(id), repository.writes.single().map(FamilyTransaction::id))
    }

    @Test
    fun appliesOneSuppliedRatePerSourceCurrencyAndPersistsItsBase() = runBlocking {
        val repository = RecordingFamilyRepository()
        val manager = FamilyImportManager(repository) { error("previewBytes should not read a URI") }
        val preview = manager.previewBytes(csv(
            "Alex,${uuid()},2026-10-01,Food,,10.00,EUR",
            "Alex,${uuid()},2026-10-02,Food,,20.00,EUR",
        ).toByteArray())

        manager.import(preview, "USD", mapOf("EUR" to BigDecimal("1.08")))

        assertEquals(listOf(BigDecimal("1.08"), BigDecimal("1.08")), repository.writes.single().map(FamilyTransaction::importRate))
        assertTrue(repository.writes.single().all { it.importBaseCurrency == "USD" })
    }

    private class RecordingFamilyRepository : FamilyRepository {
        val writes = mutableListOf<List<FamilyTransaction>>()
        override fun observeFamilyTransactions(range: PeriodRange): Flow<List<FamilyTransaction>> = emptyFlow()
        override suspend fun saveImportBatch(transactions: List<FamilyTransaction>): FamilyImportSummary {
            writes += transactions
            return FamilyImportSummary(transactions.size, 0)
        }
        override suspend fun containsFamilyRecordIds(ids: Set<String>): Set<String> = emptySet()
        override suspend fun deleteImportedRecordsForOwner(owner: String): Int = 0
    }

    private fun csv(vararg rows: String) =
        "owner,record_id,date,category,income,expense,currency\r\n" + rows.joinToString("\r\n", postfix = "\r\n")

    private fun uuid() = java.util.UUID.randomUUID().toString()
}
