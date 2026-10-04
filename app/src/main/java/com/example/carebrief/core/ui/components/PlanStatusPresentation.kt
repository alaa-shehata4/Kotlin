package com.example.carebrief.core.ui.components

import com.example.carebrief.core.model.PlanStatus

data class PlanStatusPresentation(
    val listLabel: String,
    val profileLabel: String,
    val kind: ChipKind
)

fun PlanStatus.presentation() = when (this) {
    PlanStatus.ACTIVE -> PlanStatusPresentation("Care plan active", "Active care plan", ChipKind.ACTIVE)
    PlanStatus.DRAFT -> PlanStatusPresentation("Draft needs review", "Draft care plan awaiting review", ChipKind.DRAFT)
    PlanStatus.NONE -> PlanStatusPresentation("No care plan", "No care plan yet", ChipKind.NEUTRAL)
}
