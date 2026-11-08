package com.personalfinance.tracker
import com.personalfinance.tracker.ui.theme.*
import org.junit.Assert.*
import org.junit.Test
class AppThemeRegistryTest {
 @Test fun stableIdsAndDefault() {
  assertEquals(listOf(2,6,8,9,10,11,12), AppThemeRegistry.available.map { it.id })
  assertEquals(2, AppThemeRegistry.resolve(null).id)
  AppThemeRegistry.available.forEach { assertEquals(it, AppThemeRegistry.resolve(it.id)) }
 }
 @Test fun unknownAndRetiredIdsUseAvailableDefault() {
  assertEquals(2, AppThemeRegistry.resolve(77).id)
  assertEquals(6, ThemeRegistry(AppThemeRegistry.available.filterNot { it.id == 2 },6).resolve(2).id)
 }
}
