package com.personalfinance.tracker.update

import org.junit.Assert.*
import org.junit.Test

class DownloadAddressTest {
    @Test fun acceptsOnlyHttpsGithubDownloadHosts() {
        assertTrue(isTrustedDownloadAddress("https://github.com/NikitaTestovich/AppAndroidBudjets/releases/download/v1/file.apk"))
        assertTrue(isTrustedDownloadAddress("https://release-assets.githubusercontent.com/path?signature=test"))
        assertFalse(isTrustedDownloadAddress("http://release-assets.githubusercontent.com/path"))
        assertFalse(isTrustedDownloadAddress("https://release-assets.githubusercontent.com.evil.example/path"))
        assertFalse(isTrustedDownloadAddress("https://user@github.com/path"))
        assertFalse(isTrustedDownloadAddress("https://github.com:8443/path"))
    }
}
