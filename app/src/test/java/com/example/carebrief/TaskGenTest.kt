package com.example.carebrief

import com.example.carebrief.core.model.CarePlanDraft
import com.example.carebrief.data.TaskStore
import com.example.carebrief.data.ai.DemoAiCareAssistant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskStoreTest {
    private val draft = CarePlanDraft(
        goal = "Support consistent nutrition monitoring.",
        reason = "Based on repeated mentions.",
        actions = listOf("Track meal intake daily.", "Record appetite observations."),
        monitoring = listOf("Nutrition")
    )

    @Test
    fun ensureGenerated_createsTasksOnce() {
        val store = TaskStore()
        store.ensureGenerated("sarah", draft)
        val first = store.observe("sarah").value
        assertTrue(first.isNotEmpty())
        store.ensureGenerated("sarah", draft)
        assertEquals(first.size, store.observe("sarah").value.size)
    }

    @Test
    fun toggle_flipsCompletedBothWays() {
        val store = TaskStore()
        store.ensureGenerated("sarah", draft)
        val id = store.observe("sarah").value.first().id
        store.toggle("sarah", id)
        assertTrue(store.observe("sarah").value.first { it.id == id }.completed)
        store.toggle("sarah", id)
        assertTrue(!store.observe("sarah").value.first { it.id == id }.completed)
    }
}

class AiTaskGenTest {
    private val ai = DemoAiCareAssistant()

    @Test
    fun tasks_deriveFromDraftActions() {
        val draft = CarePlanDraft(
            goal = "Keep joints moving.",
            reason = "Based on notes.",
            actions = listOf("Water the plants daily."),
            monitoring = listOf("Mobility")
        )
        val tasks = ai.generateTaskDrafts(draft)
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].title.contains("Water the plants"))
    }

    @Test
    fun tasks_areDeterministic() {
        val draft = CarePlanDraft("g", "r", listOf("Track meal intake daily."), listOf("Nutrition"))
        assertEquals(ai.generateTaskDrafts(draft), ai.generateTaskDrafts(draft))
    }

    @Test
    fun legacyStrings_stillWork() {
        val tasks = ai.generateSuggestedTasks(
            CarePlanDraft("g", "r", listOf("Track meal intake daily."), listOf("Nutrition"))
        )
        assertTrue(tasks.isNotEmpty())
    }
}
