package com.personalfinance.tracker.domain.model
import org.junit.Assert.*
import org.junit.Test
class CategoryIcons122Test {
 @Test fun existingKeysRemainAndNewKeysResolve() {
  val old = listOf("groceries", "home", "transport", "phone", "health", "education", "clothing", "dining", "entertainment", "household_goods", "travel", "gifts", "pets", "sport", "beauty", "children", "insurance", "taxes", "tools", "other")
  val added = listOf("bank_card", "airplane", "headphones", "bicycle", "coffee", "plant", "computer", "clock", "scissors", "umbrella")
  assertEquals(old, CategoryIcons.keys.take(20)); assertEquals(30, CategoryIcons.keys.size)
  assertEquals(CategoryIcons.keys.size, CategoryIcons.keys.distinct().size)
  added.forEach { assertTrue(it in CategoryIcons.keys); assertEquals(it, CategoryIcons.resolve(it)) }
  assertEquals("other", CategoryIcons.resolve("unknown"))
 }
}
