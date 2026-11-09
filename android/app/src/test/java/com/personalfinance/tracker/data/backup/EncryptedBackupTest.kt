package com.personalfinance.tracker.data.backup

import org.junit.Assert.*
import org.junit.Test

class EncryptedBackupTest {
    private val password = "пароль secure".toCharArray()
    @Test fun encryptedRoundTripAndWrongPassword() {
        val plaintext = "financial secret".toByteArray()
        val encoded = EncryptedBackup.encrypt(plaintext, password)
        assertFalse(encoded.toString(Charsets.ISO_8859_1).contains("financial secret"))
        assertArrayEquals(plaintext, EncryptedBackup.decrypt(encoded, password))
        assertTrue(runCatching { EncryptedBackup.decrypt(encoded, "wrong".toCharArray()) }.isFailure)
    }
    @Test fun randomSaltAndNonceProduceDifferentFiles() {
        val data = byteArrayOf(1, 2, 3)
        assertFalse(EncryptedBackup.encrypt(data, password).contentEquals(EncryptedBackup.encrypt(data, password)))
    }
    @Test fun damagedHeaderCiphertextAndTagAreRejected() {
        val encoded = EncryptedBackup.encrypt("secret".toByteArray(), password)
        for (offset in listOf(0, 4, 8, 24, 36, encoded.lastIndex)) {
            val damaged = encoded.copyOf().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
            assertTrue("offset $offset", runCatching { EncryptedBackup.decrypt(damaged, password) }.isFailure)
        }
        assertTrue(runCatching { EncryptedBackup.decrypt(encoded.copyOf(encoded.size - 1), password) }.isFailure)
        assertTrue(runCatching { EncryptedBackup.decrypt(encoded + byteArrayOf(0), password) }.isFailure)
    }
}
