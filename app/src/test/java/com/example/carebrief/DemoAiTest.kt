package com.example.carebrief

import com.example.carebrief.data.ai.DemoAiCareAssistant
import com.example.carebrief.data.local.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAiTest {
    private val ai = DemoAiCareAssistant()

    @Test
    fun summary_usesCautiousLanguage() {
        val summary = ai.summarizeNotes(DemoData.sarahNotes)
        assertEquals(3, summary.size)
        val joined = summary.joinToString(" ")
        assertTrue(!joined.contains("Diagnosis", ignoreCase = true))
        assertTrue(!joined.contains("definitely", ignoreCase = true))
    }

    @Test
    fun patterns_countCategories() {
        val patterns = ai.analyzePatterns(DemoData.sarahNotes)
        assertEquals(4, patterns["Nutrition"])
        assertTrue(patterns.isNotEmpty())
    }

    @Test
    fun carePlan_isDraftWithEvidence() {
        val draft = ai.generateCarePlanDraft(DemoData.sarahNotes)
        assertTrue(draft.goal.contains("monitoring", ignoreCase = true))
        assertTrue(draft.actions.size >= 3)
        assertTrue(draft.reason.contains("Based on", ignoreCase = false))
    }

    @Test
    fun tasks_arePractical() {
        val draft = ai.generateCarePlanDraft(DemoData.sarahNotes)
        val tasks = ai.generateSuggestedTasks(draft)
        // Deterministic 1:1 mapping: one practical task per draft action.
        assertEquals(draft.actions.size, tasks.size)
        assertTrue(tasks.isNotEmpty())
        assertEquals(tasks, ai.generateSuggestedTasks(draft))
    }
}
