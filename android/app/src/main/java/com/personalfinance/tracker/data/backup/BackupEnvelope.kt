package com.personalfinance.tracker.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest

/** Detects accidental corruption; this format does not encrypt or authenticate the data. */
object BackupEnvelope {
    private const val MAGIC = 0x46544231
    private const val MAX_FILE = 50 * 1024 * 1024
    private val names = listOf("database.bin", "preferences.bin")

    fun encode(files: Map<String, ByteArray>): ByteArray {
        require(files.keys == names.toSet())
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            stream.writeInt(MAGIC)
            names.forEach { name ->
                val bytes = files.getValue(name)
                require(bytes.isNotEmpty() && bytes.size <= MAX_FILE)
                stream.writeUTF(name)
                stream.writeInt(bytes.size)
                stream.write(MessageDigest.getInstance("SHA-256").digest(bytes))
                stream.write(bytes)
            }
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): Map<String, ByteArray> {
        require(bytes.size <= 2 * MAX_FILE + 1024)
        val stream = DataInputStream(ByteArrayInputStream(bytes))
        require(stream.readInt() == MAGIC)
        val result = linkedMapOf<String, ByteArray>()
        names.forEach { expected ->
            require(stream.readUTF() == expected)
            val size = stream.readInt()
            require(size in 1..MAX_FILE && size <= stream.available() - 32)
            val hash = ByteArray(32).also(stream::readFully)
            val contents = ByteArray(size).also(stream::readFully)
            require(MessageDigest.isEqual(hash, MessageDigest.getInstance("SHA-256").digest(contents)))
            result[expected] = contents
        }
        require(stream.available() == 0)
        return result
    }
}
