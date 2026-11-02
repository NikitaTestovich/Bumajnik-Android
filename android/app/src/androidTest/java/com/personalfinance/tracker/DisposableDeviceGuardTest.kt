package com.personalfinance.tracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisposableDeviceGuardTest {
    @Test fun requiresBothEmulatorPropertyAndKnownHardware() {
        assertTrue(isDisposableEmulator("1\n", "ranchu"))
        assertTrue(isDisposableEmulator("1", "goldfish"))
        assertFalse(isDisposableEmulator("0", "ranchu"))
        assertFalse(isDisposableEmulator("1", "physical"))
        assertFalse(isDisposableEmulator("", "physical"))
    }
}
