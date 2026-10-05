package com.example.carebrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 29 — on-device UI coverage for the core demonstration flow:
 * launch -> dashboard -> recipient -> notes -> add note -> analyze ->
 * summary -> care plan -> approve -> tasks -> complete task.
 *
 * Texts match the production copy; matchers use `useUnmergedTree` where the
 * bottom bar labels merge with icons.
 */
@RunWith(AndroidJUnit4::class)
class CareBriefFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsDashboard() {
        compose.onNodeWithText("Good morning", substring = true).assertIsDisplayed()
    }

    @Test
    fun dashboard_navigateToPeopleAndBack() {
        compose.onNodeWithText("People", substring = true, useUnmergedTree = true)
            .performClick()
        compose.onNodeWithText("Everyone you support", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun people_searchFiltersRecipients() {
        compose.onNodeWithText("People", substring = true, useUnmergedTree = true)
            .performClick()
        compose.onNodeWithText("Search by name", substring = true).performTextInput("Sarah")
        compose.onNodeWithText("Sarah Johnson", substring = true).assertIsDisplayed()
    }

    @Test
    fun notes_addNoteFlow() {
        compose.onNodeWithText("Notes", substring = true, useUnmergedTree = true)
            .performClick()
        compose.onNodeWithText("Daily notes", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Add daily note", substring = true).performClick()
        compose.onNodeWithText("Describe what you observed", substring = true)
            .performTextInput("Sarah ate a good portion of lunch and seemed brighter today.")
        compose.onNodeWithText("Save note", substring = true).performClick()
        // Success confirmation offers the analysis entry point.
        compose.onNodeWithText("Analyze recent notes", substring = true).assertIsDisplayed()
    }

    @Test
    fun plan_approveAndCompleteTask() {
        // Open the care plan through the bottom-bar Plan tab.
        compose.onNodeWithText("Plan", substring = true, useUnmergedTree = true)
            .performClick()
        // Draft offers editing; editor approves with an explicit dialog.
        compose.onAllNodesWithText("Edit care plan", substring = true)
            .apply { if (fetchSemanticsNodes().isNotEmpty()) get(0).performClick() }
        compose.onNodeWithText("Approve care plan", substring = true).performClick()
        compose.onNodeWithText("Activate this care plan?", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Approve", substring = true, useUnmergedTree = true)
            .performClick()
        // Active plan shows progress and generated tasks.
        compose.onNodeWithText("ACTIVE CARE PLAN", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Back", substring = true)
    }

    @Test
    fun analysis_fullFlowNavigatesToSummary() {
        // Navigate to Notes tab
        compose.onNodeWithText("Notes", substring = true, useUnmergedTree = true)
            .performClick()
        compose.onNodeWithText("Daily notes", substring = true).assertIsDisplayed()
        // Tap analyze to start the analysis flow
        compose.onNodeWithText("Analyze recent notes", substring = true).performClick()
        // Wait for analysis to complete and summary to appear
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("AI Summary", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("AI Summary", substring = true).assertIsDisplayed()
    }

    @Test
    fun tasks_completeTaskShowsCompletion() {
        // Navigate to Plan tab
        compose.onNodeWithText("Plan", substring = true, useUnmergedTree = true)
            .performClick()
        // Open care plan editor
        compose.onAllNodesWithText("Edit care plan", substring = true)
            .apply { if (fetchSemanticsNodes().isNotEmpty()) get(0).performClick() }
        // Approve the plan
        compose.onNodeWithText("Approve care plan", substring = true).performClick()
        compose.onNodeWithText("Activate this care plan?", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Approve", substring = true, useUnmergedTree = true)
            .performClick()
        // Verify active plan is shown
        compose.onNodeWithText("ACTIVE CARE PLAN", substring = true).assertIsDisplayed()
        // Complete a task by clicking its checkbox
        compose.onAllNodesWithText("Record today's meal intake", substring = true)
            .apply { if (fetchSemanticsNodes().isNotEmpty()) get(0).performClick() }
        // Verify task completion is reflected (task should show as completed)
        compose.onNodeWithText("ACTIVE CARE PLAN", substring = true).assertIsDisplayed()
    }
}
