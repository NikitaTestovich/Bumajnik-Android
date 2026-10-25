package com.personalfinance.tracker.data

import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.data.repository.PersonalRestoreRecord
import com.personalfinance.tracker.data.repository.PersonalRestoreSummary
import com.personalfinance.tracker.data.repository.restoredCategory
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.SavingsGoal
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal open class FakeFinanceRepository : FinanceRepository {
    private val transactions = MutableStateFlow<List<FinanceTransaction>>(emptyList())
    private val profile = MutableStateFlow<UserProfile?>(null)
    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val goal = MutableStateFlow<SavingsGoal?>(null)

    override fun observeTransactions(type: TransactionType?): Flow<List<FinanceTransaction>> = transactions
    override suspend fun transactionsBetween(range: PeriodRange, type: TransactionType?) = transactions.value
        .filter { it.date >= range.start && it.date < range.endExclusive && (type == null || it.type == type) }
    override suspend fun saveTransaction(transaction: FinanceTransaction) {
        transactions.value = transactions.value.filterNot { it.id == transaction.id } + transaction
    }
    override suspend fun restorePersonalTransactions(records: List<PersonalRestoreRecord>): PersonalRestoreSummary {
        val existing = transactions.value.map { it.id }.toSet()
        val additions = records.distinctBy { it.transaction.id }.filterNot { it.transaction.id in existing }.map { record ->
            val category = record.categoryName?.takeIf { it.isNotBlank() }?.let { restoredCategory(it, categories.value) }
            if (category != null) saveCategory(category)
            record.transaction.copy(categoryId = category?.id)
        }
        transactions.value += additions
        return PersonalRestoreSummary(additions.size, records.size - additions.size)
    }
    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value.filterNot { it.id == id }
    }
    override fun observeProfile(): Flow<UserProfile?> = profile
    override suspend fun saveProfile(profile: UserProfile) { this.profile.value = profile }
    override fun observeCategories(): Flow<List<Category>> = categories
    override suspend fun saveCategory(category: Category) {
        categories.value = categories.value.filterNot { it.id == category.id } + category
    }
    override fun observeGoal(): Flow<SavingsGoal?> = goal
    override suspend fun saveGoal(goal: SavingsGoal) { this.goal.value = goal }
}
