package com.personalfinance.tracker.data.csv

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val CSV_MAX_BYTES = 10 * 1024 * 1024

internal fun readCsvBytes(input: InputStream, checkCancellation: () -> Unit = {}): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        checkCancellation()
        val count = input.read(buffer, 0, minOf(buffer.size, CSV_MAX_BYTES + 1 - total))
        if (count < 0) break
        if (count == 0) continue
        total += count
        require(total <= CSV_MAX_BYTES) { "CSV file is too large" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
