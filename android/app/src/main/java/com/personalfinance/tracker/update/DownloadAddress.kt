package com.personalfinance.tracker.update

import java.net.URI

fun isTrustedDownloadAddress(address: String): Boolean {
    val uri = runCatching { URI(address) }.getOrNull() ?: return false
    return uri.scheme == "https" && uri.host in setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com") &&
        uri.userInfo == null && uri.port == -1 && uri.fragment == null
}
