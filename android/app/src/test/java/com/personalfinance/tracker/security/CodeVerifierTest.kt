package com.personalfinance.tracker.security

import org.junit.Assert.*
import org.junit.Test

class CodeVerifierTest {
    @Test fun acceptsFourToTenCharactersWithoutComplexityRules() {
        assertFalse(CodeVerifier.isValid("123"))
        assertTrue(CodeVerifier.isValid("1234"))
        assertTrue(CodeVerifier.isValid("код!"))
        assertTrue(CodeVerifier.isValid("1234567890"))
        assertFalse(CodeVerifier.isValid("12345678901"))
        assertTrue(CodeVerifier.isValid("😀😀😀😀"))
        assertFalse(CodeVerifier.isValid("😀😀"))
    }

    @Test fun verifiesExactCodeWithUniqueSalts() {
        val first = CodeVerifier.create("Ab 4")
        val second = CodeVerifier.create("Ab 4")
        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.hash.contentEquals(second.hash))
        assertTrue(CodeVerifier.verify("Ab 4", first))
        assertFalse(CodeVerifier.verify("ab 4", first))
        assertFalse(CodeVerifier.verify("Ab 5", first))
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesShortCodeAtCreation() { CodeVerifier.create("123") }
}
