package com.personalfinance.tracker.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class CodeDigest(val salt: ByteArray, val hash: ByteArray, val iterations: Int = 210_000)

object CodeVerifier {
    fun isValid(code: String): Boolean = code.codePointCount(0, code.length) in 4..10

    fun create(code: String): CodeDigest {
        require(isValid(code))
        val salt = ByteArray(32).also(SecureRandom()::nextBytes)
        return CodeDigest(salt, derive(code, salt, 210_000))
    }

    fun verify(code: String, digest: CodeDigest): Boolean {
        if (!isValid(code)) return false
        require(digest.iterations == 210_000 && digest.salt.size == 32 && digest.hash.size == 32)
        return MessageDigest.isEqual(digest.hash, derive(code, digest.salt, digest.iterations))
    }

    private fun derive(code: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
