package com.personalfinance.tracker

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.CategoryIcons
import com.personalfinance.tracker.ui.profile.CategoryEditor
import com.personalfinance.tracker.ui.theme.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

/** Catches lost label settings, hidden accessible names, and toggles that reset icon identity. */
@RunWith(Parameterized::class)
class CategoryIconPickerTest(private val themeId: Int, private val language: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "theme={0}, language={1}")
        fun cases() = listOf(2, 6, 8, 9, 10, 11, 12).flatMap { id -> listOf("en", "ru").map { arrayOf<Any>(id, it) } }
    }
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = context.getSharedPreferences("appearance", 0)
    @Before fun prepare() {
        Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        prefs.edit().remove("category_icon_labels").commit()
    }
    @After fun cleanup() { prefs.edit().remove("category_icon_labels").commit() }

    @Test fun defaultToggleSelectionSaveAndReopenRetainPreferenceAndAccessibleNames() {
        val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale(language)) })
        val labels = localized.resources.getStringArray(R.array.category_icon_names)
        var generation by mutableStateOf(0)
        var category by mutableStateOf(Category("unchanged-id", "Категория", "Category", true, iconKey = "home"))
        var saved: Pair<String, String>? = null
        ui.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                FinanceTheme(AppThemeRegistry.resolve(themeId)) {
                    key(generation) { CategoryEditor(category, language, {}) { name, icon -> saved = name to icon; true } }
                }
            }
        }
        ui.onNodeWithTag("category_icon_labels").performScrollTo().assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        CategoryIcons.keys.forEachIndexed { index, key ->
            ui.onNodeWithTag("category_icon_$key").performScrollTo().assertContentDescriptionEquals(labels[index])
            ui.onNodeWithTag("category_icon_label_$key", useUnmergedTree = true).assertIsDisplayed()
        }
        ui.onNodeWithTag("category_icon_labels").performScrollTo().performClick().assertIsOff()
        CategoryIcons.keys.forEachIndexed { index, key ->
            ui.onNodeWithTag("category_icon_$key").performScrollTo()
                .assertContentDescriptionEquals(labels[index]).assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            ui.onNodeWithTag("category_icon_label_$key", useUnmergedTree = true).assertDoesNotExist()
        }
        ui.onNodeWithTag("category_icon_home").performScrollTo().assertIsSelected()
        ui.onNodeWithTag("category_icon_transport").performScrollTo().performClick().assertIsSelected()
        ui.onNodeWithTag("category_icon_labels").performScrollTo().performClick().assertIsOn()
        ui.onNodeWithTag("category_icon_transport").performScrollTo().assertIsSelected().assertContentDescriptionEquals(labels[2])
        ui.onNodeWithTag("category_icon_label_transport", useUnmergedTree = true).assertIsDisplayed()
        ui.onNodeWithTag("category_icon_labels").performScrollTo().performClick()
        ui.onNodeWithTag("category_editor_save").performClick()
        ui.runOnIdle {
            assertEquals("transport", saved!!.second)
            assertEquals("unchanged-id", category.id)
            category = category.copy(iconKey = saved!!.second)
            generation++
        }
        ui.onNodeWithTag("category_icon_labels").performScrollTo().assertIsOff()
        ui.onNodeWithTag("category_icon_transport").performScrollTo().assertIsSelected().assertContentDescriptionEquals(labels[2])
        assertFalse(prefs.getBoolean("category_icon_labels", true))
        ui.onNodeWithTag("category_icon_labels").performScrollTo().performClick()
        ui.runOnIdle { generation++ }
        ui.onNodeWithTag("category_icon_labels").performScrollTo().assertIsOn()
        ui.onNodeWithTag("category_icon_transport").performScrollTo().assertIsSelected()
    }
}
