package com.example.carebrief.notifications

/** Pure reminder copy. Tested seam — Android delivery lives in Notifier. */
fun taskReminderText(pending: Int): String = if (pending <= 0) {
    "You're all caught up. No pending care tasks."
} else {
    "You have $pending pending care ${if (pending == 1) "task" else "tasks"}. A quick review keeps care on track."
}

fun reviewReminderText(drafts: Int): String = if (drafts <= 0) {
    "Care-plan reviews are up to date. Nothing is due."
} else {
    "$drafts care-plan ${if (drafts == 1) "review is" else "reviews are"} due or overdue."
}
