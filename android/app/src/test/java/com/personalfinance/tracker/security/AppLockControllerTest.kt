package com.personalfinance.tracker.security

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test

class AppLockControllerTest {
    @Test fun canceledBiometricCannotUnlockSameSessionOrRetry() = runBlocking {
        val lock = AppLockController(MemoryStorage(), { 1000 }, { 1000 })
        lock.enable("1234", true, true)
        lock.lockNow()
        lock.biometricEntry.beginSession(lock.generation, true, true)
        val old = lock.biometricEntry.beginAttempt()!!
        lock.biometricEntry.fallback(old)
        val retry = lock.biometricEntry.beginAttempt()!!
        assertEquals(LockResult.UNAVAILABLE, lock.biometricSucceeded(lock.generation, old))
        assertTrue(lock.state.value.locked)
        assertEquals(LockResult.SUCCESS, lock.biometricSucceeded(lock.generation, retry))
        assertFalse(lock.state.value.locked)
    }

    @Test fun backgroundInvalidatesBiometricBeforeTimeout() = runBlocking {
        val lock = AppLockController(MemoryStorage(), { 1000 }, { 1000 })
        lock.enable("1234", true, true)
        lock.lockNow()
        lock.biometricEntry.beginSession(lock.generation, true, true)
        val attempt = lock.biometricEntry.beginAttempt()!!
        lock.background()
        assertEquals(LockResult.UNAVAILABLE, lock.biometricSucceeded(lock.generation, attempt))
        assertTrue(lock.state.value.locked)
    }
    private class MemoryStorage : LockStorage {
        var record: StoredLock? = null
        var afterWrite: () -> Unit = {}
        override val enabled get() = record != null
        override fun read() = record
        override fun write(record: StoredLock) { this.record = record; afterWrite() }
        override fun clear() { record = null }
    }

    @Test fun movingWallClockForwardAndRestartingCannotBypassRetryDelay() = runBlocking {
        val store = MemoryStorage()
        var wall = 1000L
        val lock = AppLockController(store, { wall }, { 1000L })
        lock.enable("1234", false, true)
        lock.lockNow()
        repeat(5) { lock.unlock("0000") }
        wall += 86_400_000L
        val restarted = AppLockController(store, { wall }, { 1000L })
        assertEquals(LockResult.WAIT, restarted.unlock("1234"))
    }

    @Test fun screenOffWhileEnablingCannotBeOverwrittenByUnlock() = runBlocking {
        val store = MemoryStorage()
        val lock = AppLockController(store, { 1000 }, { 1000 })
        store.afterWrite = { lock.lockNow() }
        lock.enable("1234", false, true)
        assertTrue(lock.state.value.enabled)
        assertTrue(lock.state.value.locked)
    }

    @Test fun cancelingSettingsScopeDuringCommitKeepsMemoryInSync() = runBlocking {
        val store = MemoryStorage()
        val lock = AppLockController(store, { 1000 }, { 1000 })
        lateinit var operation: Job
        store.afterWrite = { operation.cancel() }
        operation = launch { lock.enable("1234", false, true) }
        operation.join()
        assertTrue(store.enabled)
        assertTrue(lock.state.value.enabled)
        store.afterWrite = {}
        lock.lockNow()
        assertEquals(LockResult.SUCCESS, lock.unlock("1234"))
    }

    @Test fun protectionIsOptInAndNewProcessLocksExistingProtectedApp() = runBlocking {
        val store = MemoryStorage()
        val lock = AppLockController(store, { 1000 }, { 1000 })
        assertFalse(lock.state.value.enabled)
        assertEquals(LockResult.UNAVAILABLE, lock.enable("1234", false, false))
        assertEquals(LockResult.SUCCESS, lock.enable("1234", false, true))
        val relaunched = AppLockController(store, { 1000 }, { 1000 })
        assertTrue(relaunched.state.value.locked)
        assertEquals(LockResult.WRONG_CODE, relaunched.unlock("9876"))
        assertTrue(relaunched.state.value.locked)
        assertEquals(LockResult.SUCCESS, relaunched.unlock("1234"))
        assertFalse(relaunched.state.value.locked)
    }

    @Test fun retryDelaySurvivesRestartAndExpiresOnMonotonicClock() = runBlocking {
        val store = MemoryStorage()
        var wall = 1000L
        var elapsed = 1000L
        val lock = AppLockController(store, { wall }, { elapsed })
        lock.enable("1234", false, true)
        lock.lockNow()
        repeat(5) { lock.unlock("0000") }
        val restarted = AppLockController(store, { wall }, { elapsed })
        assertEquals(LockResult.WAIT, restarted.unlock("1234"))
        wall -= 100_000
        elapsed += 30_000
        assertEquals(LockResult.SUCCESS, restarted.unlock("1234"))
    }

    @Test fun settingsRequireCurrentCodeAndRecoveryOnlyUsesCurrentGeneration() = runBlocking {
        val store = MemoryStorage()
        val lock = AppLockController(store, { 1000 }, { 1000 })
        lock.enable("1234", true, true)
        assertEquals(LockResult.WRONG_CODE, lock.update("0000", "5678", false))
        assertEquals(LockResult.WRONG_CODE, lock.disable("0000"))
        assertEquals(LockResult.SUCCESS, lock.update("1234", "5678", false))
        val pendingGeneration = lock.generation
        lock.lockNow()
        assertEquals(LockResult.UNAVAILABLE, lock.biometricSucceeded(pendingGeneration))
        assertEquals(LockResult.UNAVAILABLE, lock.recoverAfterDeviceCredential(pendingGeneration))
        assertTrue(lock.state.value.locked)
        assertEquals(LockResult.SUCCESS, lock.recoverAfterDeviceCredential(lock.generation))
        assertFalse(lock.state.value.enabled)
        assertNull(store.record)
    }

    @Test fun corruptProtectionFailsClosedAndCanBeRecovered() = runBlocking {
        var cleared = false
        val store = object : LockStorage {
            override val enabled = true
            override fun read(): StoredLock? = error("Corrupt ciphertext")
            override fun write(record: StoredLock) = Unit
            override fun clear() { cleared = true }
        }
        val lock = AppLockController(store, { 1000 }, { 1000 })
        assertTrue(lock.state.value.locked)
        assertTrue(lock.state.value.storageError)
        assertEquals(LockResult.UNAVAILABLE, lock.unlock("1234"))
        assertEquals(LockResult.SUCCESS, lock.recoverAfterDeviceCredential(lock.generation))
        assertTrue(cleared)
    }
}
