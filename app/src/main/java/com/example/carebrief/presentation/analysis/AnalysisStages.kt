package com.example.carebrief.presentation.analysis

data class AnalysisStage(val label: String, val durationMs: Long)

/**
 * Deterministic staged analysis. Fixed total duration so the UI
 * can never hang on a fake endless progress bar.
 */
object AnalysisStages {
    val stages = listOf(
        AnalysisStage("Reviewing recent notes", 900),
        AnalysisStage("Identifying recurring observations", 1000),
        AnalysisStage("Organizing key information", 900),
        AnalysisStage("Preparing care-plan draft", 900)
    )

    val totalDurationMs: Long = stages.sumOf { it.durationMs }
}
