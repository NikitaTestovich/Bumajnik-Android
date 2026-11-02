package com.personalfinance.tracker.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream

fun readBackupBytes(input: InputStream, maxBytes: Int = 100 * 1024 * 1024 + 1024): ByteArray {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(count <= maxBytes - output.size())
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
