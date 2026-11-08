package com.personalfinance.tracker.ui.operations

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Evaluates the calculator's bounded, non-negative operand expression. */
internal fun calculateAmount(expression: String): String? = runCatching {
    require(expression.length in 1..120)
    val input = expression.replace(',', '.').replace('×', '*').replace('÷', '/')
    require(input.matches(Regex("[0-9]+(?:\\.[0-9]+)?(?:[+*/-][0-9]+(?:\\.[0-9]+)?)*")))
    val numbers = Regex("[0-9]+(?:\\.[0-9]+)?").findAll(input).map { BigDecimal(it.value) }.toList()
    val ops = input.filter { it in "+-*/" }
    var term = numbers.first()
    var total = BigDecimal.ZERO
    var add = true
    ops.forEachIndexed { index, op ->
        val next = numbers[index + 1]
        when (op) {
            '*' -> term = term.multiply(next, MathContext.DECIMAL128)
            '/' -> term = term.divide(next, MathContext.DECIMAL128)
            '+', '-' -> {
                total = if (add) total.add(term) else total.subtract(term)
                term = next
                add = op == '+'
            }
        }
    }
    val value = (if (add) total.add(term) else total.subtract(term)).setScale(2, RoundingMode.HALF_UP)
    require(value.signum() > 0)
    value.movePointRight(2).longValueExact()
    value.toPlainString()
}.getOrNull()
