package com.personalfinance.tracker

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalfinance.tracker.domain.model.Category
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.ui.profile.ProfileScreen
import com.personalfinance.tracker.ui.profile.ProfileSection
import com.personalfinance.tracker.ui.theme.FinanceTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileSectionsTest {
    @get:Rule val ui = createComposeRule()
    @Test fun selectingCategoriesShowsOnlyThatPageAndBackReturnsToMore() {
        var section by mutableStateOf(ProfileSection.MORE)
        ui.setContent { FinanceTheme {
            ProfileScreen(UserProfile("Demo", "en", "USD", true), listOf(Category("groceries", "Продукты", "Groceries", false, iconKey = "groceries")),
                { _, _, _ -> }, { _, _, _ -> true }, { _, _, _ -> true }, { true }, {}, {}, {}, {},
                section = section, onOpenSection = { section = it }, onBack = { section = ProfileSection.MORE })
        } }
        ui.onNodeWithTag("category_row_groceries").assertDoesNotExist()
        ui.onNodeWithTag("open_CATEGORIES").performScrollTo().performClick()
        ui.onNodeWithTag("category_row_groceries").assertExists()
        ui.onNodeWithTag("open_THEMES").assertDoesNotExist()
        ui.onNodeWithText("Back").performClick()
        ui.onNodeWithTag("open_CATEGORIES").assertExists()
    }
}
