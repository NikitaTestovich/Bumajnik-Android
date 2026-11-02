package com.personalfinance.tracker.ui.operations

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFormViewModelTest {
    private fun categoryRepository() = FakeFinanceRepository().also { repository -> runBlocking {
        repository.saveCategory(com.personalfinance.tracker.domain.model.Category("groceries", "Продукты", "Groceries", false))
        repository.saveCategory(com.personalfinance.tracker.domain.model.Category("food", "Еда", "Food", true))
    } }
    @Test fun incomeCommentSurvivesEditAndRejectsOverLimit() = runBlocking {
        val repository = categoryRepository()
        val model = TransactionFormViewModel(repository)
        val comment = "😀".repeat(150)
        assertEquals(TransactionSaveResult.Saved, model.save(null, TransactionType.INCOME, "Demo", "20-10-2026", "1", "USD", null, comment))
        val saved = repository.observeTransactions(null).first().single()
        assertEquals(comment, model.load(saved.id, TransactionType.INCOME, "USD")!!.comment)
        assertEquals(TransactionSaveResult.InvalidComment, model.save(saved.id, TransactionType.INCOME, "Demo", "20-10-2026", "2", "USD", null, comment + "x"))
        assertEquals(comment, repository.observeTransactions(null).first().single().comment)
        assertEquals(TransactionSaveResult.Saved, model.save(null, TransactionType.EXPENSE, "Demo", "20-10-2026", "1", "USD", "food", "ignored"))
        assertEquals("ignored", repository.observeTransactions(TransactionType.EXPENSE).first().single().comment)
    }
    @Test
    fun expenseCannotBeSavedWithoutCategory() = runBlocking {
        val repository = categoryRepository()
        val result = TransactionFormViewModel(repository).save(
            transactionId = null,
            type = TransactionType.EXPENSE,
            owner = "Alex",
            dateText = "20-10-2026",
            amountText = "12.50",
            currencyCode = "USD",
            categoryId = null,
        )

        assertEquals(TransactionSaveResult.CategoryRequired, result)
        assertTrue(repository.observeTransactions(null).first().isEmpty())
    }

    @Test
    fun incomeCanBeSavedWithoutCategory() = runBlocking {
        val repository = categoryRepository()
        val result = TransactionFormViewModel(repository).save(
            transactionId = null,
            type = TransactionType.INCOME,
            owner = "Alex",
            dateText = "20-10-2026",
            amountText = "12.50",
            currencyCode = "USD",
            categoryId = null,
        )

        assertEquals(TransactionSaveResult.Saved, result)
        assertEquals(null, repository.observeTransactions(null).first().single().categoryId)
    }

    @Test
    fun saveParsesDateAndDecimalAmount() = runBlocking {
        val repository = categoryRepository()
        val result = TransactionFormViewModel(repository).save(
            transactionId = null,
            type = TransactionType.EXPENSE,
            owner = "Alex",
            dateText = "17-10-2026",
            amountText = "123.45",
            currencyCode = "EUR",
            categoryId = "groceries",
        )

        val saved = repository.observeTransactions(null).first().single()
        assertEquals(TransactionSaveResult.Saved, result)
        assertEquals(LocalDate.of(2026, 10, 17), saved.date)
        assertEquals(Money(12345, "EUR"), saved.money)
    }

    @Test
    fun editDraftDisplaysDayMonthYearDate() = runBlocking {
        val repository = categoryRepository()
        val transaction = FinanceTransaction(
            id = "0e20e7cf-091d-48d0-a6f0-1664397dc4d0",
            owner = "Alex",
            type = TransactionType.INCOME,
            date = LocalDate.of(2026, 10, 21),
            money = Money(1000, "USD"),
            categoryId = null,
        )
        repository.saveTransaction(transaction)

        val draft = TransactionFormViewModel(repository).load(transaction.id, transaction.type, "USD")

        assertEquals("21-10-2026", draft?.dateText)
    }

    @Test
    fun editingKeepsTheExistingTransactionId() = runBlocking {
        val repository = categoryRepository()
        val original = FinanceTransaction(
            id = "0e20e7cf-091d-48d0-a6f0-1664397dc4d0",
            owner = "Alex",
            type = TransactionType.EXPENSE,
            date = LocalDate.of(2026, 10, 17),
            money = Money(1000, "USD"),
            categoryId = "groceries",
        )
        repository.saveTransaction(original)

        val result = TransactionFormViewModel(repository).save(
            transactionId = original.id,
            type = original.type,
            owner = original.owner,
            dateText = "18-10-2026",
            amountText = "20.00",
            currencyCode = "EUR",
            categoryId = "groceries",
        )

        val saved = repository.observeTransactions(null).first().single()
        assertEquals(TransactionSaveResult.Saved, result)
        assertEquals(original.id, saved.id)
        assertEquals(2000L, saved.money.minorUnits)
        assertEquals("USD", saved.money.currencyCode)
    }

    @Test
    fun deleteOnlyOccursAfterConfirmation() = runBlocking {
        val repository = categoryRepository()
        val transaction = FinanceTransaction(
            id = "0e20e7cf-091d-48d0-a6f0-1664397dc4d0",
            owner = "Alex",
            type = TransactionType.INCOME,
            date = LocalDate.of(2026, 10, 17),
            money = Money(1000, "USD"),
            categoryId = null,
        )
        repository.saveTransaction(transaction)
        val viewModel = OperationsViewModel(repository)

        viewModel.requestDelete(transaction.id)
        assertTrue(repository.observeTransactions(null).first().isNotEmpty())
        assertEquals(transaction.id, viewModel.pendingDeleteId)

        viewModel.confirmDelete()

        assertFalse(repository.observeTransactions(null).first().any { it.id == transaction.id })
        assertEquals(null, viewModel.pendingDeleteId)
    }
}
