package com.personalfinance.tracker.data.repository

import androidx.room.withTransaction
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.local.FamilyImportBatchEntity
import com.personalfinance.tracker.data.local.FamilyTransactionEntity
import com.personalfinance.tracker.domain.model.FamilyImportSummary
import com.personalfinance.tracker.domain.model.FamilyTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.model.requireUuid
import com.personalfinance.tracker.domain.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FamilyRepository {
    fun observeFamilyTransactions(range: PeriodRange): Flow<List<FamilyTransaction>>
    suspend fun saveImportBatch(transactions: List<FamilyTransaction>): FamilyImportSummary
    suspend fun containsFamilyRecordIds(ids: Set<String>): Set<String>
    suspend fun deleteImportedRecordsForOwner(owner: String): Int
}

class RoomFamilyRepository(private val database: AppDatabase) : FamilyRepository {
    private val dao = database.familyDao()

    override fun observeFamilyTransactions(range: PeriodRange): Flow<List<FamilyTransaction>> =
        dao.observeFamilyTransactions(range.start.toString(), range.endExclusive.toString())
            .map { rows -> rows.map(FamilyTransactionEntity::toDomain) }

    override suspend fun saveImportBatch(transactions: List<FamilyTransaction>): FamilyImportSummary {
        transactions.forEach { transaction ->
            requireUuid(transaction.id)
            require(transaction.sourceMoney.minorUnits > 0) { "Family transaction amount must be positive" }
        }
        if (transactions.isEmpty()) return FamilyImportSummary(addedCount = 0, duplicateCount = 0)

        val distinct = transactions.distinctBy(FamilyTransaction::id)
        val duplicatesInsideFile = transactions.size - distinct.size
        val batchId = UUID.randomUUID().toString()
        return database.withTransaction {
            val existing = dao.existingIds(distinct.map(FamilyTransaction::id)).toSet()
            val newRecords = distinct.filterNot { it.id in existing }
            if (newRecords.isNotEmpty()) {
                dao.insertBatch(
                    FamilyImportBatchEntity(
                        id = batchId,
                        importedAtEpochMillis = System.currentTimeMillis(),
                        recordCount = newRecords.size,
                    ),
                )
                dao.insertFamilyTransactions(newRecords.map { it.toEntity(batchId) })
            }
            FamilyImportSummary(
                addedCount = newRecords.size,
                duplicateCount = duplicatesInsideFile + existing.size,
            )
        }
    }

    override suspend fun containsFamilyRecordIds(ids: Set<String>): Set<String> {
        if (ids.isEmpty()) return emptySet()
        return dao.existingIds(ids.toList()).toSet()
    }

    override suspend fun deleteImportedRecordsForOwner(owner: String): Int = database.withTransaction {
        val deleted = dao.deleteTransactionsForOwner(owner)
        dao.deleteEmptyImportBatches()
        deleted
    }
}

private fun FamilyTransaction.toEntity(batchId: String) = FamilyTransactionEntity(
    id = id,
    owner = owner,
    type = type.name,
    date = date.toString(),
    minorUnits = sourceMoney.minorUnits,
    currencyCode = sourceMoney.currencyCode,
    categoryName = categoryName,
    importRate = importRate?.stripTrailingZeros()?.toPlainString(),
    importBaseCurrency = importBaseCurrency,
    batchId = batchId,
)

private fun FamilyTransactionEntity.toDomain() = FamilyTransaction(
    id = id,
    owner = owner,
    sourceMoney = Money(minorUnits, currencyCode),
    date = LocalDate.parse(date),
    categoryName = categoryName,
    type = TransactionType.valueOf(type),
    importRate = importRate?.let(::BigDecimal),
    importBaseCurrency = importBaseCurrency,
)
