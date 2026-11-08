package com.personalfinance.tracker.ui.operations

import org.junit.Assert.*
import org.junit.Test

class AmountCalculatorTest {
 @Test fun calculatesPrecedenceAndDecimalSeparators() {
  assertEquals("125.00", calculateAmount("80+45"))
  assertEquals("11.00", calculateAmount("2+3×3"))
  assertEquals("3.75", calculateAmount("7,5÷2"))
  assertEquals("0.33", calculateAmount("1÷3"))
 }
 @Test fun rejectsInvalidUnsafeAndNonPositiveResults() {
  listOf("", "1÷0", "1+", "1--2", "-2", "0", "999999999999999999999999999999999", "1a2").forEach { assertNull(it, calculateAmount(it)) }
 }
}
