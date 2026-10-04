package com.example.carebrief.presentation.summary

/** Evidence wording: counts, never fake confidence percentages. */
fun evidenceLabel(count: Int, total: Int): String =
    "Observed in $count of $total recent notes."

/** Cautious concern line for a category. Never a diagnosis. */
fun concernText(category: String, count: Int, total: Int): String = when (category.lowercase()) {
    "nutrition", "appetite" ->
        "Reduced appetite was noted repeatedly (${evidenceLabel(count, total).trimEnd('.').lowercase()}). " +
            "Consider reviewing the pattern with the appropriate care professional if concerns persist."
    "energy", "fatigue" ->
        "Fatigue was mentioned repeatedly (${evidenceLabel(count, total).trimEnd('.').lowercase()}). " +
            "Continued observation of energy and rest may be useful."
    "sleep" ->
        "Interrupted sleep was reported more than once (${evidenceLabel(count, total).trimEnd('.').lowercase()}). " +
            "Keeping track of sleep patterns may help future reviews."
    else ->
        "$category came up more than once (${evidenceLabel(count, total).trimEnd('.').lowercase()}). " +
            "It may be worth keeping an eye on this area."
}
