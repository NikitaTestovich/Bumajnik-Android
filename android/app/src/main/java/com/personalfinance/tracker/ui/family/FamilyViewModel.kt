package com.personalfinance.tracker.ui.family

import com.personalfinance.tracker.data.repository.FamilyRepository
import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.model.PeriodRange
import java.math.BigDecimal
import kotlinx.coroutines.flow.first

class FamilyViewModel(
    private val familyRepository: FamilyRepository,
    private val rateRepository: RateRepository,
) {
    suspend fun saveRates(range: PeriodRange, currentBase: String, rates: Map<String, BigDecimal>) =
        rateRepository.saveRates(range, currentBase, rates)

    suspend fun firstFamilyDate(): java.time.LocalDate? {
        val range = PeriodRange(java.time.LocalDate.of(1, 1, 1), java.time.LocalDate.of(9999, 12, 31))
        return familyRepository.observeFamilyTransactions(range).first().minOfOrNull { it.date }
    }
}
