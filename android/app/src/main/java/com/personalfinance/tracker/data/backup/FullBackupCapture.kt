package com.personalfinance.tracker.data.backup

import androidx.room.withTransaction
import com.personalfinance.tracker.data.local.AppDatabase

suspend fun captureFullBackup(database: AppDatabase): FullBackupSnapshot = database.withTransaction {
    FullBackupSnapshot(
        profile = database.financeDao().backupProfile(),
        categories = database.financeDao().categories(),
        transactions = database.financeDao().backupTransactions(),
        familyTransactions = database.familyDao().backupFamilyTransactions(),
        batches = database.familyDao().backupBatches(),
        rates = database.exchangeRateDao().backupRates(),
        goal = database.financeDao().backupGoal(),
    )
}

/** Destructively replaces financial data; caller must obtain explicit confirmation first. */
suspend fun restoreFullBackup(database: AppDatabase, snapshot: FullBackupSnapshot) {
    validateFullBackup(snapshot)
    database.withTransaction {
        val finance = database.financeDao()
        val family = database.familyDao()
        val rates = database.exchangeRateDao()
        finance.backupClearTransactions(); family.backupClearFamilyTransactions(); family.backupClearBatches()
        finance.backupClearCategories(); finance.backupClearGoal(); finance.backupClearProfile(); rates.backupClearRates()
        snapshot.profile?.let { finance.saveProfile(it) }
        finance.backupInsertCategories(snapshot.categories)
        finance.backupInsertTransactions(snapshot.transactions)
        family.backupInsertBatches(snapshot.batches)
        family.backupInsertFamily(snapshot.familyTransactions)
        rates.insertRates(snapshot.rates)
        snapshot.goal?.let { finance.saveGoal(it) }
    }
}
