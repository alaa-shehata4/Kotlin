package com.example.carebrief.data

/** Pure validation for note entry. Keep UI thin; test through this seam. */
object NoteValidator {
    sealed interface Result {
        data object Valid : Result
        data class Invalid(val message: String) : Result
    }

    fun validate(content: String, categories: List<String>): Result {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.Invalid("Please describe what you observed.")
        if (trimmed.length < 10) return Result.Invalid("Please add a little more detail (at least 10 characters).")
        return Result.Valid
    }
}
