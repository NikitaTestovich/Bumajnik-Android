package com.personalfinance.tracker.security

import org.junit.Assert.*
import org.junit.Test

class BiometricEntryPolicyTest {
    @Test fun availableBiometricsStartOnlyOneAutomaticAttempt() {
        val p = BiometricEntryPolicy()
        p.beginSession(1, true, true)
        assertEquals(BiometricEntryPolicy.Phase.PROMPT_PENDING, p.phase.value)
        assertNotNull(p.beginAttempt())
        p.beginSession(1, true, true)
        assertNull(p.beginAttempt())
    }
    @Test fun disabledOrUnavailableStartsCode() {
        for ((enabled, available) in listOf(false to true, true to false, false to false)) {
            val p = BiometricEntryPolicy()
            p.beginSession(1, enabled, available)
            assertEquals(BiometricEntryPolicy.Phase.CODE, p.phase.value)
            assertNull(p.beginAttempt())
        }
    }
    @Test fun fallbackRejectsLateSuccessWithoutAutomaticReprompt() {
        val p = BiometricEntryPolicy()
        p.beginSession(1, true, true)
        val first = p.beginAttempt()!!
        p.fallback(first)
        p.beginSession(1, true, true)
        assertEquals(BiometricEntryPolicy.Phase.CODE, p.phase.value)
        assertFalse(p.acceptSuccess(first))
        val retry = p.beginAttempt()!!
        assertTrue(retry > first)
        p.fallback(first)
        assertFalse(p.acceptSuccess(first))
        assertTrue(p.acceptSuccess(retry))
        assertFalse(p.acceptSuccess(retry))
    }
    @Test fun newSessionRejectsOldCallbacksDuringNewPrompt() {
        val p = BiometricEntryPolicy()
        p.beginSession(1, true, true)
        val old = p.beginAttempt()!!
        p.beginSession(2, true, true)
        val fresh = p.beginAttempt()!!
        p.fallback(old)
        assertFalse(p.acceptSuccess(old))
        assertTrue(p.acceptSuccess(fresh))
    }
}
