package com.example.carebrief.presentation.summary

/** Evidence wording: counts, never fake confidence percentages. */
fun evidenceLabel(count: Int, total: Int): String =
    "Observed in $count of $total recent notes."

/** Cautious concern line for a category. Never a diagnosis. */
fun concernText(category: String, count: Int, total: Int): String = when (category.lowercase()) {
    "nutrition", "appetite" ->
        "Reduced appetite observations appeared in $count of $total recent notes and may warrant review with the appropriate care professional."
    "energy", "fatigue" ->
        "Fatigue or tiredness appeared in $count of $total recent notes and may be useful to discuss with the care team."
    "sleep" ->
        "Interrupted or restless sleep appeared in $count of $total recent notes and may warrant continued tracking."
    else ->
        "$category observations appeared in $count of $total recent notes and may warrant review."
}
