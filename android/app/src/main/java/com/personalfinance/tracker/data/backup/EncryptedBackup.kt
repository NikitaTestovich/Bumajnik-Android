package com.personalfinance.tracker.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable authenticated envelope. FTB2, fixed PBKDF2-SHA256 parameters, AES-256-GCM. */
object EncryptedBackup {
    private const val MAGIC = 0x46544232
    private const val ITERATIONS = 600_000
    private const val HEADER_SIZE = 4 + 4 + 16 + 12
    private const val MAX_SIZE = 100 * 1024 * 1024 + 2048
    private val random = SecureRandom()

    fun isEncrypted(bytes: ByteArray): Boolean = bytes.size >= 4 && ByteBuffer.wrap(bytes).int == MAGIC

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty() && plaintext.isNotEmpty() && plaintext.size <= MAX_SIZE - HEADER_SIZE - 16)
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_SIZE).putInt(MAGIC).putInt(ITERATIONS).put(salt).put(nonce).array()
        return header + crypt(Cipher.ENCRYPT_MODE, plaintext, password, salt, nonce, header)
    }

    fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty() && bytes.size in (HEADER_SIZE + 16)..MAX_SIZE)
        val input = ByteBuffer.wrap(bytes)
        require(input.int == MAGIC && input.int == ITERATIONS)
        val salt = ByteArray(16).also(input::get)
        val nonce = ByteArray(12).also(input::get)
        val header = bytes.copyOfRange(0, HEADER_SIZE)
        return crypt(Cipher.DECRYPT_MODE, bytes.copyOfRange(HEADER_SIZE, bytes.size), password, salt, nonce, header)
    }

    private fun crypt(mode: Int, data: ByteArray, password: CharArray, salt: ByteArray, nonce: ByteArray, header: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        val key = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(header)
            cipher.doFinal(data)
        } finally { key.fill(0) }
    }
}
