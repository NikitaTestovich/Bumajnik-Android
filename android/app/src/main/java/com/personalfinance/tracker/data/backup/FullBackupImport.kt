package com.personalfinance.tracker.data.backup

import android.content.Context
import android.net.Uri
import com.personalfinance.tracker.FinanceApplication
import com.personalfinance.tracker.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

suspend fun readFullBackup(context: Context, source: Uri): Pair<FullBackupSnapshot, BackupPreferences> = withContext(Dispatchers.IO) {
    val stream = checkNotNull(context.contentResolver.openInputStream(source))
    FullBackupFile.decode(stream.use { readBackupBytes(it) })
}

/** Replaces financial data; settings and database use separate stores and cannot be committed atomically. */
suspend fun applyFullBackup(context: Context, snapshot: FullBackupSnapshot, preferences: BackupPreferences): Boolean =
    withContext(Dispatchers.IO + NonCancellable) {
        BackupPreferencesCodec.encode(preferences)
        val app = context.applicationContext as FinanceApplication
        restoreFullBackup(app.container.database, snapshot)
        try {
        val appearance = app.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        val home = app.getSharedPreferences("home_layout", Context.MODE_PRIVATE)
        val operations = app.getSharedPreferences("operations", Context.MODE_PRIVATE)
        val appearanceOk = appearance.edit().putInt("theme_id", preferences.themeId)
            .putBoolean("category_icon_labels", preferences.categoryIconLabels).commit()
        val homeOk = home.edit().putString("blocks", preferences.homeBlocks).commit()
        val operationsOk = operations.edit().putString("selected_type", preferences.selectedTab).commit()
        if (appearanceOk && homeOk && operationsOk) {
            app.themeStore.select(preferences.themeId)
            app.operationsTabStore.select(TransactionType.valueOf(preferences.selectedTab))
            true
        } else false
        } catch (_: Exception) { false }
    }
