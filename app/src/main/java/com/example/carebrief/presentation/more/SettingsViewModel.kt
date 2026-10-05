package com.example.carebrief.presentation.more

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carebrief.data.AiProviderSetting
import com.example.carebrief.data.AnalysisRange
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.SettingsStore
import com.example.carebrief.data.StartScreen
import com.example.carebrief.data.ThemeMode
import com.example.carebrief.data.ai.AiProviders
import com.example.carebrief.data.buildExportJson
import com.example.carebrief.data.TaskStore
import com.example.carebrief.notifications.ReminderScheduler
import com.example.carebrief.notifications.canNotify
import com.example.carebrief.notifications.notifyTaskReminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val startScreen: StartScreen = StartScreen.HOME,
    val aiProvider: AiProviderSetting = AiProviderSetting.DEMO,
    val analysisRange: AnalysisRange = AnalysisRange.LAST_7_DAYS,
    val pendingTasks: Int = 0
)

class SettingsViewModel(
    private val appContext: Context,
    private val settings: SettingsStore,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared
) : ViewModel() {
    val uiState = combine(
        settings.notificationsEnabled,
        settings.themeMode,
        settings.startScreen,
        settings.aiProvider,
        settings.analysisRange,
        repo.observeRecipients()
    ) { array: Array<Any> ->
        SettingsUiState(
            notificationsEnabled = array[0] as Boolean,
            themeMode = array[1] as ThemeMode,
            startScreen = array[2] as StartScreen,
            aiProvider = array[3] as AiProviderSetting,
            analysisRange = array[4] as AnalysisRange,
            pendingTasks = (array[5] as List<com.example.carebrief.core.model.CareRecipient>).sumOf { it.pendingTasks }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settings.setNotifications(enabled)
            ReminderScheduler.setEnabled(appContext, enabled)
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setTheme(mode) }
    }

    fun setStartScreen(screen: StartScreen) {
        viewModelScope.launch { settings.setStartScreen(screen) }
    }

    fun setAiProvider(provider: AiProviderSetting) {
        viewModelScope.launch {
            settings.setAiProvider(provider)
            AiProviders.useRemote = provider == AiProviderSetting.REMOTE
        }
    }

    fun setAnalysisRange(range: AnalysisRange) {
        viewModelScope.launch { settings.setAnalysisRange(range) }
    }

    fun exportText(onResult: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val recipients = repo.observeRecipients().first()
                val notes = repo.observeAllNotes().first()
                buildExportJson(recipients, notes)
            }.onSuccess(onResult)
                .onFailure { onError("Export failed. Please try again.") }
        }
    }

    fun resetDemoData(onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repo.resetDemoData()
                TaskStore.shared.clear()
                CarePlanStore.shared.clear()
            }.onSuccess { onDone() }
                .onFailure { onError("Reset failed. Please try again.") }
        }
    }

    /** Returns false when system notifications are blocked so UI can explain. */
    fun sendTestReminder(): Boolean {
        if (!canNotify(appContext)) return false
        notifyTaskReminder(appContext, uiState.value.pendingTasks)
        return true
    }

    class Factory(
        private val appContext: Context,
        private val settings: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(appContext, settings) as T
    }
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_TEXT, text)
        .setType("text/plain")
    context.startActivity(Intent.createChooser(intent, "Export CareBrief data"))
}
