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
        assertFalse(p.platformReady.value)
        assertNull(p.beginAttempt())
        p.terminal(first)
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
        assertNull(p.beginAttempt())
        assertFalse(p.acceptSuccess(old))
        p.terminal(old)
        val fresh = p.beginAttempt()!!
        p.fallback(old)
        assertFalse(p.acceptSuccess(old))
        assertTrue(p.acceptSuccess(fresh))
    }

    @Test fun recoveredAvailabilityAllowsExplicitRetryWithoutAutoPrompt() {
        val p = BiometricEntryPolicy()
        p.beginSession(1, true, false)
        assertEquals(BiometricEntryPolicy.Phase.CODE, p.phase.value)
        p.beginSession(1, true, true)
        assertEquals(BiometricEntryPolicy.Phase.CODE, p.phase.value)
        assertNotNull(p.beginAttempt())
    }

    @Test fun missingTerminalCallbackKeepsCodeFallbackAndBlocksPromptReplacement() {
        val p = BiometricEntryPolicy()
        p.beginSession(1, true, true)
        val old = p.beginAttempt()!!
        p.fallback(old)
        p.beginSession(2, true, true)
        assertEquals(BiometricEntryPolicy.Phase.PROMPT_PENDING, p.phase.value)
        assertNull(p.beginAttempt())
        assertFalse(p.acceptSuccess(old))
        assertEquals(old, p.platformRequest?.attempt)
        // The code path remains available; no replacement prompt can claim a late old result.
        p.beginSession(2, true, false)
        assertEquals(BiometricEntryPolicy.Phase.CODE, p.phase.value)
    }
}
