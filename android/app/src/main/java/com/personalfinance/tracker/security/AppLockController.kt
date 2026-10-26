package com.personalfinance.tracker.security

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AppLockState(
    val enabled: Boolean = false,
    val locked: Boolean = false,
    val biometrics: Boolean = false,
    val retrySeconds: Int = 0,
    val storageError: Boolean = false,
)

enum class LockResult { SUCCESS, WRONG_CODE, WAIT, INVALID_CODE, UNAVAILABLE }

class AppLockController(
    private val storage: LockStorage,
    private val now: () -> Long = System::currentTimeMillis,
    private val elapsed: () -> Long = SystemClock::elapsedRealtime,
) {
    private val mutex = Mutex()
    private val session = LockSession()
    val instanceId: String = java.util.UUID.randomUUID().toString()
    private var damaged = false
    private var record = try { storage.read() } catch (_: Exception) { damaged = true; null }
    private var enabled = storage.enabled
    // BOOT_COUNT + elapsed realtime survives process restarts without trusting adjustable wall time.
    // Across a reboot, conservatively require at most one fresh 30-second delay.
    private var retryDeadline = record?.takeIf { it.retry.failures >= 5 }?.let {
        if (it.retryBootId == storage.bootId) it.retryDeadlineElapsed.coerceAtMost(elapsed() + 30_000)
        else elapsed() + 30_000
    } ?: 0L
    private val mutableState = MutableStateFlow(snapshot())
    val state = mutableState.asStateFlow()
    var generation: Long = 0
        private set

    private fun remaining() = (retryDeadline - elapsed()).coerceIn(0, 30_000)
    private fun snapshot() = AppLockState(enabled, enabled && session.locked,
        enabled && record?.biometrics == true && !damaged, ((remaining() + 999) / 1000).toInt(), damaged)
    fun refresh() { mutableState.value = snapshot() }
    fun background() { session.background(elapsed()) }
    fun foreground() {
        val wasLocked = session.locked
        session.foreground(elapsed())
        if (!wasLocked && session.locked) generation++
        refresh()
    }
    fun lockNow() { generation++; session.screenOff(); refresh() }

    // A caller leaving the screen must not cancel between a durable write and its in-memory mirror.
    private suspend fun operation(block: suspend () -> LockResult): LockResult =
        mutex.withLock { withContext(NonCancellable) { block() } }

    suspend fun enable(code: String, biometrics: Boolean, deviceSecure: Boolean): LockResult = operation {
        if (enabled || !deviceSecure) return@operation LockResult.UNAVAILABLE
        if (!CodeVerifier.isValid(code)) return@operation LockResult.INVALID_CODE
        val epoch = generation
        persist {
            val next = StoredLock(withContext(Dispatchers.Default) { CodeVerifier.create(code) }, biometrics)
            withContext(Dispatchers.IO) { storage.write(next) }
            record = next; enabled = true; damaged = false; retryDeadline = 0
            if (epoch == generation) session.unlock() else session.screenOff()
        }
    }

    private suspend fun verify(code: String): LockResult {
        if (!enabled || damaged) return LockResult.UNAVAILABLE
        if (remaining() > 0) return LockResult.WAIT
        val current = record ?: return LockResult.UNAVAILABLE
        val epoch = generation
        val valid = withContext(Dispatchers.Default) { CodeVerifier.verify(code, current.digest) }
        if (epoch != generation) return LockResult.UNAVAILABLE
        if (valid) return LockResult.SUCCESS
        // Use the monotonic deadline while running; timestamps only bridge process restarts.
        val prior = if (current.retry.blockedUntil != 0L) RetryState() else current.retry
        val retry = prior.failed(now())
        val deadline = if (retry.failures >= 5) elapsed() + 30_000 else 0L
        val next = current.copy(retry = retry, retryDeadlineElapsed = deadline, retryBootId = storage.bootId)
        withContext(Dispatchers.IO) { storage.write(next) }
        record = next; retryDeadline = deadline; refresh()
        return if (remaining() > 0) LockResult.WAIT else LockResult.WRONG_CODE
    }

    suspend fun unlock(code: String): LockResult = operation {
        safely {
            val result = verify(code)
            if (result == LockResult.SUCCESS) authorize()
            result
        }
    }

    suspend fun biometricSucceeded(expectedGeneration: Long): LockResult = operation {
        if (!enabled || record?.biometrics != true || expectedGeneration != generation || damaged)
            return@operation LockResult.UNAVAILABLE
        persist { authorize() }
    }

    private suspend fun authorize() {
        val epoch = generation
        val next = checkNotNull(record).copy(retry = RetryState(), retryDeadlineElapsed = 0, retryBootId = -1)
        withContext(Dispatchers.IO) { storage.write(next) }
        record = next; retryDeadline = 0
        if (epoch == generation) session.unlock()
        refresh()
    }

    suspend fun update(code: String, newCode: String?, biometrics: Boolean): LockResult = operation {
        if (state.value.locked) return@operation LockResult.UNAVAILABLE
        if (newCode != null && !CodeVerifier.isValid(newCode)) return@operation LockResult.INVALID_CODE
        safely {
            val result = verify(code)
            if (result != LockResult.SUCCESS) return@safely result
            val epoch = generation
            val digest = if (newCode == null) checkNotNull(record).digest
                else withContext(Dispatchers.Default) { CodeVerifier.create(newCode) }
            if (epoch != generation) return@safely LockResult.UNAVAILABLE
            val next = StoredLock(digest, biometrics)
            withContext(Dispatchers.IO) { storage.write(next) }
            record = next; retryDeadline = 0; refresh()
            LockResult.SUCCESS
        }
    }

    suspend fun disable(code: String): LockResult = operation {
        if (state.value.locked) return@operation LockResult.UNAVAILABLE
        safely {
            val result = verify(code)
            if (result == LockResult.SUCCESS) clear()
            result
        }
    }

    /** Called only after Android confirms the device credential, never from a UI confirmation alone. */
    suspend fun recoverAfterDeviceCredential(expectedGeneration: Long): LockResult = operation {
        if (expectedGeneration != generation) return@operation LockResult.UNAVAILABLE
        persist { clear() }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) { storage.clear() }
        record = null; enabled = false; damaged = false; retryDeadline = 0
        generation++; session.unlock(); refresh()
    }

    private suspend fun persist(block: suspend () -> Unit): LockResult = safely { block(); refresh(); LockResult.SUCCESS }
    private suspend fun safely(block: suspend () -> LockResult): LockResult = try { block() }
    catch (error: kotlinx.coroutines.CancellationException) { throw error }
    catch (_: Exception) {
        damaged = true
        enabled = enabled || storage.enabled
        if (enabled) session.screenOff()
        refresh()
        LockResult.UNAVAILABLE
    }
}
