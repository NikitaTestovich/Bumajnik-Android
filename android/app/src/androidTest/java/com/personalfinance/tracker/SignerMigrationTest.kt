package com.personalfinance.tracker

import android.os.Bundle
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.backup.*
import com.personalfinance.tracker.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.math.BigDecimal
import com.personalfinance.tracker.domain.model.PeriodRange
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Two phases with disposable data only. The test package must match each target APK's signing key. */
@RunWith(AndroidJUnit4::class)
class SignerMigrationTest {
    @Test fun transferAcrossDifferentSigner() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("signerMigrationPhase")
        assumeTrue(phase == "export" || phase == "restore")
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as FinanceApplication
        if (phase == "export") {
            val snapshot = captureFullBackup(app.container.database)
            assertEquals("Upgrade Demo", snapshot.profile?.name)
            val preferences = BackupPreferences(app.themeStore.theme.value.id, app.operationsTabStore.selectedType.value.name, "summary:1;goal:0", false)
            val bytes = FullBackupFile.encode(snapshot, preferences)
            instrumentation.sendStatus(0, Bundle().apply { putString("syntheticBackup", Base64.encodeToString(bytes, Base64.NO_WRAP)) })
        } else {
            val bytes = Base64.decode(requireNotNull(args.getString("syntheticBackup")), Base64.NO_WRAP)
            val (snapshot, preferences) = FullBackupFile.decode(bytes)
            assertTrue(applyFullBackup(app, snapshot, preferences))
            assertEquals("Upgrade Demo", app.container.financeRepository.observeProfile().first()?.name)
            assertEquals(12345L, app.container.financeRepository.observeTransactions(null).first().single().money.minorUnits)
            assertEquals("groceries", app.container.financeRepository.observeCategories().first().single().iconKey)
            assertEquals(20000L, app.container.financeRepository.observeGoal().first()?.saved?.minorUnits)
            val period = PeriodRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))
            assertEquals("Family Demo", app.container.familyRepository.observeFamilyTransactions(period).first().single().owner)
            assertEquals(BigDecimal("3"), app.container.rateRepository.ratesFor(period, "BYN").first()["USD"])
            assertEquals(8, app.themeStore.theme.value.id)
            assertEquals(TransactionType.INCOME, app.operationsTabStore.selectedType.value)
            assertEquals("summary:1;goal:0", app.getSharedPreferences("home_layout", 0).getString("blocks", ""))
            assertFalse(app.getSharedPreferences("appearance", 0).getBoolean("category_icon_labels", true))
            assertFalse("Device-bound lock must be reconfigured after reinstall", app.appLock.state.value.enabled)
        }
    }
}
