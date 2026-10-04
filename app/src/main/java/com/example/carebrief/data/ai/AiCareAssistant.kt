package com.example.carebrief.data.ai

import com.example.carebrief.core.model.CarePlanDraft
import com.example.carebrief.core.model.DailyNote

/**
 * AI seam. Demo provider works fully offline with deterministic output.
 * A remote LLM adapter can be added later behind this same interface
 * (secrets belong on a backend, never in the APK).
 */
interface AiCareAssistant {
    fun summarizeNotes(notes: List<DailyNote>): List<String>
    fun analyzePatterns(notes: List<DailyNote>): Map<String, Int>
    fun generateCarePlanDraft(notes: List<DailyNote>): CarePlanDraft
    fun generateSuggestedTasks(draft: CarePlanDraft): List<String>
}

class DemoAiCareAssistant : AiCareAssistant {
    override fun summarizeNotes(notes: List<DailyNote>): List<String> = listOf(
        "Appetite appears lower across several recent notes.",
        "Increased fatigue was mentioned repeatedly.",
        "Sleep was reported as interrupted on multiple occasions."
    )

    override fun analyzePatterns(notes: List<DailyNote>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        notes.forEach { note ->
            note.categories.forEach { c -> counts[c] = (counts[c] ?: 0) + 1 }
        }
        return counts.toList().sortedByDescending { it.second }.toMap()
    }

    override fun generateCarePlanDraft(notes: List<DailyNote>): CarePlanDraft = CarePlanDraft(
        goal = "Support consistent nutrition and energy monitoring.",
        reason = "Based on repeated mentions of reduced appetite and fatigue in recent notes.",
        actions = listOf(
            "Track meal intake daily.",
            "Record appetite observations.",
            "Note changes in energy or mood.",
            "Escalate significant changes per existing care procedures."
        ),
        monitoring = listOf("Nutrition", "Energy", "Mood")
    )

    override fun generateSuggestedTasks(draft: CarePlanDraft): List<String> = listOf(
        "Record breakfast intake",
        "Record lunch intake",
        "Record appetite observation",
        "Review weekly nutrition pattern"
    )
}

/** Placeholder for a future backend-connected implementation. Disabled by default. */
class RemoteAiCareAssistant : AiCareAssistant {
    override fun summarizeNotes(notes: List<DailyNote>): List<String> =
        error("Remote AI is not configured. Connect via backend; never embed API keys in the app.")

    override fun analyzePatterns(notes: List<DailyNote>): Map<String, Int> =
        error("Remote AI is not configured.")

    override fun generateCarePlanDraft(notes: List<DailyNote>): com.example.carebrief.core.model.CarePlanDraft =
        error("Remote AI is not configured.")

    override fun generateSuggestedTasks(draft: com.example.carebrief.core.model.CarePlanDraft): List<String> =
        error("Remote AI is not configured.")
}
