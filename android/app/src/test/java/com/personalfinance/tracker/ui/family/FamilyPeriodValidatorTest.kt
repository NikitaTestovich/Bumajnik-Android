package com.personalfinance.tracker.ui.family

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyPeriodValidatorTest {
    @Test
    fun classifiesInvalidDateInputForLocalizedScreenMessages() {
        assertEquals(
            FamilyPeriodValidationResult.INVALID_DATE_FORMAT,
            FamilyPeriodValidator.validate("not-a-date", "20-10-2026", null),
        )
    }

    @Test
    fun rejectsStartBeforeFirstFamilyRecord() {
        assertEquals(
            FamilyPeriodValidationResult.BEFORE_FIRST_RECORD,
            FamilyPeriodValidator.validate("01-10-2026", "20-10-2026", LocalDate.parse("2026-10-02")),
        )
    }

    @Test
    fun rejectsStartAfterEnd() {
        assertEquals(
            FamilyPeriodValidationResult.START_AFTER_END,
            FamilyPeriodValidator.validate("10-10-2026", "09-10-2026", null),
        )
    }

    @Test
    fun returnsParsedRangeForValidInput() {
        assertEquals(
            FamilyPeriodValidationResult.Valid(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-20")),
            FamilyPeriodValidator.validate("01-10-2026", "20-10-2026", LocalDate.parse("2026-09-25")),
        )
    }
}
