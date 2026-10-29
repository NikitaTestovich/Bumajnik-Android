package com.personalfinance.tracker

import android.app.Application
import androidx.room.Room
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.local.MIGRATION_1_2
import com.personalfinance.tracker.data.local.MIGRATION_2_3
import com.personalfinance.tracker.data.repository.FamilyRepository
import com.personalfinance.tracker.data.repository.FinanceRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.data.repository.RoomFamilyRepository
import com.personalfinance.tracker.data.repository.RoomFinanceRepository
import com.personalfinance.tracker.data.repository.RoomRateRepository

data class FinanceContainer(
    val database: AppDatabase,
    val financeRepository: FinanceRepository,
    val familyRepository: FamilyRepository,
    val rateRepository: RateRepository,
)

class FinanceApplication : Application() {
    lateinit var appLock: com.personalfinance.tracker.security.AppLockController
        private set
    lateinit var container: FinanceContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appLock = com.personalfinance.tracker.security.AppLockController(
            com.personalfinance.tracker.security.AppLockStore(this),
        )
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        container = FinanceContainer(
            database = database,
            financeRepository = RoomFinanceRepository(database),
            familyRepository = RoomFamilyRepository(database),
            rateRepository = RoomRateRepository(database),
        )
    }

    private companion object {
        const val DATABASE_NAME = "finance_tracker.db"
    }
}
