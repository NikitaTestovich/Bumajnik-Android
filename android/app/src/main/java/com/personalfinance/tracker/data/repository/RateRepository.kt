package com.personalfinance.tracker.data.repository

import androidx.room.withTransaction
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.local.ExchangeRateEntity
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.money.SupportedCurrencies
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RateRepository {
    fun ratesFor(range: PeriodRange, baseCurrency: String): Flow<Map<String, BigDecimal>>
    suspend fun saveRates(range: PeriodRange, baseCurrency: String, rates: Map<String, BigDecimal>)
}

class RoomRateRepository(private val database: AppDatabase) : RateRepository {
    private val dao = database.exchangeRateDao()

    override fun ratesFor(range: PeriodRange, baseCurrency: String): Flow<Map<String, BigDecimal>> =
        dao.observeRates(range.start.toString(), range.endExclusive.toString(), baseCurrency)
            .map { rows -> rows.associate { it.sourceCurrency to BigDecimal(it.rateToBase) } }

    override suspend fun saveRates(
        range: PeriodRange,
        baseCurrency: String,
        rates: Map<String, BigDecimal>,
    ) {
        require(baseCurrency in SupportedCurrencies.codes) { "Unsupported base currency: $baseCurrency" }
        require(rates.keys.all { it in SupportedCurrencies.codes }) { "Unsupported source currency" }
        require(rates.values.all { it > BigDecimal.ZERO }) { "Exchange rates must be positive" }

        val start = range.start.toString()
        val end = range.endExclusive.toString()
        val rows = rates.map { (sourceCurrency, rate) ->
            ExchangeRateEntity(start, end, baseCurrency, sourceCurrency, rate.stripTrailingZeros().toPlainString())
        }
        database.withTransaction {
            dao.deleteRates(start, end, baseCurrency)
            if (rows.isNotEmpty()) dao.insertRates(rows)
        }
    }
}
