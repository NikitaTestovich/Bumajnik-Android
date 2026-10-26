package com.personalfinance.tracker.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.provider.Settings
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

data class StoredLock(
    val digest: CodeDigest,
    val biometrics: Boolean,
    val retry: RetryState = RetryState(),
    val retryDeadlineElapsed: Long = 0,
    val retryBootId: Int = -1,
)

interface LockStorage {
    val enabled: Boolean
    val bootId: Int get() = 0
    fun read(): StoredLock?
    fun write(record: StoredLock)
    fun clear()
}

/** Only a salted verifier is persisted, encrypted with a non-exportable Android Keystore key. */
class AppLockStore(context: Context) : LockStorage {
    private val preferences = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
    override val bootId: Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
    override val enabled get() = preferences.getBoolean("enabled", false) || preferences.contains("record")

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        check(create) { "Lock key unavailable" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }

    override fun read(): StoredLock? {
        if (!enabled) return null
        val packed = checkNotNull(preferences.getString("record", null)).split(":")
        check(packed.size == 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, decode(packed[0])))
        val json = JSONObject(String(cipher.doFinal(decode(packed[1])), Charsets.UTF_8))
        return StoredLock(
            CodeDigest(decode(json.getString("salt")), decode(json.getString("hash")), json.getInt("iterations")),
            json.getBoolean("biometrics"),
            RetryState(json.getInt("failures"), json.getLong("blockedUntil")),
            json.optLong("retryDeadlineElapsed", 0), json.optInt("retryBootId", -1),
        ).also {
            check(it.digest.iterations == 210_000 && it.digest.salt.size == 32 && it.digest.hash.size == 32)
            check(it.retry.failures in 0..5)
        }
    }

    override fun write(record: StoredLock) {
        val json = JSONObject().put("salt", encode(record.digest.salt)).put("hash", encode(record.digest.hash))
            .put("iterations", record.digest.iterations).put("biometrics", record.biometrics)
            .put("failures", record.retry.failures).put("blockedUntil", record.retry.blockedUntil)
            .put("retryDeadlineElapsed", record.retryDeadlineElapsed).put("retryBootId", record.retryBootId)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true))
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        check(preferences.edit().putBoolean("enabled", true)
            .putString("record", "${encode(cipher.iv)}:${encode(encrypted)}").commit()) { "Cannot save application lock" }
    }

    override fun clear() { check(preferences.edit().clear().commit()) { "Cannot clear application lock" } }
    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String) = Base64.getDecoder().decode(text)
    private companion object { const val KEY_ALIAS = "finance_tracker_app_lock_v1" }
}
