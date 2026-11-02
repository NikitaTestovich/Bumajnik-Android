package com.personalfinance.tracker.update

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class UpdateDownloaderTest {
    private val payload = "sample apk".toByteArray()
    private val sha = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
    private val start = "https://github.com/NikitaTestovich/AppAndroidBudjets/releases/download/v1/finance-tracker-vc5.apk"
    private val target = "https://release-assets.githubusercontent.com/example?token=1"

    @Test fun followsTrustedRedirectAndVerifiesFile() {
        val file = kotlin.io.path.createTempFile().toFile()
        try {
            val downloader = UpdateDownloader { url -> FakeConnection(url, if (url.host == "github.com") 302 else 200, target, payload) }
            downloader.download(start, sha, file)
            assertArrayEquals(payload, file.readBytes())
        } finally { file.delete() }
    }

    @Test fun rejectsUntrustedRedirectAndDeletesPartialFile() {
        val file = kotlin.io.path.createTempFile().toFile()
        val downloader = UpdateDownloader { url -> FakeConnection(url, 302, "https://evil.example/file", payload) }
        assertTrue(runCatching { downloader.download(start, sha, file) }.isFailure)
        assertFalse(file.exists())
    }

    @Test fun rejectsWrongDigestAndDeletesPartialFile() {
        val file = kotlin.io.path.createTempFile().toFile()
        val downloader = UpdateDownloader { url -> FakeConnection(url, 200, null, payload) }
        assertTrue(runCatching { downloader.download(start, "00".repeat(32), file) }.isFailure)
        assertFalse(file.exists())
    }

    @Test fun networkFailureRemovesTemporaryDownload() {
        val file = kotlin.io.path.createTempFile().toFile()
        val downloader = UpdateDownloader { throw IOException("offline") }
        assertTrue(runCatching { downloader.download(start, sha, file) }.isFailure)
        assertFalse(file.exists())
    }

    @Test fun redirectLoopStopsAndRemovesTemporaryDownload() {
        val file = kotlin.io.path.createTempFile().toFile()
        var requests = 0
        val downloader = UpdateDownloader { url ->
            requests++
            FakeConnection(url, 302, start, payload)
        }
        assertTrue(runCatching { downloader.download(start, sha, file) }.isFailure)
        assertEquals(6, requests)
        assertFalse(file.exists())
    }

    private class FakeConnection(url: URL, private val code: Int, private val location: String?, private val data: ByteArray) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = code
        override fun getHeaderField(name: String?): String? = if (name == "Location") location else null
        override fun getInputStream() = ByteArrayInputStream(data)
    }
}
