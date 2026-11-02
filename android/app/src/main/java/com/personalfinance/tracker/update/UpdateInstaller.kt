package com.personalfinance.tracker.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateInstaller(private val context: Context) {
    suspend fun prepare(asset: SelectedReleaseAsset): Intent = withContext(Dispatchers.IO) {
        require(chooseReleaseAsset(listOf(asset.asset), 0) == asset)
        val dir = File(context.cacheDir, "updates")
        check(dir.isDirectory || dir.mkdirs())
        val file = File.createTempFile("update-", ".apk", dir)
        try {
            UpdateDownloader().download(asset.asset.url, asset.sha256, file)
            val reader = ApkIdentityReader(context)
            val candidate = reader.archive(file)
            val hash = file.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            require(candidate.versionCode == asset.versionCode)
            require(isAcceptableUpdate(reader.installed(), candidate, asset.sha256, hash))
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }
}
