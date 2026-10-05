package com.example.carebrief

import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.data.buildExportJson
import com.example.carebrief.data.local.DemoData
import com.example.carebrief.notifications.reviewReminderText
import com.example.carebrief.notifications.taskReminderText
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDataTest {
    private val recipient = CareRecipient(
        "sarah", "Sarah Johnson", 72, "Stable · Home care",
        PlanStatus.DRAFT, "Today, 09:30", 4, "SJ"
    )

    @Test
    fun export_containsPeopleAndNotes() {
        val json = buildExportJson(listOf(recipient), DemoData.sarahNotes)
        assertTrue(json.contains("Sarah Johnson"))
        assertTrue(json.contains("breakfast"))
        assertTrue(json.contains("CareBrief"))
    }

    @Test
    fun taskReminder_countsPending() {
        assertTrue(taskReminderText(3).contains("3"))
        assertTrue(taskReminderText(0).contains("caught up"))
    }

    @Test
    fun reviewReminder_namesDrafts() {
        assertTrue(reviewReminderText(2).contains("2"))
        assertTrue(reviewReminderText(0).contains("up to date"))
    }
}
