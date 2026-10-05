package com.example.carebrief.core.ui.state

/**
 * Phase 28 — explicit UI state contract.
 *
 * Every major screen exposes exactly one [StateFlow] of its sealed screen
 * state with [Loading], [Empty], [Success]/content and [Error] variants.
 * ViewModels stay small: each maps repository Flow(s) into its state and
 * exposes `retry()` — no god-ViewModels, no nullable-guessing in Compose.
 */
sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>
    data class Empty(val message: String = "") : ScreenState<Nothing>
    data class Success<T>(val data: T) : ScreenState<T>
    data class Error(val message: String, val retryable: Boolean = true) : ScreenState<Nothing>
}

fun <T> ScreenState<T>.dataOrNull(): T? = (this as? ScreenState.Success)?.data

const val GENERIC_LOAD_ERROR = "Something went wrong while loading this information."
const val AI_ANALYSIS_ERROR = "We couldn't generate the analysis right now."
