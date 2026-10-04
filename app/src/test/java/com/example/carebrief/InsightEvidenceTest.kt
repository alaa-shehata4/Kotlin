package com.example.carebrief

import com.example.carebrief.data.local.DemoData
import com.example.carebrief.presentation.summary.buildInsightEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightEvidenceTest {
    @Test
    fun nutrition_linksSupportingNotes() {
        val evidence = buildInsightEvidence("Nutrition", DemoData.sarahNotes)
        assertEquals(4, evidence.matchCount)
        assertEquals(4, evidence.supportingNotes.size)
        assertTrue(evidence.frequencyLabel.contains("4 of 4"))
        assertFalse(evidence.frequencyLabel.contains("%"))
    }

    @Test
    fun unknownCategory_isEmptyButHonest() {
        val evidence = buildInsightEvidence("Vision", DemoData.sarahNotes)
        assertEquals(0, evidence.matchCount)
        assertTrue(evidence.supportingNotes.isEmpty())
    }

    @Test
    fun frequency_neverUsesConfidencePercent() {
        val evidence = buildInsightEvidence("Sleep", DemoData.sarahNotes)
        assertFalse(evidence.frequencyLabel.contains("%"))
        assertFalse(evidence.frequencyLabel.contains("confidence", ignoreCase = true))
    }
}
