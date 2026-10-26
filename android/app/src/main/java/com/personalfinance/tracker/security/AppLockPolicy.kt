package com.personalfinance.tracker.security

data class RetryState(val failures: Int = 0, val blockedUntil: Long = 0) {
    fun remainingMillis(now: Long): Long = (blockedUntil - now).coerceIn(0, 30_000)
    fun failed(now: Long): RetryState {
        val count = if (blockedUntil != 0L && now >= blockedUntil) 1 else failures + 1
        return if (count >= 5) RetryState(5, now + 30_000) else RetryState(count)
    }
}

class LockSession {
    var locked: Boolean = true
        private set
    private var backgroundAt: Long? = null
    fun unlock() { locked = false; backgroundAt = null }
    fun background(elapsed: Long) { backgroundAt = elapsed }
    fun foreground(elapsed: Long) {
        if (backgroundAt?.let { elapsed - it >= 30_000 } == true) locked = true
        backgroundAt = null
    }
    fun screenOff() { locked = true }
}
