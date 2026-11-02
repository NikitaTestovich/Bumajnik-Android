package com.personalfinance.tracker.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/** Reads archive metadata. Android's package installer performs final signature verification. */
class ApkIdentityReader(private val context: Context) {
    @Suppress("DEPRECATION")
    private val flags: Int = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    fun installed(): UpdateIdentity = identity(context.packageManager.getPackageInfo(context.packageName, flags))

    fun archive(file: File): UpdateIdentity {
        @Suppress("DEPRECATION")
        val info = requireNotNull(context.packageManager.getPackageArchiveInfo(file.absolutePath, flags))
        return identity(info)
    }

    @Suppress("DEPRECATION")
    private fun identity(info: PackageInfo): UpdateIdentity {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            val signing = requireNotNull(info.signingInfo)
            require(!signing.hasMultipleSigners())
            signing.apkContentsSigners
        } else info.signatures
        val signature = requireNotNull(signatures).single()
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val version = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return UpdateIdentity(info.packageName, version, digest)
    }
}
