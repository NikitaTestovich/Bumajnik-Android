package com.personalfinance.tracker.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.local.CategoryEntity
import com.personalfinance.tracker.data.local.ExchangeRateEntity
import com.personalfinance.tracker.data.local.FamilyImportBatchEntity
import com.personalfinance.tracker.data.local.FinanceTransactionEntity
import com.personalfinance.tracker.data.local.MIGRATION_1_2
import com.personalfinance.tracker.data.local.MIGRATION_2_3
import com.personalfinance.tracker.data.local.SavingsGoalEntity
import com.personalfinance.tracker.data.local.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@Entity(
    tableName = "family_transactions",
    indices = [Index("date"), Index("type"), Index("currencyCode"), Index("owner"), Index("batchId")],
)
data class LegacyFamilyTransactionEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val type: String,
    val date: String,
    val minorUnits: Long,
    val currencyCode: String,
    val categoryName: String?,
    val importRate: String?,
    val batchId: String?,
)

@Entity(tableName = "categories")
data class LegacyCategoryEntity(@PrimaryKey val id: String, val nameRu: String?, val nameEn: String?, val isCustom: Boolean)
@Entity(tableName = "finance_transactions", indices = [Index("date"), Index("type"), Index("currencyCode"), Index("owner")])
data class LegacyPersonalEntity(@PrimaryKey val id: String, val owner: String, val type: String, val date: String, val minorUnits: Long, val currencyCode: String, val categoryId: String?)

@Dao
interface LegacyMigrationDao

@Database(
    entities = [
        UserProfileEntity::class,
        LegacyCategoryEntity::class,
        LegacyPersonalEntity::class,
        LegacyFamilyTransactionEntity::class,
        FamilyImportBatchEntity::class,
        ExchangeRateEntity::class,
        SavingsGoalEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LegacyFinanceDatabase : RoomDatabase() {
    abstract fun migrationDao(): LegacyMigrationDao
}

@Entity(tableName = "family_transactions", indices = [Index("date"), Index("type"), Index("currencyCode"), Index("owner"), Index("batchId")])
data class VersionTwoFamilyEntity(@PrimaryKey val id: String, val owner: String, val type: String, val date: String, val minorUnits: Long, val currencyCode: String, val categoryName: String?, val importRate: String?, val importBaseCurrency: String, val batchId: String?)
@Database(entities = [UserProfileEntity::class, LegacyCategoryEntity::class, LegacyPersonalEntity::class, VersionTwoFamilyEntity::class, FamilyImportBatchEntity::class, ExchangeRateEntity::class, SavingsGoalEntity::class], version = 2, exportSchema = false)
abstract class VersionTwoDatabase : RoomDatabase() { abstract fun migrationDao(): LegacyMigrationDao }

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test
    fun populatedVersionTwoPreservesValuesAndBackfillsMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-v2-test.db"
        context.deleteDatabase(name)
        val legacy = Room.databaseBuilder(context, VersionTwoDatabase::class.java, name).build()
        legacy.openHelper.writableDatabase.apply {
            execSQL("INSERT INTO user_profile VALUES (1, 'Alex', 'en', 'BYN', 1)")
            execSQL("INSERT INTO savings_goal VALUES (1, 10000, 'USD', 2500, 'USD')")
            execSQL("INSERT INTO family_import_batches VALUES ('batch', 123456, 1)")
            execSQL("INSERT INTO period_exchange_rates VALUES ('2026-01-01', '2026-02-01', 'BYN', 'EUR', '3.25')")
            execSQL("INSERT INTO categories VALUES ('housing_utilities', 'Home RU', 'Home', 0)")
            execSQL("INSERT INTO categories VALUES ('custom', NULL, 'Custom', 1)")
            execSQL("INSERT INTO finance_transactions VALUES ('record', 'Alex', 'EXPENSE', '2025-12-31', 1234, 'EUR', 'housing_utilities')")
            execSQL("INSERT INTO family_transactions VALUES ('family', 'Family', 'EXPENSE', '2026-01-01', 500, 'EUR', 'Home', '1.08', 'BYN', 'batch')")
        }
        legacy.close()
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        try {
            upgraded.openHelper.readableDatabase.query("SELECT owner, type, date, minorUnits, currencyCode, categoryId, comment, createdOrder FROM finance_transactions").use {
                it.moveToFirst()
                assertEquals(listOf("Alex", "EXPENSE", "2025-12-31", "1234", "EUR", "housing_utilities", "", "0"), (0..7).map { i -> it.getString(i) })
            }
            upgraded.openHelper.readableDatabase.query("SELECT nameRu, nameEn, isCustom, isArchived, iconKey FROM categories WHERE id = 'housing_utilities'").use {
                it.moveToFirst()
                assertEquals(listOf("Home RU", "Home", "0", "0", "home"), (0..4).map { i -> it.getString(i) })
            }
            upgraded.openHelper.readableDatabase.query("SELECT owner, type, date, minorUnits, currencyCode, categoryName, importRate, importBaseCurrency, batchId, comment FROM family_transactions").use {
                it.moveToFirst()
                assertEquals(listOf("Family", "EXPENSE", "2026-01-01", "500", "EUR", "Home", "1.08", "BYN", "batch", ""), (0..9).map { i -> it.getString(i) })
            }
            val preserved = mapOf(
                "SELECT name, languageTag, baseCurrency, onboardingComplete FROM user_profile" to listOf("Alex", "en", "BYN", "1"),
                "SELECT targetMinorUnits, targetCurrency, savedMinorUnits, savedCurrency FROM savings_goal" to listOf("10000", "USD", "2500", "USD"),
                "SELECT id, importedAtEpochMillis, recordCount FROM family_import_batches" to listOf("batch", "123456", "1"),
                "SELECT rangeStart, rangeEndExclusive, baseCurrency, sourceCurrency, rateToBase FROM period_exchange_rates" to listOf("2026-01-01", "2026-02-01", "BYN", "EUR", "3.25"),
                "SELECT nameRu, nameEn, isCustom, isArchived, iconKey FROM categories WHERE id = 'custom'" to listOf(null, "Custom", "1", "0", "other"),
            )
            preserved.forEach { (query, values) ->
                upgraded.openHelper.readableDatabase.query(query).use {
                    it.moveToFirst()
                    assertEquals(values, values.indices.map { i -> it.getString(i) })
                }
            }
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }

    @Test
    fun migrationBackfillsTheProfileCurrencyForExistingFamilyRows() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-v1-test.db"
        context.deleteDatabase(name)
        val legacy = Room.databaseBuilder(context, LegacyFinanceDatabase::class.java, name).build()
        legacy.openHelper.writableDatabase.apply {
            execSQL("INSERT INTO user_profile (id, name, languageTag, baseCurrency, onboardingComplete) VALUES (1, 'Alex', 'en', 'BYN', 1)")
            execSQL("INSERT INTO family_transactions (id, owner, type, date, minorUnits, currencyCode, categoryName, importRate, batchId) VALUES ('record', 'Wife', 'EXPENSE', '2026-10-01', 500, 'EUR', 'Food', '1.08', NULL)")
        }
        legacy.close()

        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        try {
            val cursor = upgraded.openHelper.readableDatabase.query(
                "SELECT importBaseCurrency, comment FROM family_transactions WHERE id = 'record'",
            )
            cursor.use {
                it.moveToFirst()
                assertEquals("BYN", it.getString(0))
                assertEquals("", it.getString(1))
            }
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
