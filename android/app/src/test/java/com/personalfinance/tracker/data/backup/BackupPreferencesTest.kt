package com.personalfinance.tracker.data.backup

import org.junit.Assert.*
import org.junit.Test

class BackupPreferencesTest {
    @Test fun preservesThemeTabLayoutAndLabels() {
        val prefs = BackupPreferences(8, "INCOME", "summary:1;goal:0;future:1", false)
        assertEquals(prefs, BackupPreferencesCodec.decode(BackupPreferencesCodec.encode(prefs)))
    }
    @Test fun rejectsBadValuesAndExtraBytes() {
        assertTrue(runCatching { BackupPreferencesCodec.encode(BackupPreferences(2, "BAD", "", true)) }.isFailure)
        val valid = BackupPreferencesCodec.encode(BackupPreferences(2, "EXPENSE", "summary:1", true))
        assertTrue(runCatching { BackupPreferencesCodec.decode(valid + byteArrayOf(0)) }.isFailure)
    }
}
