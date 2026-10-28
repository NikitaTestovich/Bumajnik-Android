package com.personalfinance.tracker.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation state retained by one controller instance, never persisted across processes. */
class BiometricEntryPolicy {
    enum class Phase { PROMPT_PENDING, PROMPT_ACTIVE, CODE }
    private val mutablePhase = MutableStateFlow(Phase.CODE)
    val phase = mutablePhase.asStateFlow()
    private var generation: Long? = null
    private var eligible = false
    private var nextAttempt = 0L
    var activeAttempt: Long? = null
        private set

    fun beginSession(generation: Long, enabled: Boolean, available: Boolean) {
        if (this.generation == generation) {
            if (!enabled || !available) {
                eligible = false
                activeAttempt = null
                mutablePhase.value = Phase.CODE
            }
            return
        }
        this.generation = generation
        eligible = enabled && available
        activeAttempt = null
        mutablePhase.value = if (eligible) Phase.PROMPT_PENDING else Phase.CODE
    }

    fun beginAttempt(): Long? {
        if (!eligible || activeAttempt != null) return null
        return (++nextAttempt).also {
            activeAttempt = it
            mutablePhase.value = Phase.PROMPT_ACTIVE
        }
    }

    fun fallback(attempt: Long) {
        if (activeAttempt != attempt) return
        activeAttempt = null
        mutablePhase.value = Phase.CODE
    }

    fun acceptSuccess(attempt: Long): Boolean {
        if (activeAttempt != attempt) return false
        activeAttempt = null
        mutablePhase.value = Phase.CODE
        return true
    }
}
