package com.personalfinance.tracker.domain.money

/** Currency amounts are stored in the smallest unit, such as cents or kopecks. */
data class Money(val minorUnits: Long, val currencyCode: String) {
    init {
        require(currencyCode in SupportedCurrencies.codes) {
            "Unsupported currency code: $currencyCode"
        }
    }
}

object SupportedCurrencies {
    val codes: Set<String> = setOf("USD", "EUR", "RUB", "BYN")
}
