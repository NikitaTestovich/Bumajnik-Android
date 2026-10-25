package com.personalfinance.tracker.domain.money

import com.personalfinance.tracker.data.repository.RateRepository
import com.personalfinance.tracker.domain.model.PeriodRange
import java.math.BigDecimal
import kotlinx.coroutines.flow.first

/** Stores and reuses manually entered base-currency units per one source-currency unit. */
class ExchangeRateService(private val rates: RateRepository) {
    suspend fun getRates(
        range: PeriodRange,
        baseCurrency: String,
        sourceCurrencies: Set<String>,
    ): Map<String, BigDecimal> {
        require(baseCurrency in SupportedCurrencies.codes) { "Unsupported base currency: $baseCurrency" }
        require(sourceCurrencies.all { it in SupportedCurrencies.codes }) { "Unsupported source currency" }

        val saved = rates.ratesFor(range, baseCurrency).first()
        return buildMap {
            put(baseCurrency, BigDecimal.ONE)
            sourceCurrencies.asSequence()
                .filter { it != baseCurrency }
                .forEach { currency -> saved[currency]?.let { put(currency, it) } }
        }
    }

    suspend fun saveRates(
        range: PeriodRange,
        baseCurrency: String,
        values: Map<String, BigDecimal>,
    ) {
        require(baseCurrency in SupportedCurrencies.codes) { "Unsupported base currency: $baseCurrency" }
        require(values.keys.all { it in SupportedCurrencies.codes }) { "Unsupported source currency" }
        require(values.values.all { it > BigDecimal.ZERO }) { "Exchange rates must be positive" }

        val current = rates.ratesFor(range, baseCurrency).first()
        val merged = (current + values).filterKeys { it != baseCurrency }
        rates.saveRates(range, baseCurrency, merged)
    }
}
