package com.personalfinance.tracker.data.repository

import androidx.room.withTransaction
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.local.CategoryEntity
import com.personalfinance.tracker.data.local.FinanceTransactionEntity
import com.personalfinance.tracker.data.local.SavingsGoalEntity
import com.personalfinance.tracker.data.local.UserProfileEntity
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.domain.model.requireUuid
import com.personalfinance.tracker.domain.money.Money
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FinanceRepository {
    fun observeTransactions(type: TransactionType?): Flow<List<FinanceTransaction>>
    suspend fun transactionsBetween(range: PeriodRange, type: TransactionType?): List<FinanceTransaction>
    suspend fun saveTransaction(transaction: FinanceTransaction)
    /** Inserts new IDs and category mappings together; never replaces personal data. */
    suspend fun restorePersonalTransactions(records: List<PersonalRestoreRecord>): PersonalRestoreSummary
    suspend fun deleteTransaction(id: String)
    fun observeProfile(): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
    fun observeCategories(): Flow<List<Category>>
    suspend fun saveCategory(category: Category)
    fun observeGoal(): Flow<SavingsGoal?>
    suspend fun saveGoal(goal: SavingsGoal)
}

data class PersonalRestoreRecord(val transaction: FinanceTransaction, val categoryName: String?)
data class PersonalRestoreSummary(val addedCount: Int, val duplicateCount: Int)

/** Shared conversion policy; persistence of these categories belongs to the batch transaction. */
internal fun restoredCategory(name: String, categories: Collection<Category>): Category {
    val normalized = name.trim().lowercase(Locale.ROOT)
    return categories.firstOrNull { category ->
        listOfNotNull(category.nameRu, category.nameEn).any { it.trim().lowercase(Locale.ROOT) == normalized }
    } ?: Category(
        id = "backup-" + UUID.nameUUIDFromBytes("personal-category:$normalized".toByteArray(Charsets.UTF_8)),
        nameRu = name.trim(),
        nameEn = name.trim(),
        isCustom = true,
    )
}

class RoomFinanceRepository(private val database: AppDatabase) : FinanceRepository {
    private val dao = database.financeDao()

    override fun observeTransactions(type: TransactionType?): Flow<List<FinanceTransaction>> =
        dao.observeTransactions(type?.name).map { items -> items.map(FinanceTransactionEntity::toDomain) }

    override suspend fun transactionsBetween(
        range: PeriodRange,
        type: TransactionType?,
    ): List<FinanceTransaction> = dao.transactionsBetween(
        start = range.start.toString(),
        endExclusive = range.endExclusive.toString(),
        type = type?.name,
    ).map(FinanceTransactionEntity::toDomain)

    override suspend fun saveTransaction(transaction: FinanceTransaction) {
        requireUuid(transaction.id)
        require(transaction.owner.isNotBlank()) { "Transaction owner must not be blank" }
        require(transaction.money.minorUnits > 0) { "Transaction amount must be positive" }
        database.withTransaction {
            val order = dao.transactionById(transaction.id)?.createdOrder ?: Math.addExact(dao.maxCreatedOrder(), 1L)
            dao.saveTransaction(transaction.copy(createdOrder = order).toEntity())
        }
    }

    override suspend fun restorePersonalTransactions(records: List<PersonalRestoreRecord>): PersonalRestoreSummary {
        // Validate even duplicate rows before opening a write transaction.
        records.forEach { (transaction, categoryName) ->
            requireUuid(transaction.id)
            require(transaction.owner.isNotBlank())
            require(transaction.money.minorUnits > 0)
            require(transaction.type == TransactionType.EXPENSE || categoryName.isNullOrBlank())
        }
        return database.withTransaction {
            val existingIds = dao.transactionIds().toSet()
            val categories = dao.categories().map(CategoryEntity::toDomain).toMutableList()
            val newRecords = records.distinctBy { it.transaction.id }.filterNot { it.transaction.id in existingIds }
            var nextOrder = dao.maxCreatedOrder()
            val transactions = newRecords.map { record ->
                val category = record.categoryName?.takeIf { it.isNotBlank() }?.let { name ->
                    restoredCategory(name, categories).also { mapped ->
                        if (categories.none { it.id == mapped.id }) {
                            dao.insertRestoredCategory(CategoryEntity(mapped.id, mapped.nameRu, mapped.nameEn, mapped.isCustom, mapped.isArchived, mapped.iconKey))
                            categories += mapped
                        }
                    }
                }
                nextOrder = Math.addExact(nextOrder, 1L)
                record.transaction.copy(categoryId = category?.id, createdOrder = nextOrder).toEntity()
            }
            val added = dao.insertRestoredTransactions(transactions).count { it != -1L }
            PersonalRestoreSummary(added, records.size - added)
        }
    }

    override suspend fun deleteTransaction(id: String) {
        dao.deleteTransaction(id)
    }

    override fun observeProfile(): Flow<UserProfile?> = dao.observeProfile().map { entity -> entity?.toDomain() }

    override suspend fun saveProfile(profile: UserProfile) {
        dao.saveProfile(
            UserProfileEntity(
                name = profile.name,
                languageTag = profile.languageTag,
                baseCurrency = profile.baseCurrency,
                onboardingComplete = profile.onboardingComplete,
            ),
        )
    }

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { items -> items.map(CategoryEntity::toDomain) }

    override suspend fun saveCategory(category: Category) {
        dao.saveCategory(CategoryEntity(category.id, category.nameRu, category.nameEn, category.isCustom, category.isArchived, category.iconKey))
    }

    override fun observeGoal(): Flow<SavingsGoal?> = dao.observeGoal().map { entity -> entity?.toDomain() }

    override suspend fun saveGoal(goal: SavingsGoal) {
        dao.saveGoal(
            SavingsGoalEntity(
                targetMinorUnits = goal.target.minorUnits,
                targetCurrency = goal.target.currencyCode,
                savedMinorUnits = goal.saved.minorUnits,
                savedCurrency = goal.saved.currencyCode,
            ),
        )
    }
}

private fun FinanceTransaction.toEntity() = FinanceTransactionEntity(
    id = id,
    owner = owner,
    type = type.name,
    date = date.toString(),
    minorUnits = money.minorUnits,
    currencyCode = money.currencyCode,
    categoryId = categoryId,
    comment = comment,
    createdOrder = createdOrder,
)

private fun FinanceTransactionEntity.toDomain() = FinanceTransaction(
    id = id,
    owner = owner,
    type = TransactionType.valueOf(type),
    date = LocalDate.parse(date),
    money = Money(minorUnits, currencyCode),
    categoryId = categoryId,
    comment = comment,
    createdOrder = createdOrder,
)

private fun UserProfileEntity.toDomain() = UserProfile(name, languageTag, baseCurrency, onboardingComplete)
private fun CategoryEntity.toDomain() = Category(id, nameRu, nameEn, isCustom, isArchived, iconKey)
private fun SavingsGoalEntity.toDomain() = SavingsGoal(
    target = Money(targetMinorUnits, targetCurrency),
    saved = Money(savedMinorUnits, savedCurrency),
)
