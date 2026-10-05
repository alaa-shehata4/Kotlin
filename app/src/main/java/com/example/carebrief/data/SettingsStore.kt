package com.example.carebrief.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("carebrief_settings")
private val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
private val THEME = stringPreferencesKey("theme_mode")
private val START = stringPreferencesKey("start_screen")
private val AI_PROVIDER = stringPreferencesKey("ai_provider")
private val ANALYSIS_RANGE = stringPreferencesKey("analysis_range")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class StartScreen { HOME, PEOPLE, NOTES }
enum class AiProviderSetting { DEMO, REMOTE }
enum class AnalysisRange { LAST_7_DAYS, ALL_NOTES }

class SettingsStore(private val context: Context) {
    val notificationsEnabled: Flow<Boolean> =
        context.settingsStore.data.map { it[NOTIFICATIONS] ?: true }

    val themeMode: Flow<ThemeMode> =
        context.settingsStore.data.map {
            runCatching { ThemeMode.valueOf(it[THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
        }

    val startScreen: Flow<StartScreen> =
        context.settingsStore.data.map {
            runCatching { StartScreen.valueOf(it[START] ?: "HOME") }.getOrDefault(StartScreen.HOME)
        }

    val aiProvider: Flow<AiProviderSetting> =
        context.settingsStore.data.map {
            runCatching { AiProviderSetting.valueOf(it[AI_PROVIDER] ?: "DEMO") }
                .getOrDefault(AiProviderSetting.DEMO)
        }

    val analysisRange: Flow<AnalysisRange> =
        context.settingsStore.data.map {
            runCatching { AnalysisRange.valueOf(it[ANALYSIS_RANGE] ?: AnalysisRange.LAST_7_DAYS.name) }
                .getOrDefault(AnalysisRange.LAST_7_DAYS)
        }

    suspend fun setNotifications(enabled: Boolean) {
        context.settingsStore.edit { it[NOTIFICATIONS] = enabled }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsStore.edit { it[THEME] = mode.name }
    }

    suspend fun setStartScreen(screen: StartScreen) {
        context.settingsStore.edit { it[START] = screen.name }
    }

    suspend fun setAiProvider(provider: AiProviderSetting) {
        context.settingsStore.edit { it[AI_PROVIDER] = provider.name }
    }

    suspend fun setAnalysisRange(range: AnalysisRange) {
        context.settingsStore.edit { it[ANALYSIS_RANGE] = range.name }
    }
}
