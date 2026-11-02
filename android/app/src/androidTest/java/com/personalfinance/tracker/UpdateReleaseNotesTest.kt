package com.personalfinance.tracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.update.UpdateAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalfinance.tracker.update.UpdateReleaseNotes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateReleaseNotesTest {
    @get:Rule val ui = createComposeRule()

    @Test fun releaseNotesAreShownWhenProvided() {
        ui.setContent { UpdateReleaseNotes("Исправлен поиск\nУлучшены отчёты") }
        ui.onNodeWithText("Исправлен поиск\nУлучшены отчёты").assertIsDisplayed()
    }

    @Test fun checkButtonBecomesUpdateButtonWhenUpdateAvailable() {
        val available = mutableStateOf(false)
        var checks = 0
        var installs = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ui.setContent { UpdateAction(available.value, false, { checks++ }, { installs++ }) }
        ui.onNodeWithText(context.getString(R.string.updates_check)).performClick()
        ui.runOnIdle { org.junit.Assert.assertEquals(1, checks); available.value = true }
        ui.onNodeWithText(context.getString(R.string.updates_check)).assertDoesNotExist()
        ui.onNodeWithText(context.getString(R.string.updates_install)).performClick()
        ui.runOnIdle { org.junit.Assert.assertEquals(1, installs) }
    }
}
