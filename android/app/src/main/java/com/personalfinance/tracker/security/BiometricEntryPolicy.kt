package com.personalfinance.tracker.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation state retained by one controller instance, never persisted across processes. */
class BiometricEntryPolicy {
    enum class Phase { PROMPT_PENDING, PROMPT_ACTIVE, CODE }
    data class PlatformRequest(val generation: Long, val attempt: Long)
    private val mutablePhase = MutableStateFlow(Phase.CODE)
    val phase = mutablePhase.asStateFlow()
    private val mutablePlatformReady = MutableStateFlow(true)
    val platformReady = mutablePlatformReady.asStateFlow()
    private var generation: Long? = null
    private var eligible = false
    private var nextAttempt = 0L
    var activeAttempt: Long? = null
        private set
    var platformRequest: PlatformRequest? = null
        private set
    private var cancelling = false

    fun beginSession(generation: Long, enabled: Boolean, available: Boolean) {
        if (this.generation == generation) {
            eligible = enabled && available
            if (!enabled || !available) {
                activeAttempt?.let(::fallback)
                mutablePhase.value = Phase.CODE
            }
            return
        }
        this.generation = generation
        eligible = enabled && available
        activeAttempt = null
        if (platformRequest != null) cancelling = true
        mutablePhase.value = if (eligible) Phase.PROMPT_PENDING else Phase.CODE
    }

    fun beginAttempt(): Long? {
        if (!eligible || activeAttempt != null || platformRequest != null) return null
        return (++nextAttempt).also {
            activeAttempt = it
            platformRequest = PlatformRequest(checkNotNull(generation), it)
            mutablePlatformReady.value = false
            cancelling = false
            mutablePhase.value = Phase.PROMPT_ACTIVE
        }
    }

    fun fallback(attempt: Long) {
        if (activeAttempt != attempt) return
        activeAttempt = null
        if (platformRequest?.attempt == attempt) cancelling = true
        mutablePhase.value = Phase.CODE
    }

    /** A framework success or error is terminal; only then may another prompt be registered. */
    fun terminal(attempt: Long) {
        if (platformRequest?.attempt != attempt) return
        platformRequest = null
        cancelling = false
        mutablePlatformReady.value = true
    }

    fun acceptSuccess(attempt: Long): Boolean {
        if (activeAttempt != attempt || platformRequest?.attempt != attempt || cancelling) return false
        activeAttempt = null
        terminal(attempt)
        mutablePhase.value = Phase.CODE
        return true
    }
}
