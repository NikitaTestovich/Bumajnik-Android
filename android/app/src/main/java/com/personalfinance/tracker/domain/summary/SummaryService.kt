package com.personalfinance.tracker.domain.summary

import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.domain.money.Money
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import java.math.BigDecimal
import java.math.RoundingMode

data class PeriodSummary(val income: Money, val expense: Money, val balance: Money)

class MissingExchangeRatesException(val currencies: Set<String>) : IllegalArgumentException(
    "Missing exchange rates for: ${currencies.sorted().joinToString()}",
)

class SummaryService(private val repository: FinanceRepository) {
    suspend fun summarize(
        range: PeriodRange,
        baseCurrency: String,
        rates: Map<String, BigDecimal>,
    ): PeriodSummary {
        require(baseCurrency in SupportedCurrencies.codes) { "Unsupported base currency: $baseCurrency" }
        val transactions = repository.transactionsBetween(range, null)
        val missingRates = transactions.asSequence()
            .map { it.money.currencyCode }
            .filter { it != baseCurrency && it !in rates }
            .toSet()
        if (missingRates.isNotEmpty()) throw MissingExchangeRatesException(missingRates)

        var incomeMinorUnits = 0L
        var expenseMinorUnits = 0L
        transactions.forEach { transaction ->
            val rate = if (transaction.money.currencyCode == baseCurrency) {
                BigDecimal.ONE
            } else {
                rates.getValue(transaction.money.currencyCode)
            }
            require(rate > BigDecimal.ZERO) { "Exchange rates must be positive" }
            val convertedMinorUnits = BigDecimal.valueOf(transaction.money.minorUnits)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
            when (transaction.type) {
                TransactionType.INCOME -> incomeMinorUnits = Math.addExact(incomeMinorUnits, convertedMinorUnits)
                TransactionType.EXPENSE -> expenseMinorUnits = Math.addExact(expenseMinorUnits, convertedMinorUnits)
            }
        }
        return PeriodSummary(
            income = Money(incomeMinorUnits, baseCurrency),
            expense = Money(expenseMinorUnits, baseCurrency),
            balance = Money(Math.subtractExact(incomeMinorUnits, expenseMinorUnits), baseCurrency),
        )
    }
}
