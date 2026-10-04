package com.example.carebrief

import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.local.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarePlanStoreTest {
    @Test
    fun default_comesFromAiWithReviewDate() {
        val store = CarePlanStore()
        val plan = store.defaultFor("sarah", DemoData.sarahNotes)
        assertTrue(plan.goal.contains("monitoring", ignoreCase = true))
        assertTrue(plan.actions.size >= 3)
        assertTrue(plan.reviewDateLabel.isNotBlank())
        assertEquals("DRAFT", plan.status)
    }

    @Test
    fun save_thenApprove_flipsStatus() {
        val store = CarePlanStore()
        val plan = store.defaultFor("sarah", DemoData.sarahNotes)
        store.save(plan.copy(goal = "Edited goal"))
        assertEquals("Edited goal", store.observe("sarah").value?.goal)
        store.approve("sarah")
        assertEquals("ACTIVE", store.observe("sarah").value?.status)
    }

    @Test
    fun emptyNotes_stillProducesHonestDraft() {
        val store = CarePlanStore()
        val plan = store.defaultFor("amina", emptyList())
        assertTrue(plan.reason.isNotBlank())
        assertTrue(plan.actions.isNotEmpty())
    }
}
