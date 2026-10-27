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
    version = 3,
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

/** Existing records retain unknown creation order (zero); no historical times are invented. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE finance_transactions ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE finance_transactions ADD COLUMN createdOrder INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE family_transactions ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE categories ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'other'")
        database.execSQL("""
            UPDATE categories SET iconKey = CASE id
                WHEN 'groceries' THEN 'groceries'
                WHEN 'housing_utilities' THEN 'home'
                WHEN 'transport' THEN 'transport'
                WHEN 'communications' THEN 'phone'
                WHEN 'healthcare' THEN 'health'
                WHEN 'education' THEN 'education'
                WHEN 'clothing' THEN 'clothing'
                WHEN 'cafes_restaurants' THEN 'dining'
                WHEN 'entertainment' THEN 'entertainment'
                WHEN 'household_goods' THEN 'household_goods'
                ELSE 'other' END WHERE isCustom = 0
        """.trimIndent())
    }
}
