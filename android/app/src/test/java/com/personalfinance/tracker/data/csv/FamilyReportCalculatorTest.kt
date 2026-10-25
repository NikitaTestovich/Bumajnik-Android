package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.FamilyTransaction
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyReportCalculatorTest {
    @Test
    fun aggregatesExpensesByOwnerAndCategoryAfterCurrencyConversion() {
        val imported = FamilyTransaction(
            id = "wife-food", owner = "Wife", sourceMoney = Money(1000, "EUR"),
            date = LocalDate.parse("2026-10-01"), categoryName = "Food",
            type = TransactionType.EXPENSE, importRate = BigDecimal("1.10"), importBaseCurrency = "USD",
        )
        val personal = FinanceTransaction(
            id = "husband-transport", owner = "Husband", type = TransactionType.EXPENSE,
            date = LocalDate.parse("2026-10-02"), money = Money(2000, "BYN"), categoryId = "transport",
        )

        val report = FamilyReportCalculator.calculate(
            importedRows = listOf(imported),
            personalTransactions = listOf(personal),
            currentBase = "RUB",
            rates = mapOf("USD" to BigDecimal("90"), "BYN" to BigDecimal("3.30")),
            categoryNamesById = mapOf("transport" to "Transport"),
        )

        assertEquals(99_000L, report.expensesByOwnerAndCategory.getValue("Wife").getValue("Food"))
        assertEquals(6_600L, report.expensesByOwnerAndCategory.getValue("Husband").getValue("Transport"))
    }

    @Test
    fun convertsFromSourceCurrencyThroughSavedImportBaseIntoCurrentBase() {
        val row = FamilyTransaction(
            id = "id", owner = "Wife", sourceMoney = Money(1000, "EUR"),
            date = LocalDate.parse("2026-10-01"), categoryName = "Food",
            type = TransactionType.EXPENSE, importRate = BigDecimal("1.10"), importBaseCurrency = "USD",
        )

        val report = FamilyReportCalculator.calculate(listOf(row), "RUB", mapOf("USD" to BigDecimal("90")))

        assertEquals(99_000L, report.expensesMinorUnits)
        assertEquals(99_000L, report.byOwner.getValue("Wife").expensesMinorUnits)
    }

    @Test
    fun includesPersonalOwnerTransactionsAlongsideImportedFamilyRows() {
        val imported = FamilyTransaction(
            id = "wife-row", owner = "Wife", sourceMoney = Money(1000, "EUR"),
            date = LocalDate.parse("2026-10-01"), categoryName = "Food",
            type = TransactionType.EXPENSE, importRate = BigDecimal("1.10"), importBaseCurrency = "USD",
        )
        val personal = FinanceTransaction(
            id = "husband-row", owner = "Husband", type = TransactionType.INCOME,
            date = LocalDate.parse("2026-10-02"), money = Money(2000, "BYN"), categoryId = null,
        )

        val report = FamilyReportCalculator.calculate(
            importedRows = listOf(imported),
            personalTransactions = listOf(personal),
            currentBase = "RUB",
            rates = mapOf("USD" to BigDecimal("90"), "BYN" to BigDecimal("3.30")),
        )

        assertEquals(6_600L, report.incomeMinorUnits)
        assertEquals(99_000L, report.expensesMinorUnits)
        assertEquals(6_600L, report.byOwner.getValue("Husband").incomeMinorUnits)
        assertEquals(99_000L, report.byOwner.getValue("Wife").expensesMinorUnits)
    }

    @Test
    fun reportsAllMissingPersonalAndImportedRebaseRateCurrencies() {
        val imported = FamilyTransaction(
            id = "wife-row", owner = "Wife", sourceMoney = Money(1000, "EUR"),
            date = LocalDate.parse("2026-10-01"), categoryName = "Food",
            type = TransactionType.EXPENSE, importRate = BigDecimal("1.10"), importBaseCurrency = "USD",
        )
        val personal = FinanceTransaction(
            id = "husband-row", owner = "Husband", type = TransactionType.INCOME,
            date = LocalDate.parse("2026-10-02"), money = Money(2000, "BYN"), categoryId = null,
        )

        assertEquals(
            setOf("USD", "BYN"),
            FamilyReportCalculator.missingCurrencies(listOf(imported), listOf(personal), "RUB", emptyMap()),
        )
    }

    @Test
    fun doesNotCountPersonalRowAgainWhenItsCsvWasImported() {
        val importedDuplicate = FamilyTransaction(
            id = "same-record-id", owner = "Husband", sourceMoney = Money(1000, "EUR"),
            date = LocalDate.parse("2026-10-01"), categoryName = "Food",
            type = TransactionType.EXPENSE, importRate = BigDecimal("1.10"), importBaseCurrency = "USD",
        )
        val personal = FinanceTransaction(
            id = "same-record-id", owner = "Husband", type = TransactionType.EXPENSE,
            date = LocalDate.parse("2026-10-01"), money = Money(120_000, "RUB"), categoryId = "food",
        )

        val report = FamilyReportCalculator.calculate(
            importedRows = listOf(importedDuplicate),
            personalTransactions = listOf(personal),
            currentBase = "RUB",
            rates = emptyMap(),
        )

        assertEquals(120_000L, report.expensesMinorUnits)
        assertEquals(120_000L, report.byOwner.getValue("Husband").expensesMinorUnits)
    }
}
