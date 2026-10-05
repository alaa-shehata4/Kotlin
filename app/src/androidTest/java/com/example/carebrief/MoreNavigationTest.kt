package com.example.carebrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoreNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun moreTabOpensSettingsWithoutReturningToOnboarding() {
        compose.onNodeWithText("More", substring = true, useUnmergedTree = true)
            .performClick()

        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithText("Care documentation, made simpler.").assertDoesNotExist()
    }
}
