package com.personalfinance.tracker.update

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateDownloader(private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }) {
    /** Caller supplies a dedicated temporary path, never an existing user file. */
    fun download(address: String, sha256: String, destination: File) {
        try {
            var current = URL(address)
            repeat(6) {
                require(isTrustedDownloadAddress(current.toString()))
                val connection = open(current)
                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    when (connection.responseCode) {
                        200 -> {
                            connection.inputStream.use { input ->
                                destination.outputStream().use { output ->
                                    copyVerifiedPayload(input, output, sha256, 150L * 1024 * 1024)
                                }
                            }
                            return
                        }
                        301, 302, 303, 307, 308 -> {
                            val location = requireNotNull(connection.getHeaderField("Location"))
                            current = URL(current, location)
                        }
                        else -> error("Download failed")
                    }
                } finally { connection.disconnect() }
            }
            error("Too many redirects")
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }
}
