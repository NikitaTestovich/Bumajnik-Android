package com.personalfinance.tracker.update

data class UpdateIdentity(val packageName: String, val versionCode: Long, val certificateSha256: String)

fun isAcceptableUpdate(installed: UpdateIdentity, candidate: UpdateIdentity, expectedDigest: String, actualDigest: String): Boolean {
    fun normalized(value: String) = value.replace(":", "").lowercase()
    fun validHash(value: String) = value.matches(Regex("[a-f0-9]{64}"))
    val installedCertificate = normalized(installed.certificateSha256)
    val candidateCertificate = normalized(candidate.certificateSha256)
    val expected = normalized(expectedDigest)
    val actual = normalized(actualDigest)
    return installed.packageName.isNotBlank() && installed.packageName == candidate.packageName &&
        candidate.versionCode > installed.versionCode && validHash(installedCertificate) &&
        installedCertificate == candidateCertificate && validHash(expected) && expected == actual
}
