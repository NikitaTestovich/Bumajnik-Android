package com.personalfinance.tracker.data.backup

import android.content.Context
import android.net.Uri
import com.personalfinance.tracker.FinanceApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Encrypts the portable snapshot with a user-supplied password. */
suspend fun exportFullBackup(context: Context, destination: Uri, password: CharArray) = withContext(Dispatchers.IO) {
    val app = context.applicationContext as FinanceApplication
    val appearance = app.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    val operations = app.getSharedPreferences("operations", Context.MODE_PRIVATE)
    val home = app.getSharedPreferences("home_layout", Context.MODE_PRIVATE)
    val preferences = BackupPreferences(
        appearance.getInt("theme_id", 2),
        operations.getString("selected_type", "EXPENSE") ?: "EXPENSE",
        home.getString("blocks", "") ?: "",
        appearance.getBoolean("category_icon_labels", true),
    )
    val bytes = EncryptedBackup.encrypt(FullBackupFile.encode(captureFullBackup(app.container.database), preferences), password)
    val stream = checkNotNull(context.contentResolver.openOutputStream(destination, "wt"))
    stream.use { it.write(bytes) }
}
