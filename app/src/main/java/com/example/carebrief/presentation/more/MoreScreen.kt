package com.example.carebrief.presentation.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary

@Composable
fun MoreScreen(onResetOnboarding: () -> Unit) {
    var notifications by remember { mutableStateOf(true) }
    var demoMode by remember { mutableStateOf(true) }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(CareBriefSpacing.md)) {
        Text("More", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Preferences, AI, data and about.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Spacer(Modifier.height(12.dp))

        SectionHeader("Preferences")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notifications", style = MaterialTheme.typography.titleMedium)
                        Text("Task and review reminders", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    }
                    Switch(checked = notifications, onCheckedChange = { notifications = it })
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("AI")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo AI provider", style = MaterialTheme.typography.titleMedium)
                        Text("Deterministic offline drafts", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    }
                    Switch(checked = demoMode, onCheckedChange = { demoMode = it })
                }
                Text(
                    "Remote AI connects later via a backend. Secrets are never embedded in the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("About")
        CareBriefCard {
            Column {
                Text("CareBrief 1.0 · Assistive prototype", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "AI-generated content is provided as a draft and should be reviewed by an appropriate human professional. Not a medical diagnostic system. Demo profiles are fictional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        androidx.compose.material3.TextButton(onClick = onResetOnboarding) {
            Text("Replay onboarding")
        }
    }
}
