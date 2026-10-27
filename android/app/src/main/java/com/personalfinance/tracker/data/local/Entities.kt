package com.personalfinance.tracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val name: String,
    val languageTag: String,
    val baseCurrency: String,
    val onboardingComplete: Boolean,
) {
    companion object { const val SINGLETON_ID = 1 }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameRu: String?,
    val nameEn: String?,
    val isCustom: Boolean,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "'other'") val iconKey: String = "other",
)

@Entity(
    tableName = "finance_transactions",
    indices = [Index("date"), Index("type"), Index("currencyCode"), Index("owner")],
)
data class FinanceTransactionEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val type: String,
    val date: String,
    val minorUnits: Long,
    val currencyCode: String,
    val categoryId: String?,
    @ColumnInfo(defaultValue = "''") val comment: String = "",
    @ColumnInfo(defaultValue = "0") val createdOrder: Long = 0,
)

@Entity(
    tableName = "family_transactions",
    indices = [Index("date"), Index("type"), Index("currencyCode"), Index("owner"), Index("batchId")],
)
data class FamilyTransactionEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val type: String,
    val date: String,
    val minorUnits: Long,
    val currencyCode: String,
    val categoryName: String?,
    val importRate: String?,
    val importBaseCurrency: String = "USD",
    val batchId: String?,
    @ColumnInfo(defaultValue = "''") val comment: String = "",
)

@Entity(tableName = "family_import_batches")
data class FamilyImportBatchEntity(
    @PrimaryKey val id: String,
    val importedAtEpochMillis: Long,
    val recordCount: Int,
)

@Entity(
    tableName = "period_exchange_rates",
    primaryKeys = ["rangeStart", "rangeEndExclusive", "baseCurrency", "sourceCurrency"],
    indices = [Index("baseCurrency"), Index("sourceCurrency")],
)
data class ExchangeRateEntity(
    val rangeStart: String,
    val rangeEndExclusive: String,
    val baseCurrency: String,
    val sourceCurrency: String,
    val rateToBase: String,
)

@Entity(tableName = "savings_goal")
data class SavingsGoalEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val targetMinorUnits: Long,
    val targetCurrency: String,
    val savedMinorUnits: Long,
    val savedCurrency: String,
) {
    companion object { const val SINGLETON_ID = 1 }
}
