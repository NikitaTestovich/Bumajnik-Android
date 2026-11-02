package com.personalfinance.tracker.data.csv

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.local.AppDatabase
import com.personalfinance.tracker.data.repository.RoomFinanceRepository
import com.personalfinance.tracker.data.repository.RoomFamilyRepository
import com.personalfinance.tracker.domain.model.PeriodRange
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.domain.summary.SummaryService
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThreeMonthFixturesTest {
    @Test fun personalAndFamilyImportsReconcileForEachMonthAndDuplicatesAreSkipped() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, AppDatabase::class.java).build()
        try {
            val personal = RoomFinanceRepository(db)
            val family = RoomFamilyRepository(db)
            personal.saveProfile(UserProfile("Анна Пример", "ru", "BYN", true))
            fun fixture(index: Int) = instrumentation.context.assets.open("fixtures-120/person-$index.csv").use { it.readBytes() }
            val personalBackup = PersonalBackupManager(personal, { error("unused") }, { _, _ -> error("unused") })
            assertEquals(420, personalBackup.restoreBytes(fixture(1)).addedCount)
            assertEquals(420, personalBackup.restoreBytes(fixture(1)).duplicateCount)
            val importer = FamilyImportManager(family) { error("unused") }
            val second = importer.previewBytes(fixture(2))
            val third = importer.previewBytes(fixture(3))
            assertEquals(420, importer.import(second, "BYN", emptyMap()).addedCount)
            assertEquals(420, importer.import(third, "BYN", mapOf("USD" to BigDecimal("3"))).addedCount)
            assertEquals(420, importer.import(second, "BYN", emptyMap()).duplicateCount)
            for (month in 7..9) {
            val start = LocalDate.of(2026, month, 1)
            val period = PeriodRange(start, start.plusMonths(1))
            val own = personal.transactionsBetween(period, null)
            val summary = SummaryService(personal).summarize(period, "BYN", mapOf("BYN" to BigDecimal.ONE))
            assertEquals(462770L, summary.income.minorUnits)
            assertEquals(347490L, summary.expense.minorUnits)
            val imported = family.observeFamilyTransactions(period).first()
            val categoryNames = personal.observeCategories().first().associate { it.id to (it.nameRu ?: it.nameEn.orEmpty()) }
            val report = FamilyReportCalculator.calculate(imported, own, "BYN", emptyMap(), categoryNames, "Без категории")
            assertEquals(2335340L, report.incomeMinorUnits)
            assertEquals(1754740L, report.expensesMinorUnits)
            assertEquals(report.expensesMinorUnits, report.expensesByCategory.values.sum())
            report.expensesByCategory.forEach { (name, total) -> assertEquals(total, report.expenseDetailsByCategory.getValue(name).sumOf { it.convertedMinorUnits }) }
            }
        } finally { db.close() }
    }
}
