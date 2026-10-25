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

@Dao
interface LegacyMigrationDao

@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        FinanceTransactionEntity::class,
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

@RunWith(AndroidJUnit4::class)
class MigrationTest {
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
            .addMigrations(MIGRATION_1_2)
            .build()
        try {
            val cursor = upgraded.openHelper.readableDatabase.query(
                "SELECT importBaseCurrency FROM family_transactions WHERE id = 'record'",
            )
            cursor.use {
                it.moveToFirst()
                assertEquals("BYN", it.getString(0))
            }
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
