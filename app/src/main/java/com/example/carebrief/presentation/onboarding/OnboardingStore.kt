package com.example.carebrief.presentation.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("carebrief_prefs")
private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

class OnboardingStore(private val context: Context) {
    val completed: Flow<Boolean> = context.store.data.map { it[ONBOARDING_DONE] == true }

    suspend fun setCompleted(done: Boolean) {
        context.store.edit { it[ONBOARDING_DONE] = done }
    }
}
