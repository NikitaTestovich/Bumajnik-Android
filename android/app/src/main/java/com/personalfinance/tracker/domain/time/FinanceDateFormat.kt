package com.personalfinance.tracker.domain.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object FinanceDateFormat {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun format(date: LocalDate): String = date.format(displayFormatter)

    fun parse(text: String): LocalDate? {
        val value = text.trim()
        return runCatching { LocalDate.parse(value, displayFormatter) }.getOrNull()
            ?: runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }
}
