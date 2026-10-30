package com.personalfinance.tracker.ui.operations

import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.model.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class OperationsViewModel(
    private val repository: FinanceRepository,
    private val today: LocalDate = LocalDate.now(),
) {
    private val _pendingDeleteId = MutableStateFlow<String?>(null)
    val pendingDeleteId: String? get() = _pendingDeleteId.value
    val pendingDeleteIdFlow: StateFlow<String?> = _pendingDeleteId.asStateFlow()

    fun transactions(type: TransactionType): Flow<List<com.personalfinance.tracker.domain.model.FinanceTransaction>> =
        repository.observeTransactions(type)

    fun currentMonthTotals(type: TransactionType): Flow<Map<String, Long>> {
        val start = today.withDayOfMonth(1)
        val end = start.plusMonths(1)
        return repository.observeTransactions(type).map { transactions ->
            transactions.asSequence()
                .filter { it.date >= start && it.date < end }
                .groupBy { it.money.currencyCode }
                .mapValues { (_, entries) -> entries.sumOf { it.money.minorUnits } }
        }
    }

    fun requestDelete(id: String) {
        _pendingDeleteId.value = id
    }

    suspend fun confirmDelete() {
        _pendingDeleteId.value?.let { repository.deleteTransaction(it) }
        _pendingDeleteId.value = null
    }

    fun cancelDelete() {
        _pendingDeleteId.value = null
    }
}
