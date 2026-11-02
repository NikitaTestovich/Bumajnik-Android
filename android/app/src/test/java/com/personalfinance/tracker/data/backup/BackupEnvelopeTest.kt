package com.personalfinance.tracker.data.backup

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupEnvelopeTest {
    private val files = mapOf("database.bin" to "database".toByteArray(), "preferences.bin" to "preferences".toByteArray())

    @Test fun backupRoundTripPreservesBothFiles() {
        val result = BackupEnvelope.decode(BackupEnvelope.encode(files))
        assertEquals(files.keys, result.keys)
        files.forEach { (name, bytes) -> assertArrayEquals(bytes, result.getValue(name)) }
    }

    @Test fun rejectMissingOrDamagedFiles() {
        assertTrue(runCatching { BackupEnvelope.encode(mapOf("database.bin" to byteArrayOf(1))) }.isFailure)
        val damaged = BackupEnvelope.encode(files).copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x7f).toByte() }
        assertTrue(runCatching { BackupEnvelope.decode(damaged) }.isFailure)
    }
    @Test fun rejectsOversizedInputBeforeDecoding() {
        assertArrayEquals(byteArrayOf(1, 2), readBackupBytes(ByteArrayInputStream(byteArrayOf(1, 2)), 2))
        assertTrue(runCatching { readBackupBytes(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 2) }.isFailure)
    }
}
