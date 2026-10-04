package com.example.carebrief

import com.example.carebrief.data.DemoCareBriefRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareBriefRepositoryTest {
    @Test
    fun search_filtersByName() = runTest {
        val repo = DemoCareBriefRepository()
        assertEquals(1, repo.searchRecipients("sarah").first().size)
        assertEquals(3, repo.searchRecipients("").first().size)
        assertEquals(0, repo.searchRecipients("zzz").first().size)
    }

    @Test
    fun addNote_prependsAndValidates() = runTest {
        val repo = DemoCareBriefRepository()
        val before = repo.observeNotes("sarah").first().size
        val note = repo.addNote(
            recipientId = "sarah",
            content = "Sarah finished most of lunch and seemed brighter.",
            categories = listOf("Nutrition", "Mood"),
            author = "Caregiver Omar"
        )
        assertTrue(note.content.contains("brighter"))
        assertEquals(before + 1, repo.observeNotes("sarah").first().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addNote_blankThrows() = runTest {
        val repo = DemoCareBriefRepository()
        repo.addNote("sarah", "   ", emptyList(), "Nurse")
    }
}
