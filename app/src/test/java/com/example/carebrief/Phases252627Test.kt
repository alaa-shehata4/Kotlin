package com.example.carebrief

import com.example.carebrief.core.model.AiInsight
import com.example.carebrief.core.model.CarePlan
import com.example.carebrief.core.model.CarePlanStatus
import com.example.carebrief.core.model.CarePriority
import com.example.carebrief.core.model.CareTaskModel
import com.example.carebrief.core.model.InsightSeverity
import com.example.carebrief.core.model.TaskPriority
import com.example.carebrief.core.model.toCarePlanStatus
import com.example.carebrief.core.model.toCarePriority
import com.example.carebrief.core.model.toDraft
import com.example.carebrief.core.ui.responsive.widthClassFor
import com.example.carebrief.core.ui.responsive.WindowWidthClass
import com.example.carebrief.data.toEntity
import com.example.carebrief.data.toModel
import org.junit.Assert.*
import org.junit.Test

class Phases252627Test {
    // Phase 25: responsive breakpoints, no hard-coded dimensions.
    @Test
    fun widthClass_compact_medium_expanded() {
        assertEquals(WindowWidthClass.COMPACT, widthClassFor(320f))
        assertEquals(WindowWidthClass.COMPACT, widthClassFor(599f))
        assertEquals(WindowWidthClass.MEDIUM, widthClassFor(600f))
        assertEquals(WindowWidthClass.MEDIUM, widthClassFor(839f))
        assertEquals(WindowWidthClass.EXPANDED, widthClassFor(840f))
        assertEquals(WindowWidthClass.EXPANDED, widthClassFor(1280f))
    }

    // Phase 26: canonical models carry all spec fields.
    @Test
    fun carePlan_model_roundTrip() {
        val plan = CarePlan(
            id = "p1",
            recipientId = "sarah",
            goal = "Support consistent nutrition monitoring.",
            reason = "Based on repeated mentions.",
            actions = listOf("Track meal intake daily.", "Record appetite observations."),
            monitoringIndicators = listOf("Nutrition", "Energy"),
            priority = CarePriority.HIGH,
            status = CarePlanStatus.DRAFT,
            reviewDateMillis = 123L,
            reviewDateLabel = "In 7 days"
        )
        val entity = plan.toEntity(now = 999L)
        assertEquals("High", entity.priority)
        assertEquals("DRAFT", entity.status)
        assertEquals(999L, entity.updatedAtMillis)
        val back = entity.toModel()
        assertEquals(plan.goal, back.goal)
        assertEquals(plan.actions, back.actions)
        assertEquals(plan.monitoringIndicators, back.monitoringIndicators)
        assertEquals(CarePriority.HIGH, back.priority)
        assertEquals(CarePlanStatus.DRAFT, back.status)
        // Draft seam stays compatible with the AI provider.
        assertEquals(plan.goal, plan.toDraft().goal)
    }

    @Test
    fun task_model_roundTrip_withPlanLink() {
        val task = CareTaskModel(
            id = "t1",
            carePlanId = "p1",
            recipientId = "sarah",
            title = "Record today's meal intake",
            description = "Breakfast + lunch",
            dueLabel = "Today",
            priority = TaskPriority.HIGH,
            category = "Nutrition"
        )
        val entity = task.toEntity()
        assertEquals("p1", entity.carePlanId)
        assertEquals("High", entity.priority)
        val back = entity.toModel()
        assertEquals("p1", back.carePlanId)
        assertEquals(TaskPriority.HIGH, back.priority)
        assertFalse(back.completed)
    }

    @Test
    fun insight_model_isExplainable_notADiagnosis() {
        val insight = AiInsight(
            id = "i1",
            recipientId = "sarah",
            title = "Reduced appetite",
            description = "May warrant additional observation.",
            evidence = "3 notes over the last 5 days mentioned reduced food intake.",
            frequency = "Observed in 3 of 5 recent notes.",
            frequencyCount = 3,
            frequencyTotal = 5,
            severity = InsightSeverity.WATCH
        )
        assertFalse(insight.frequency.contains("%"))
        assertFalse(insight.description.contains("Diagnosis", ignoreCase = true))
        val back = insight.toEntity().toModel()
        assertEquals(3, back.frequencyCount)
        assertEquals(5, back.frequencyTotal)
        assertEquals(InsightSeverity.WATCH, back.severity)
    }

    @Test
    fun priority_parsing_isCaseInsensitive() {
        assertEquals(CarePriority.HIGH, "high".toCarePriority())
        assertEquals(CarePriority.LOW, " Low ".toCarePriority())
        assertEquals(CarePriority.NORMAL, "bogus".toCarePriority())
        assertEquals(CarePlanStatus.ACTIVE, "active".toCarePlanStatus())
    }
}
