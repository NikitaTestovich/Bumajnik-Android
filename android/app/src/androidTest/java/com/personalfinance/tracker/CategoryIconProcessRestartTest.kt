package com.personalfinance.tracker

import android.os.Process
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.ui.profile.CategoryEditor
import com.personalfinance.tracker.ui.theme.FinanceTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Run seed and verify separately with adb force-stop between them. */
class CategoryIconProcessRestartTest {
    @get:Rule val ui = createComposeRule()
    @Test fun hiddenLabelsAndSavedIconSurviveNewProcess() = runBlocking<Unit> {
        val phase = InstrumentationRegistry.getArguments().getString("iconRestartPhase")
        Assume.assumeTrue(phase == "seed" || phase == "verify")
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinanceApplication
        val repository = app.container.financeRepository
        val prefs = app.getSharedPreferences("appearance", 0)
        val marker = app.getSharedPreferences("synthetic-icon-restart", 0)
        val id = "synthetic-icon-process-restart"
        if (phase == "seed") {
            prefs.edit().remove("category_icon_labels").commit()
            val category = Category(id, "Тест", "Test", true, iconKey = "home")
            repository.saveCategory(category)
            ui.setContent { FinanceTheme { CategoryEditor(category, "en", {}) { _, icon ->
                repository.saveCategory(category.copy(iconKey = icon)); true
            } } }
            ui.onNodeWithTag("category_icon_labels").performScrollTo().assertIsOn().performClick()
            ui.onNodeWithTag("category_icon_transport").performScrollTo().performClick()
            ui.onNodeWithTag("category_editor_save").performClick()
            ui.waitUntil(5000) { runBlocking { repository.observeCategories().first().any { it.id == id && it.iconKey == "transport" } } }
            assertTrue(marker.edit().putInt("pid", Process.myPid()).commit())
        } else {
            assertTrue(marker.getInt("pid", -1) > 0)
            assertNotEquals(marker.getInt("pid", -1), Process.myPid())
            val category = repository.observeCategories().first().single { it.id == id }
            assertEquals("transport", category.iconKey)
            ui.setContent { FinanceTheme { CategoryEditor(category, "en", {}, { _, _ -> true }) } }
            ui.onNodeWithTag("category_icon_labels").performScrollTo().assertIsOff()
            ui.onNodeWithTag("category_icon_transport").performScrollTo().assertIsSelected()
            ui.onNodeWithTag("category_icon_labels").performScrollTo().performClick().assertIsOn()
            ui.onNodeWithTag("category_icon_transport").performScrollTo().assertIsSelected()
            prefs.edit().remove("category_icon_labels").commit()
            marker.edit().clear().commit()
        }
    }
}
