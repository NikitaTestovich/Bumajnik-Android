package com.personalfinance.tracker.ui.family

import java.time.LocalDate
import com.personalfinance.tracker.domain.time.FinanceDateFormat

sealed interface FamilyPeriodValidationResult {
    data object INVALID_DATE_FORMAT : FamilyPeriodValidationResult
    data object BEFORE_FIRST_RECORD : FamilyPeriodValidationResult
    data object START_AFTER_END : FamilyPeriodValidationResult
    data class Valid(val start: LocalDate, val endInclusive: LocalDate) : FamilyPeriodValidationResult
}

object FamilyPeriodValidator {
    fun validate(startText: String, endText: String, firstRecordDate: LocalDate?): FamilyPeriodValidationResult {
        val start = FinanceDateFormat.parse(startText)
        val end = FinanceDateFormat.parse(endText)
        if (start == null || end == null) return FamilyPeriodValidationResult.INVALID_DATE_FORMAT
        if (firstRecordDate != null && start < firstRecordDate) return FamilyPeriodValidationResult.BEFORE_FIRST_RECORD
        if (start > end) return FamilyPeriodValidationResult.START_AFTER_END
        return FamilyPeriodValidationResult.Valid(start, end)
    }
}
