package com.personalfinance.tracker.update

import java.net.URL
import java.io.ByteArrayOutputStream
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

sealed interface ReleaseCheckResult {
    data object NoRelease : ReleaseCheckResult
    data object NoCompatibleUpdate : ReleaseCheckResult
    data object Failed : ReleaseCheckResult
    data class Available(val versionName: String, val asset: SelectedReleaseAsset, val releaseNotes: String = "") : ReleaseCheckResult
}
class GitHubReleaseChecker {
    suspend fun check(installedVersionCode: Long): ReleaseCheckResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL("https://api.github.com/repos/NikitaTestovich/AppAndroidBudjets/releases/latest").openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                when (connection.responseCode) {
                    404 -> ReleaseCheckResult.NoRelease
                    200 -> {
                        val bytes = connection.inputStream.use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 1_048_576)
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        val json = JSONObject(bytes.toString(Charsets.UTF_8))
                        require(!json.optBoolean("draft") && !json.optBoolean("prerelease"))
                        val list = json.getJSONArray("assets")
                        val assets = (0 until list.length()).map { index -> list.getJSONObject(index).let {
                            ReleaseAsset(it.optString("name"), it.optString("browser_download_url"), it.optString("digest"))
                        } }
                        val selected = chooseReleaseAsset(assets, installedVersionCode)
                        if (selected == null) ReleaseCheckResult.NoCompatibleUpdate
                        else ReleaseCheckResult.Available(json.optString("tag_name"), selected, json.optString("body", ""))
                    }
                    else -> ReleaseCheckResult.Failed
                }
            } finally { connection.disconnect() }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { ReleaseCheckResult.Failed }
    }
}
