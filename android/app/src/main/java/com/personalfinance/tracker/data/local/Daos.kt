package com.personalfinance.tracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("DELETE FROM finance_transactions") suspend fun backupClearTransactions()
    @Query("DELETE FROM categories") suspend fun backupClearCategories()
    @Query("DELETE FROM user_profile") suspend fun backupClearProfile()
    @Query("DELETE FROM savings_goal") suspend fun backupClearGoal()
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun backupInsertTransactions(rows: List<FinanceTransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun backupInsertCategories(rows: List<CategoryEntity>)
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun backupProfile(): UserProfileEntity?
    @Query("SELECT * FROM finance_transactions ORDER BY id")
    suspend fun backupTransactions(): List<FinanceTransactionEntity>
    @Query("SELECT * FROM savings_goal WHERE id = 1 LIMIT 1")
    suspend fun backupGoal(): SavingsGoalEntity?
    @Query("SELECT * FROM finance_transactions WHERE id = :id LIMIT 1")
    suspend fun transactionById(id: String): FinanceTransactionEntity?

    @Query("SELECT COALESCE(MAX(createdOrder), 0) FROM finance_transactions")
    suspend fun maxCreatedOrder(): Long

    @Query("SELECT id FROM finance_transactions")
    suspend fun transactionIds(): List<String>

    @Query("SELECT * FROM categories ORDER BY isCustom ASC, id ASC")
    suspend fun categories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRestoredTransactions(transactions: List<FinanceTransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRestoredCategory(category: CategoryEntity)

    @Query("SELECT * FROM finance_transactions WHERE (:type IS NULL OR type = :type) ORDER BY date DESC, createdOrder DESC, id ASC")
    fun observeTransactions(type: String?): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE date >= :start AND date < :endExclusive AND (:type IS NULL OR type = :type) ORDER BY date DESC, createdOrder DESC, id ASC")
    suspend fun transactionsBetween(start: String, endExclusive: String, type: String?): List<FinanceTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTransaction(transaction: FinanceTransactionEntity)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM categories ORDER BY isCustom ASC, id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategory(category: CategoryEntity)

    @Query("SELECT * FROM savings_goal WHERE id = 1 LIMIT 1")
    fun observeGoal(): Flow<SavingsGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoal(goal: SavingsGoalEntity)
}

@Dao
interface FamilyDao {
    @Query("DELETE FROM family_transactions") suspend fun backupClearFamilyTransactions()
    @Query("DELETE FROM family_import_batches") suspend fun backupClearBatches()
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun backupInsertFamily(rows: List<FamilyTransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun backupInsertBatches(rows: List<FamilyImportBatchEntity>)
    @Query("SELECT * FROM family_transactions ORDER BY id")
    suspend fun backupFamilyTransactions(): List<FamilyTransactionEntity>
    @Query("SELECT * FROM family_import_batches ORDER BY id")
    suspend fun backupBatches(): List<FamilyImportBatchEntity>
    @Query("SELECT * FROM family_transactions WHERE date >= :start AND date < :endExclusive ORDER BY date DESC, id ASC")
    fun observeFamilyTransactions(start: String, endExclusive: String): Flow<List<FamilyTransactionEntity>>

    @Query("SELECT id FROM family_transactions WHERE id IN (:ids)")
    suspend fun existingIds(ids: List<String>): List<String>

    @Query("DELETE FROM family_transactions WHERE owner = :owner")
    suspend fun deleteTransactionsForOwner(owner: String): Int

    @Query("DELETE FROM family_import_batches WHERE id NOT IN (SELECT DISTINCT batchId FROM family_transactions WHERE batchId IS NOT NULL)")
    suspend fun deleteEmptyImportBatches()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFamilyTransactions(transactions: List<FamilyTransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: FamilyImportBatchEntity)
}

@Dao
interface ExchangeRateDao {
    @Query("DELETE FROM period_exchange_rates") suspend fun backupClearRates()
    @Query("SELECT * FROM period_exchange_rates ORDER BY rangeStart, rangeEndExclusive, baseCurrency, sourceCurrency")
    suspend fun backupRates(): List<ExchangeRateEntity>
    @Query("SELECT * FROM period_exchange_rates WHERE rangeStart = :start AND rangeEndExclusive = :endExclusive AND baseCurrency = :baseCurrency")
    fun observeRates(start: String, endExclusive: String, baseCurrency: String): Flow<List<ExchangeRateEntity>>

    @Query("DELETE FROM period_exchange_rates WHERE rangeStart = :start AND rangeEndExclusive = :endExclusive AND baseCurrency = :baseCurrency")
    suspend fun deleteRates(start: String, endExclusive: String, baseCurrency: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)
}
