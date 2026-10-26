package com.personalfinance.tracker.security

import org.junit.Assert.*
import org.junit.Test

class AppLockPolicyTest {
    @Test fun fiveFailuresStartThirtySecondDelayAndExpiredDelayResetsCount() {
        var attempts = RetryState()
        repeat(4) { attempts = attempts.failed(1000) }
        assertEquals(0L, attempts.remainingMillis(1000))
        attempts = attempts.failed(1000)
        assertEquals(30_000L, attempts.remainingMillis(1000))
        assertEquals(1L, attempts.remainingMillis(30_999))
        assertEquals(0L, attempts.remainingMillis(31_000))
        assertEquals(1, attempts.failed(31_000).failures)
    }

    @Test fun persistedDelayIsBoundedWhenWallClockMovesBackward() {
        val restored = RetryState(5, 50_000)
        assertEquals(10_000L, restored.remainingMillis(40_000))
        assertEquals(30_000L, restored.remainingMillis(-100_000))
    }

    @Test fun locksAfterTimeoutOrScreenOffButAllowsShortPickerRoundTrip() {
        val session = LockSession()
        assertTrue(session.locked)
        session.unlock()
        session.background(1000)
        session.foreground(30_999)
        assertFalse(session.locked)
        session.background(40_000)
        session.foreground(70_000)
        assertTrue(session.locked)
        session.unlock()
        session.screenOff()
        assertTrue(session.locked)
    }
}
