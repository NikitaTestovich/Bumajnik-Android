package com.personalfinance.tracker

import android.graphics.Bitmap
import android.os.Build
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.csv.FamilyImportManager
import com.personalfinance.tracker.data.csv.PersonalBackupManager
import com.personalfinance.tracker.ui.onboarding.WelcomeViewModel
import com.personalfinance.tracker.ui.family.FamilyScreen
import com.personalfinance.tracker.ui.operations.OperationsScreen
import com.personalfinance.tracker.ui.reports.ReportsScreen
import com.personalfinance.tracker.ui.theme.FinanceTheme
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only; all displayed names and transactions are synthetic. */
@RunWith(AndroidJUnit4::class)
class FixtureScreenshotsTest {
    @get:Rule val ui = createComposeRule()
    @Test fun captureOperationsPersonalAndFamilyReports() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish"))
        val inst = InstrumentationRegistry.getInstrumentation()
        val app = inst.targetContext.applicationContext as FinanceApplication
        val repo = app.container.financeRepository
        val screen = mutableIntStateOf(0)
        fun fixture(index: Int) = inst.context.assets.open("fixtures-120/person-$index.csv").use { it.readBytes() }
        fun save(name: String) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Finance120")
            }
            val uri = checkNotNull(app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            checkNotNull(app.contentResolver.openOutputStream(uri)).use { stream ->
                ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        try {
            runBlocking {
                app.container.database.clearAllTables()
                WelcomeViewModel(repo, "ru").completeOnboarding("Анна Пример", "BYN")
                val backup = PersonalBackupManager(repo, { error("unused") }, { _, _ -> error("unused") })
                check(backup.restoreBytes(fixture(1)).addedCount == 420)
                val family = FamilyImportManager(app.container.familyRepository) { error("unused") }
                family.import(family.previewBytes(fixture(2)), "BYN", emptyMap())
                family.import(family.previewBytes(fixture(3)), "BYN", mapOf("USD" to BigDecimal("3")))
            }
            ui.setContent { FinanceTheme { key(screen.intValue) {
                val categories by repo.observeCategories().collectAsState(initial = emptyList())
                when (screen.intValue) {
                    0 -> OperationsScreen(categories, "en", {}, {})
                    1 -> ReportsScreen()
                    else -> FamilyScreen({})
                }
            } } }
            ui.waitUntil(15000) { ui.onAllNodesWithTag("operation_search_toggle").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            ui.waitUntil(15000) { runBlocking { repo.observeTransactions(null).first().size == 420 } }
            save("operations-v3.png")
            ui.runOnIdle { screen.intValue = 1 }
            ui.waitUntil(15000) { ui.onAllNodesWithText(app.getString(R.string.reports_apply)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            ui.onNode(hasText(app.getString(R.string.reports_from)) and hasSetTextAction()).performTextReplacement("01-07-2026")
            ui.onNode(hasText(app.getString(R.string.reports_to)) and hasSetTextAction()).performTextReplacement("30-09-2026")
            ui.onNodeWithText(app.getString(R.string.reports_apply)).performClick()
            ui.onNodeWithText(app.getString(R.string.reports_categories)).performScrollTo().assertIsDisplayed()
            ui.onAllNodes(hasContentDescription("Продукты", substring = true)).onFirst().performScrollTo().assertIsDisplayed()
            save("personal-report-v3.png")
            ui.onAllNodes(hasContentDescription("Продукты", substring = true)).onFirst().performClick()
            ui.onAllNodesWithText("Покупка 11, \"пример\"").onFirst().performScrollTo().assertIsDisplayed()
            save("personal-expanded-120.png")
            ui.runOnIdle { screen.intValue = 2 }
            ui.waitUntil(15000) { ui.onAllNodesWithText(app.getString(R.string.family_apply)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            ui.onNode(hasText(app.getString(R.string.family_from)) and hasSetTextAction()).performTextReplacement("01-07-2026")
            ui.onNode(hasText(app.getString(R.string.family_to)) and hasSetTextAction()).performTextReplacement("30-09-2026")
            ui.onNodeWithText(app.getString(R.string.family_apply)).performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText(app.getString(R.string.family_by_category)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            ui.onNodeWithText(app.getString(R.string.family_by_category)).performScrollTo().assertIsDisplayed()
            ui.onAllNodes(hasContentDescription("Продукты", substring = true)).onFirst().performScrollTo().assertIsDisplayed()
            save("family-report-v3.png")
            ui.onAllNodes(hasContentDescription("Продукты", substring = true)).onFirst().performClick()
            ui.onAllNodesWithText("Покупка 11, \"пример\"").onFirst().performScrollTo().assertIsDisplayed()
            save("family-expanded-120.png")
        } finally { app.container.database.clearAllTables() }
    }
}
