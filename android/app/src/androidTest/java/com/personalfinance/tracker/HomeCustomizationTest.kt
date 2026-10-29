package com.personalfinance.tracker

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.preferences.HomeLayoutStore
import com.personalfinance.tracker.ui.home.*
import com.personalfinance.tracker.ui.theme.FinanceTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeCustomizationTest {
    @get:Rule val ui = createComposeRule()
    @Before fun emulatorOnly() { Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu", "goldfish")) }
    @Test fun largeTextScrollDoesNotReorderAndHandleCanMoveAcrossEdge() {
        var settings by mutableStateOf(normalizeHomeLayout(emptyList()))
        ui.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                FinanceTheme { HomeCustomizationSheet(settings, { settings = it }, {}, {}) }
            }
        }
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("home_setting_summary").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        ui.onNodeWithTag("home_setting_summary").performTouchInput { swipeUp(startY = center.y, endY = 0f) }
        ui.runOnIdle { assertEquals(HomeBlock.SUMMARY, settings.first().id) }
        ui.onNodeWithTag("home_drag_expenses").performScrollTo()
        val handle = ui.onNodeWithTag("home_drag_expenses").fetchSemanticsNode().boundsInRoot
        ui.onNodeWithTag("home_drag_expenses").performTouchInput {
            down(center); advanceEventTime(650); moveTo(Offset(center.x, -200f)); advanceEventTime(900); up()
        }
        ui.runOnIdle { assertTrue(settings.indexOfFirst { it.id == HomeBlock.EXPENSES } < 4) }
    }
    @Test fun allHiddenStillOffersCustomization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = HomeLayoutStore(context)
        store.save(normalizeHomeLayout(emptyList()).map { it.copy(visible = false) })
        ui.setContent { FinanceTheme { HomeScreen({}, {}, {}, {}) } }
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("home_customize").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        ui.onNodeWithTag("home_customize").assertIsDisplayed().performClick()
        ui.onNodeWithTag("home_visible_summary").assertIsDisplayed()
        store.reset()
    }
    @Test fun dragCancelArrowsVisibilityAndPersistence() {
        var settings by mutableStateOf(normalizeHomeLayout(emptyList()))
        var saves = 0
        ui.setContent { FinanceTheme { HomeCustomizationSheet(settings, { settings = it; saves++ }, {}, {}) } }
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("home_drag_summary").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        val target = ui.onNodeWithTag("home_setting_goal").fetchSemanticsNode().boundsInRoot.center
        val handle = ui.onNodeWithTag("home_drag_summary").fetchSemanticsNode().boundsInRoot
        ui.onNodeWithTag("home_drag_summary").performTouchInput {
            down(center); advanceEventTime(650); moveTo(Offset(center.x, target.y - handle.top)); up()
        }
        ui.runOnIdle { assertEquals(HomeBlock.SUMMARY, settings[2].id); assertEquals(1, saves) }
        ui.onNodeWithTag("home_settings_list").performScrollToNode(hasTestTag("home_drag_quick_actions"))
        ui.onNodeWithTag("home_drag_quick_actions").performTouchInput { down(center); advanceEventTime(650); moveBy(Offset(0f, 50f)); cancel() }
        ui.runOnIdle { assertEquals(HomeBlock.QUICK_ACTIONS, settings.first().id); assertEquals(1, saves) }
        ui.onNodeWithTag("home_down_quick_actions").performClick()
        ui.runOnIdle { assertEquals(HomeBlock.QUICK_ACTIONS, settings[1].id) }
        ui.onNodeWithTag("home_visible_quick_actions").performClick()
        ui.runOnIdle { assertFalse(settings.single { it.id == HomeBlock.QUICK_ACTIONS }.visible) }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = HomeLayoutStore(context)
        ui.runOnIdle { store.save(settings) }
        assertEquals(settings, HomeLayoutStore(context).layout.value)
        store.reset()
    }
    @Test fun unknownFutureKeySurvivesKnownPreferenceSave() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("home_layout", 0)
        prefs.edit().putString("blocks", "future:0;latest:0").commit()
        val store = HomeLayoutStore(context)
        assertEquals(HomeBlock.LATEST, store.layout.value.first().id)
        store.save(store.layout.value)
        assertTrue(prefs.getString("blocks", "")!!.contains("future:0"))
        prefs.edit().clear().commit()
    }
}
