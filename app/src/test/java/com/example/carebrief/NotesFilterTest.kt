package com.example.carebrief

import com.example.carebrief.data.local.DemoData
import com.example.carebrief.presentation.notes.filterNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesFilterTest {
    @Test
    fun category_allReturnsEverything() {
        assertEquals(4, filterNotes(DemoData.sarahNotes, category = null, day = null).size)
    }

    @Test
    fun category_filters() {
        val sleep = filterNotes(DemoData.sarahNotes, category = "Sleep", day = null)
        assertEquals(1, sleep.size)
        assertTrue(sleep.all { "Sleep" in it.categories })
    }

    @Test
    fun day_filters() {
        val today = filterNotes(DemoData.sarahNotes, category = null, day = "Today")
        assertEquals(1, today.size)
    }

    @Test
    fun combined_narrows() {
        val result = filterNotes(DemoData.sarahNotes, category = "Nutrition", day = "Today")
        assertEquals(1, result.size)
    }
}
