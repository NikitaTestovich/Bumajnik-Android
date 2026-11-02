package com.personalfinance.tracker.update

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class UpdatePayloadTest {
    private val bytes = "sample apk bytes".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test fun copiesOnlyPayloadWithMatchingDigest() {
        val output = ByteArrayOutputStream()
        copyVerifiedPayload(ByteArrayInputStream(bytes), output, hash, 1024)
        assertArrayEquals(bytes, output.toByteArray())
    }
    @Test fun rejectsWrongDigestEmptyFileAndOversizedFile() {
        assertTrue(runCatching { copyVerifiedPayload(ByteArrayInputStream(bytes), ByteArrayOutputStream(), "00".repeat(32), 1024) }.isFailure)
        assertTrue(runCatching { copyVerifiedPayload(ByteArrayInputStream(byteArrayOf()), ByteArrayOutputStream(), hash, 1024) }.isFailure)
        assertTrue(runCatching { copyVerifiedPayload(ByteArrayInputStream(bytes), ByteArrayOutputStream(), hash, 2) }.isFailure)
    }
}
