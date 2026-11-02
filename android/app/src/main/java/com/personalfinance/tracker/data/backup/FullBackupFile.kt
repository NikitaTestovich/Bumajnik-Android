package com.personalfinance.tracker.data.backup

object FullBackupFile {
    fun encode(snapshot: FullBackupSnapshot, preferences: BackupPreferences): ByteArray = BackupEnvelope.encode(mapOf(
        "database.bin" to FullBackupCodec.encode(snapshot),
        "preferences.bin" to BackupPreferencesCodec.encode(preferences),
    ))

    fun decode(bytes: ByteArray): Pair<FullBackupSnapshot, BackupPreferences> {
        val files = BackupEnvelope.decode(bytes)
        return FullBackupCodec.decode(files.getValue("database.bin")) to BackupPreferencesCodec.decode(files.getValue("preferences.bin"))
    }
}
