package com.example.carebrief.presentation.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.SecondaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.AiProviderSetting
import com.example.carebrief.data.AnalysisRange
import com.example.carebrief.data.SettingsStore
import com.example.carebrief.data.StartScreen
import com.example.carebrief.data.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(
    onResetOnboarding: () -> Unit,
    settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            LocalContext.current.applicationContext,
            SettingsStore(LocalContext.current.applicationContext)
        )
    )
) {
    val state by settingsVm.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scroll = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(CareBriefSpacing.md),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Preferences, AI, data and about.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)

            SectionHeader("Preferences")
            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Notifications", style = MaterialTheme.typography.titleMedium)
                            Text("Task and review reminders", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                        }
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = {
                                settingsVm.setNotifications(it)
                                toast(if (it) "Reminders on" else "Reminders off")
                            }
                        )
                    }
                    SecondaryButton(
                        "Send test reminder",
                        onClick = {
                            if (settingsVm.sendTestReminder()) toast("Reminder sent")
                            else toast("System notifications are blocked — enable them in Android settings")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.themeMode == mode,
                                onClick = { settingsVm.setTheme(mode) },
                                label = {
                                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }
                }
            }

            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Start screen", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartScreen.entries.forEach { screen ->
                            FilterChip(
                                selected = state.startScreen == screen,
                                onClick = { settingsVm.setStartScreen(screen) },
                                label = {
                                    Text(screen.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }
                    Text(
                        "Applies the next time the app starts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }

            SectionHeader("AI")
            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI provider", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.aiProvider == AiProviderSetting.DEMO,
                            onClick = { settingsVm.setAiProvider(AiProviderSetting.DEMO) },
                            label = { Text("Demo AI") }
                        )
                        FilterChip(
                            selected = state.aiProvider == AiProviderSetting.REMOTE,
                            onClick = { settingsVm.setAiProvider(AiProviderSetting.REMOTE) },
                            label = { Text("Remote") }
                        )
                    }
                    Text(
                        "Demo AI works fully offline with deterministic drafts. " +
                            "Remote requires a backend connection and is not available in this build — " +
                            "analysis screens will report that instead of guessing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Text("Analysis behavior", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnalysisRange.entries.forEach { range ->
                            FilterChip(
                                selected = state.analysisRange == range,
                                onClick = { settingsVm.setAnalysisRange(range) },
                                label = {
                                    Text(if (range == AnalysisRange.LAST_7_DAYS) "Last 7 days" else "All notes")
                                }
                            )
                        }
                    }
                    Text(
                        "Choose which notes are included in AI summaries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Text(
                        "AI-generated content is provided as a draft and should be reviewed by an " +
                            "appropriate human professional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }

            SectionHeader("Data")
            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        "Export data",
                        onClick = {
                            settingsVm.exportText(
                                onResult = { json -> shareText(context, json) },
                                onError = { message -> toast(message) }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton(
                        "Reset demo data",
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Export shares everyone, all notes and current drafts as JSON. " +
                            "Only share exported files through trusted channels — they " +
                            "contain the observations you recorded. " +
                            "Reset restores the original demo dataset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }

            SectionHeader("Privacy")
            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your data stays on this device", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notes, drafts, plans and tasks are stored locally in an on-device " +
                            "database; preferences stay in on-device storage. The app has no " +
                            "backend, sends no analytics, and the demo AI runs fully offline — " +
                            "nothing you record leaves the phone unless you explicitly export it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Text(
                        "Demo profiles are fictional. AI suggestions are labeled DRAFT and are " +
                            "kept separate from reviewed care plans — a plan only becomes active " +
                            "after a caregiver confirms it. Nothing here is a medical diagnosis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }

            SectionHeader("About")
            CareBriefCard {
                Column {
                    Text("CareBrief 1.0 · Assistive prototype", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Caregivers record daily observations; the app summarizes them and drafts " +
                            "structured care plans for human review. Not a medical diagnostic system. " +
                            "Demo profiles are fictional and all data stays on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }

            TextButton(onClick = onResetOnboarding) {
                Text("Replay onboarding")
            }
            Spacer(Modifier.height(8.dp))
        }
        SnackbarHost(snackbar, Modifier.padding(16.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset demo data?") },
            text = {
                Text("This clears notes you added, edited plans and tasks, then restores the original demo dataset.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    settingsVm.resetDemoData(
                        onDone = { toast("Demo data restored") },
                        onError = { message -> toast(message) }
                    )
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
