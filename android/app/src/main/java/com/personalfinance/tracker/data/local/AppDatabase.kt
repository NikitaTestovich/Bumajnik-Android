package com.personalfinance.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        FinanceTransactionEntity::class,
        FamilyTransactionEntity::class,
        FamilyImportBatchEntity::class,
        ExchangeRateEntity::class,
        SavingsGoalEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun familyDao(): FamilyDao
    abstract fun exchangeRateDao(): ExchangeRateDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE family_transactions ADD COLUMN importBaseCurrency TEXT NOT NULL DEFAULT 'USD'")
        database.execSQL(
            "UPDATE family_transactions SET importBaseCurrency = COALESCE((SELECT baseCurrency FROM user_profile WHERE id = 1), 'USD')",
        )
    }
}
