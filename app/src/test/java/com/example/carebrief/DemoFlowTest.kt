package com.example.carebrief

import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.TaskStore
import com.example.carebrief.data.ai.DemoAiCareAssistant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 29 — end-to-end demonstration script exercised without Android:
 * recipients -> notes -> add note -> analyze -> summary -> care-plan draft ->
 * edit -> approve -> tasks -> complete task. Mirrors FINAL DEMONSTRATION SCRIPT.
 */
class DemoFlowTest {
    private val ai = DemoAiCareAssistant()

    @Test
    fun fullDemoFlow_sarah() = runTest {
        val repo = DemoCareBriefRepository()
        val plans = CarePlanStore()
        val tasks = TaskStore()

        // 1-2. Recipients present; Sarah selectable.
        val people = repo.observeRecipients().first()
        assertTrue(people.any { it.id == "sarah" && it.name == "Sarah Johnson" })

        // 3. Recent notes exist for Sarah.
        val before = repo.observeNotes("sarah").first()
        assertTrue(before.size >= 4)

        // 4. Add another observation.
        val added = repo.addNote(
            recipientId = "sarah",
            content = "Sarah ate a good portion of lunch and seemed a little brighter.",
            categories = listOf("Nutrition", "Mood"),
            author = "Caregiver"
        )
        assertTrue(repo.observeNotes("sarah").first().any { it.id == added.id })

        // 5-6. Analyze: summary + patterns + concerns (cautious language).
        val notes = repo.observeNotes("sarah").first()
        val summary = ai.summarizeNotes(notes)
        assertTrue(summary.isNotEmpty())
        assertFalse(summary.joinToString(" ").contains("diagnos", ignoreCase = true))
        val patterns = ai.analyzePatterns(notes)
        assertTrue((patterns["Nutrition"] ?: 0) >= 4)
        val concerns = ai.analyzePotentialConcerns(notes)
        assertTrue(concerns.isNotEmpty())

        // 7-8. Care-plan draft from the analysis.
        val draft = ai.generateCarePlanDraft(notes)
        assertTrue(draft.goal.isNotBlank() && draft.actions.size >= 3)

        // 9. Edit one action, then save as draft.
        val edited = plans.defaultFor("sarah", notes).copy(
            goal = draft.goal,
            reason = draft.reason,
            actions = draft.actions.mapIndexed { i, a ->
                if (i == 0) "Track meal intake daily, including portion sizes." else a
            },
            monitoring = draft.monitoring
        )
        plans.save(edited)
        assertTrue(plans.observe("sarah").first()?.actions?.first()
            ?.contains("portion sizes") == true)

        // 10. Approve -> plan becomes ACTIVE and repository reflects it.
        plans.approve("sarah")
        repo.setPlanStatus("sarah", PlanStatus.ACTIVE)
        assertEquals("ACTIVE", plans.observe("sarah").first()?.status)
        assertEquals(PlanStatus.ACTIVE, repo.observeRecipient("sarah").first()?.planStatus)

        // 11. Generate tasks from the draft, then complete one.
        val saved = plans.observe("sarah").first()!!
        tasks.ensureGenerated(
            "sarah",
            com.example.carebrief.core.model.CarePlanDraft(
                saved.goal, saved.reason, saved.actions, saved.monitoring
            )
        )
        val generated = tasks.observe("sarah").first()
        assertTrue(generated.isNotEmpty())
        tasks.toggle("sarah", generated.first().id)
        assertTrue(tasks.observe("sarah").first().first { it.id == generated.first().id }.completed)

        // 12. AI task drafts stay deterministic for the same draft.
        assertEquals(ai.generateTaskDrafts(draft), ai.generateTaskDrafts(draft))
    }
}
