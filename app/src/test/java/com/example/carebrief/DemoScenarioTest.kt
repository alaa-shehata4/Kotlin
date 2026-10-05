package com.example.carebrief

import com.example.carebrief.data.ai.DemoAiCareAssistant
import com.example.carebrief.data.local.DemoData
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phases 19-20: the demo dataset must carry the full story on its own. */
class DemoScenarioTest {
    private val allNotes = DemoData.sarahNotes + DemoData.michaelNotes + DemoData.aminaNotes

    @Test
    fun everyRecipient_hasAtLeastTwoNotes() {
        DemoData.recipients.forEach { person ->
            val count = allNotes.count { it.recipientId == person.id }
            assertTrue("${person.name} has only $count note(s)", count >= 2)
        }
    }

    @Test
    fun demo_spansSeveralDays() {
        val days = allNotes.map { it.dayLabel }.distinct()
        assertTrue("Only covers: $days", days.size >= 4)
    }

    @Test
    fun demo_coversCoreCategories() {
        val covered = allNotes.flatMap { it.categories }.toSet()
        listOf("Nutrition", "Sleep", "Mood", "Mobility", "Medication adherence").forEach {
            assertTrue("Missing category: $it", it in covered)
        }
        assertTrue("No behavioral/pain coverage: $covered", "Behavior" in covered || "Pain" in covered)
    }

    @Test
    fun sarahScenario_pointsAtAppetiteAndFatigueWithoutDiagnosing() {
        val ai = DemoAiCareAssistant()
        val patterns = ai.analyzePatterns(DemoData.sarahNotes)
        assertTrue((patterns["Nutrition"] ?: 0) >= 3)
        val draft = ai.generateCarePlanDraft(DemoData.sarahNotes)
        val text = (listOf(draft.goal, draft.reason) + draft.actions).joinToString(" ")
        assertTrue(!text.contains("diagnos", ignoreCase = true))
        assertTrue(!text.contains("definitely", ignoreCase = true))
        assertTrue(text.contains("meal intake", ignoreCase = true))
    }
}
