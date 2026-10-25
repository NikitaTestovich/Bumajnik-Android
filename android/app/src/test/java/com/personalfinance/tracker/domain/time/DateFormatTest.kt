package com.personalfinance.tracker.domain.time

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatTest {
    @Test
    fun formatsAndParsesDayMonthYear() {
        val date = LocalDate.of(2026, 10, 21)

        assertEquals("21-10-2026", FinanceDateFormat.format(date))
        assertEquals(date, FinanceDateFormat.parse("21-10-2026"))
    }

    @Test
    fun parsesLegacyIsoDatesForExistingCsvFiles() {
        assertEquals(LocalDate.of(2026, 10, 21), FinanceDateFormat.parse("2026-10-21"))
    }
}
