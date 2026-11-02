package com.personalfinance.tracker.update

import java.net.URI

data class ReleaseAsset(val name: String, val url: String, val digest: String)
data class SelectedReleaseAsset(val asset: ReleaseAsset, val versionCode: Long, val sha256: String)
internal const val RELEASE_REPOSITORY = "NikitaTestovich/Bumajnik-Android"

fun chooseReleaseAsset(assets: List<ReleaseAsset>, installedVersionCode: Long): SelectedReleaseAsset? =
    assets.mapNotNull { asset ->
        val version = Regex("finance-tracker-vc([1-9][0-9]*)\\.apk").matchEntire(asset.name)?.groupValues?.get(1)?.toLongOrNull()
            ?: return@mapNotNull null
        val uri = runCatching { URI(asset.url) }.getOrNull() ?: return@mapNotNull null
        val sha = asset.digest.removePrefix("sha256:")
        if (version <= installedVersionCode || uri.scheme != "https" || uri.host != "github.com" || uri.port != -1 ||
            uri.userInfo != null || uri.query != null || uri.fragment != null ||
            !uri.path.startsWith("/$RELEASE_REPOSITORY/releases/download/") ||
            !uri.path.endsWith("/" + asset.name) || !asset.digest.startsWith("sha256:") ||
            !sha.matches(Regex("[a-fA-F0-9]{64}"))) return@mapNotNull null
        SelectedReleaseAsset(asset, version, sha.lowercase())
    }.maxByOrNull { it.versionCode }
