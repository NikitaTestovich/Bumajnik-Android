package com.personalfinance.tracker.update

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/** Callers must discard the output unless this function returns successfully. */
fun copyVerifiedPayload(input: InputStream, output: OutputStream, expectedSha256: String, maxBytes: Long) {
    require(expectedSha256.matches(Regex("[a-fA-F0-9]{64}")) && maxBytes > 0)
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total = Math.addExact(total, count.toLong())
        require(total <= maxBytes)
        digest.update(buffer, 0, count)
        output.write(buffer, 0, count)
    }
    require(total > 0)
    val actual = digest.digest().joinToString("") { "%02x".format(it) }
    require(actual.equals(expectedSha256, ignoreCase = true))
}
