package com.personalfinance.tracker.data.csv

import com.personalfinance.tracker.domain.model.FamilyTransaction
import com.personalfinance.tracker.domain.model.FinanceTransaction
import com.personalfinance.tracker.domain.model.TransactionType
import com.personalfinance.tracker.data.repository.DefaultCategories
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class FamilyOwnerTotals(val incomeMinorUnits: Long = 0, val expensesMinorUnits: Long = 0)
data class FamilyReportTotals(
    val incomeMinorUnits: Long,
    val expensesMinorUnits: Long,
    val byOwner: Map<String, FamilyOwnerTotals>,
    val expensesByOwnerAndCategory: Map<String, Map<String, Long>>,
    val expensesByCategory: Map<String, Long>,
)

object FamilyReportCalculator {
    fun missingRebaseCurrencies(rows: List<FamilyTransaction>, currentBase: String, rates: Map<String, BigDecimal>): Set<String> =
        rows.asSequence().map(FamilyTransaction::importBaseCurrency).filter { it != currentBase }.toSet() - rates.keys

    fun calculate(
        rows: List<FamilyTransaction>,
        currentBase: String,
        ratesFromImportBases: Map<String, BigDecimal>,
    ): FamilyReportTotals = calculate(rows, emptyList(), currentBase, ratesFromImportBases)

    fun missingCurrencies(
        importedRows: List<FamilyTransaction>,
        personalTransactions: List<FinanceTransaction>,
        currentBase: String,
        rates: Map<String, BigDecimal>,
    ): Set<String> {
        val uniqueImportedRows = withoutPersonalDuplicates(importedRows, personalTransactions)
        val familyCurrencies = missingRebaseCurrencies(uniqueImportedRows, currentBase, rates)
        val personalCurrencies = personalTransactions.asSequence()
            .map { it.money.currencyCode }
            .filter { it != currentBase }
            .toSet() - rates.keys
        return familyCurrencies + personalCurrencies
    }

    fun calculate(
        importedRows: List<FamilyTransaction>,
        personalTransactions: List<FinanceTransaction>,
        currentBase: String,
        rates: Map<String, BigDecimal>,
        categoryNamesById: Map<String, String> = emptyMap(),
        uncategorizedLabel: String = "Uncategorized",
    ): FamilyReportTotals {
        val missing = missingCurrencies(importedRows, personalTransactions, currentBase, rates)
        require(missing.isEmpty()) { "Missing rebase rates for ${missing.sorted().joinToString()}" }
        require(rates.values.all { it > BigDecimal.ZERO }) { "Exchange rates must be positive" }
        val uniqueImportedRows = withoutPersonalDuplicates(importedRows, personalTransactions)
        val totals = mutableMapOf<String, FamilyOwnerTotals>()
        val categoryTotals = mutableMapOf<String, MutableMap<String, Long>>()
        val combinedCategories = mutableMapOf<String, Long>()
        val categoryLabels = mutableMapOf<String, String>()
        fun normalized(label: String) = label.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        val standardNames = DefaultCategories.forLanguage("en").flatMap { seed ->
            listOf(normalized(seed.nameRu) to seed, normalized(seed.nameEn) to seed)
        }.toMap()
        // Prefer the device's localized names, including for equivalent imported categories.
        val preferredLabels = categoryNamesById.values.associateBy({ normalized(it) }, { it.trim() })
        fun combinedLabel(label: String, personal: Boolean = false): String {
            val seed = standardNames[normalized(label)]
            val localName = seed?.let { categoryNamesById[it.id] }
            // Imported labels are snapshots: only localize an untouched standard alias.
            val untouched = seed != null && localName != null && normalized(localName) in setOf(normalized(seed.nameRu), normalized(seed.nameEn))
            val display = if (!personal && untouched) localName!! else label.trim().replace(Regex("\\s+"), " ")
            val key = "category:${normalized(display)}"
            return categoryLabels.getOrPut(key) { preferredLabels[normalized(display)] ?: display }
        }

        fun add(owner: String, type: TransactionType, converted: Long, category: String?, personal: Boolean = false) {
            val previous = totals[owner] ?: FamilyOwnerTotals()
            totals[owner] = when (type) {
                TransactionType.INCOME -> previous.copy(incomeMinorUnits = Math.addExact(previous.incomeMinorUnits, converted))
                TransactionType.EXPENSE -> {
                    val label = category?.takeIf(String::isNotBlank) ?: uncategorizedLabel
                    val ownerCategories = categoryTotals.getOrPut(owner) { mutableMapOf() }
                    ownerCategories[label] = Math.addExact(ownerCategories[label] ?: 0L, converted)
                    val combined = combinedLabel(label, personal)
                    combinedCategories[combined] = Math.addExact(combinedCategories[combined] ?: 0L, converted)
                    previous.copy(expensesMinorUnits = Math.addExact(previous.expensesMinorUnits, converted))
                }
            }
        }

        uniqueImportedRows.forEach { row ->
            val savedRate = row.importRate ?: BigDecimal.ONE
            val rebaseRate = if (row.importBaseCurrency == currentBase) BigDecimal.ONE
            else rates.getValue(row.importBaseCurrency)
            val converted = BigDecimal.valueOf(row.sourceMoney.minorUnits)
                .multiply(savedRate)
                .multiply(rebaseRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
            add(row.owner, row.type, converted, row.categoryName)
        }
        personalTransactions.forEach { transaction ->
            val conversionRate = if (transaction.money.currencyCode == currentBase) BigDecimal.ONE
            else rates.getValue(transaction.money.currencyCode)
            val converted = BigDecimal.valueOf(transaction.money.minorUnits)
                .multiply(conversionRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
            add(
                transaction.owner,
                transaction.type,
                converted,
                transaction.categoryId?.let(categoryNamesById::get) ?: uncategorizedLabel,
                personal = true,
            )
        }
        return FamilyReportTotals(
            incomeMinorUnits = totals.values.fold(0L) { a, b -> Math.addExact(a, b.incomeMinorUnits) },
            expensesMinorUnits = totals.values.fold(0L) { a, b -> Math.addExact(a, b.expensesMinorUnits) },
            byOwner = totals.toSortedMap(),
            expensesByOwnerAndCategory = categoryTotals.toSortedMap().mapValues { (_, categories) -> categories.toSortedMap() },
            expensesByCategory = combinedCategories.toSortedMap(),
        )
    }

    /** Prefer the device's canonical personal row when the same CSV record is also imported. */
    private fun withoutPersonalDuplicates(
        importedRows: List<FamilyTransaction>,
        personalTransactions: List<FinanceTransaction>,
    ): List<FamilyTransaction> {
        val personalRecordIds = personalTransactions.asSequence().map(FinanceTransaction::id).toSet()
        return importedRows.filterNot { it.id in personalRecordIds }
    }
}
