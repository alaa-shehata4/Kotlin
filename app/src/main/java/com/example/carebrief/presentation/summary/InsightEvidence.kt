package com.example.carebrief.presentation.summary

import com.example.carebrief.core.model.DailyNote

/**
 * Explainable-insight module. Every important insight carries its
 * observation, the supporting note excerpts (evidence), and a
 * count-based frequency — never a fake confidence percentage.
 */
data class InsightEvidence(
    val category: String,
    val observation: String,
    val supportingNotes: List<DailyNote>,
    val matchCount: Int,
    val totalNotes: Int,
    val frequencyLabel: String
)

fun buildInsightEvidence(
    category: String,
    notes: List<DailyNote>,
    supportingNoteIds: List<String>
): InsightEvidence {
    val matches = notes.filter { it.id in supportingNoteIds }
    return InsightEvidence(
        category = category,
        observation = concernText(category, matches.size, notes.size),
        supportingNotes = matches,
        matchCount = matches.size,
        totalNotes = notes.size,
        frequencyLabel = evidenceLabel(matches.size, notes.size)
    )
}
