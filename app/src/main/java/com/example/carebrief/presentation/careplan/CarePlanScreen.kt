package com.example.carebrief.presentation.careplan

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.ChipKind
import com.example.carebrief.core.ui.components.DraftBadge
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.components.StatusChip
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.core.ui.theme.SuccessText
import com.example.carebrief.data.ai.DemoAiCareAssistant
import com.example.carebrief.data.local.DemoData

@Composable
fun CarePlanScreen() {
    val ai = DemoAiCareAssistant()
    val draft = ai.generateCarePlanDraft(DemoData.sarahNotes)
    val tasks = ai.generateSuggestedTasks(draft)
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(CareBriefSpacing.md)) {
        Text("Suggested care plan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Sarah Johnson · based on recorded notes", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Spacer(Modifier.height(8.dp))
        DraftBadge()
        Spacer(Modifier.height(12.dp))

        SectionHeader("Goal")
        CareBriefCard {
            Column {
                Text(draft.goal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(draft.reason, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("Suggested actions")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.actions.forEach { action ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessText)
                        Text(
                            action,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("Monitoring")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.monitoring.forEach { StatusChip(it, ChipKind.INFO) }
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("Suggested tasks")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Spacer(Modifier.height(12.dp))

        PrimaryButton("Approve care plan (demo)", onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "AI-generated draft. Review before using. Not a diagnosis. Requires human approval.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
    }
}
