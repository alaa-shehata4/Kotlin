package com.example.carebrief.presentation.careplan

/** Pure list + validation helpers for the plan editor. Tested seam. */
object PlanEditOps {
    /** Moves item from [from] to [to]; returns original on out-of-range. */
    fun move(list: List<String>, from: Int, to: Int): List<String> {
        if (from !in list.indices || to !in list.indices) return list
        if (from == to) return list
        val mutable = list.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable.toList()
    }
}

/** Returns an error message, or null when the plan is valid. */
fun validatePlan(goal: String, actions: List<String>, monitoring: List<String>): String? {
    if (goal.trim().isEmpty()) return "Please describe the goal."
    if (actions.none { it.trim().isNotEmpty() }) return "Please keep at least one action."
    if (monitoring.isEmpty()) return "Please select at least one monitoring indicator."
    return null
}
