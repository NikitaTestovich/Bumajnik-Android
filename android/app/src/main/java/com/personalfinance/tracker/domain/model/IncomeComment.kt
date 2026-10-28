package com.personalfinance.tracker.domain.model

object IncomeComment {
    const val MAX_LENGTH = 150
    fun length(value: String): Int = value.codePointCount(0, value.length)
    fun isValid(value: String): Boolean = length(value) <= MAX_LENGTH
}
