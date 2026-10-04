package com.example.carebrief

import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.presentation.careplan.PlanEditOps
import com.example.carebrief.presentation.careplan.validatePlan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEditorTest {
    @Test
    fun moveAction_reorders() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), PlanEditOps.move(list, 0, 1))
        assertEquals(listOf("a", "c", "b"), PlanEditOps.move(list, 2, 1))
        assertEquals(list, PlanEditOps.move(list, 0, 5))
    }

    @Test
    fun validation_requiresGoalActionMonitoring() {
        assertTrue(validatePlan("", listOf("a"), listOf("Mood")) is String)
        assertTrue(validatePlan("Goal", emptyList(), listOf("Mood")) is String)
        assertTrue(validatePlan("Goal", listOf("a"), emptyList()) is String)
        assertNull(validatePlan("Goal", listOf("a"), listOf("Mood")))
    }

    @Test
    fun approval_marksRecipientPlanActive() = runTest {
        val repo = DemoCareBriefRepository()
        repo.setPlanStatus("amina", PlanStatus.ACTIVE)
        assertEquals(PlanStatus.ACTIVE, repo.observeRecipient("amina").first()?.planStatus)
    }
}
