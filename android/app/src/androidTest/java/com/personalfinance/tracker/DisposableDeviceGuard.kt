package com.personalfinance.tracker

internal fun isDisposableEmulator(qemu: String, hardware: String): Boolean =
    qemu.trim() == "1" && hardware in listOf("ranchu", "goldfish")
