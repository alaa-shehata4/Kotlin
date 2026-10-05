package com.example.carebrief.data.local

import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.model.PlanStatus

object DemoData {
    val recipients = listOf(
        CareRecipient(
            id = "sarah",
            name = "Sarah Johnson",
            age = 72,
            careStatus = "Stable · Home care",
            planStatus = PlanStatus.DRAFT,
            lastNoteLabel = "Today, 09:30",
            pendingTasks = 4,
            initials = "SJ"
        ),
        CareRecipient(
            id = "michael",
            name = "Michael Carter",
            age = 68,
            careStatus = "Stable · Assisted living",
            planStatus = PlanStatus.ACTIVE,
            lastNoteLabel = "Today, 15:40",
            pendingTasks = 2,
            initials = "MC"
        ),
        CareRecipient(
            id = "amina",
            name = "Amina Hassan",
            age = 75,
            careStatus = "Observation · Home care",
            planStatus = PlanStatus.NONE,
            lastNoteLabel = "Today, 10:20",
            pendingTasks = 1,
            initials = "AH"
        )
    )

    val sarahNotes = listOf(
        DailyNote(
            id = "n1",
            recipientId = "sarah",
            dayLabel = "Today",
            timeLabel = "09:30",
            author = "Nurse Layla",
            content = "Sarah ate approximately half of her breakfast and said she wasn't very hungry. Seemed more tired than usual.",
            categories = listOf("Nutrition", "Energy")
        ),
        DailyNote(
            id = "n2",
            recipientId = "sarah",
            dayLabel = "Yesterday",
            timeLabel = "18:10",
            author = "Caregiver Omar",
            content = "Left part of lunch untouched. Requested to rest early in the evening. Sleep described as interrupted.",
            categories = listOf("Nutrition", "Sleep")
        ),
        DailyNote(
            id = "n3",
            recipientId = "sarah",
            dayLabel = "2 days ago",
            timeLabel = "12:45",
            author = "Nurse Layla",
            content = "Ate very little at dinner and mentioned feeling fatigued. Mood quieter than usual.",
            categories = listOf("Nutrition", "Mood")
        ),
        DailyNote(
            id = "n4",
            recipientId = "sarah",
            dayLabel = "3 days ago",
            timeLabel = "08:50",
            author = "Caregiver Omar",
            content = "Breakfast intake reduced again. Drank fluids well. Mobility steady with usual support.",
            categories = listOf("Nutrition", "Mobility")
        )
    )

    val michaelNotes = listOf(
        DailyNote(
            id = "m0",
            recipientId = "michael",
            dayLabel = "Today",
            timeLabel = "15:40",
            author = "Caregiver Omar",
            content = "Michael was restless during the afternoon activity and needed gentle redirection. Settled with music.",
            categories = listOf("Behavior", "Mood")
        ),
        DailyNote(
            id = "m1",
            recipientId = "michael",
            dayLabel = "Yesterday",
            timeLabel = "18:05",
            author = "Nurse Layla",
            content = "Michael joined the evening walk and kept a steady pace. Appetite good at dinner.",
            categories = listOf("Mobility", "Nutrition")
        ),
        DailyNote(
            id = "m2",
            recipientId = "michael",
            dayLabel = "2 days ago",
            timeLabel = "09:15",
            author = "Caregiver Omar",
            content = "Morning medication taken on time. Mood positive and engaged during activities.",
            categories = listOf("Medication adherence", "Mood")
        )
    )

    val aminaNotes = listOf(
        DailyNote(
            id = "a0",
            recipientId = "amina",
            dayLabel = "Today",
            timeLabel = "10:20",
            author = "Nurse Layla",
            content = "Morning walk went well with usual support. Appetite steady at breakfast.",
            categories = listOf("Mobility", "Nutrition")
        ),
        DailyNote(
            id = "a1",
            recipientId = "amina",
            dayLabel = "2 days ago",
            timeLabel = "14:20",
            author = "Nurse Layla",
            content = "Amina napped in the afternoon after a restless night. Evening mood calm.",
            categories = listOf("Sleep", "Mood")
        )
    )

}
