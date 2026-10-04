package com.example.carebrief

import com.example.carebrief.presentation.analysis.AnalysisStages
import com.example.carebrief.presentation.notes.TimelineGrouper
import com.example.carebrief.presentation.summary.evidenceLabel
import com.example.carebrief.data.local.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phases789Test {
    @Test
    fun timeline_groupsByDayPreservingOrder() {
        val groups = TimelineGrouper.group(DemoData.sarahNotes)
        assertEquals(4, groups.size)
        assertEquals("Today", groups[0].dayLabel)
        assertEquals(1, groups[0].notes.size)
    }

    @Test
    fun analysis_hasFourDeterministicStages() {
        val stages = AnalysisStages.stages
        assertEquals(4, stages.size)
        assertEquals("Reviewing recent notes", stages[0].label)
        assertEquals("Preparing care-plan draft", stages[3].label)
        assertTrue(AnalysisStages.totalDurationMs in 2000..6000)
    }

    @Test
    fun evidence_usesCountsNotPercentages() {
        assertEquals("Observed in 3 of 5 recent notes.", evidenceLabel(3, 5))
        assertTrue(!evidenceLabel(3, 5).contains("%"))
    }
}
