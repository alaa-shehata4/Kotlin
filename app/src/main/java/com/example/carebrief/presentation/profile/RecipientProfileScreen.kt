package com.example.carebrief.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.CareRecipientAvatar
import com.example.carebrief.core.ui.components.ChipKind
import com.example.carebrief.core.ui.components.DraftBadge
import com.example.carebrief.core.ui.components.EmptyState
import com.example.carebrief.core.ui.components.InsightCard
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.components.NoteCard
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SecondaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.components.StatusChip
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.ai.DemoAiCareAssistant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val recipient: CareRecipient? = null,
    val notes: List<DailyNote> = emptyList()
)

class RecipientProfileViewModel(
    private val recipientId: String,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared
) : ViewModel() {
    val uiState = combine(
        repo.observeRecipient(recipientId),
        repo.observeNotes(recipientId)
    ) { recipient, notes -> ProfileUiState(recipient, notes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    class Factory(private val recipientId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecipientProfileViewModel(recipientId) as T
    }
}

@Composable
fun RecipientProfileScreen(
    recipientId: String,
    onBack: () -> Unit,
    onAddNote: (String) -> Unit,
    onAnalyze: () -> Unit,
    onViewPlan: () -> Unit,
    vm: RecipientProfileViewModel = viewModel(factory = RecipientProfileViewModel.Factory(recipientId))
) {
    val state by vm.uiState.collectAsState()
    val scroll = rememberScrollState()
    val ai = remember { DemoAiCareAssistant() }

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(CareBriefSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))

        val person = state.recipient
        if (person == null) {
            LoadingRow("Loading profile")
            return@Column
        }

        // Overview hero
        CareBriefCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CareRecipientAvatar(person.initials, modifier = Modifier.padding(end = 4.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(person.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${person.age} years old · ${person.careStatus}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (label, kind) = when (person.planStatus) {
                            PlanStatus.ACTIVE -> "Care plan active" to ChipKind.ACTIVE
                            PlanStatus.DRAFT -> "Draft — review required" to ChipKind.DRAFT
                            PlanStatus.NONE -> "No care plan" to ChipKind.NEUTRAL
                        }
                        StatusChip(label, kind)
                        StatusChip("${person.pendingTasks} pending tasks", ChipKind.NEUTRAL)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Add daily note", onClick = { onAddNote(person.id) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        // Recent observations
        SectionHeader("Recent observations", actionLabel = "View all", onAction = onAnalyze)
        if (state.notes.isEmpty()) {
            EmptyState("No notes yet.", "Add your first note to start the timeline.", "Add note") { onAddNote(person.id) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.notes.take(3).forEach { note ->
                    NoteCard(note.dayLabel, note.timeLabel, note.author, note.content, note.categories)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Current concerns (AI, cautious)
        SectionHeader("Current concerns")
        DraftBadge()
        Spacer(Modifier.height(8.dp))
        val patterns = ai.analyzePatterns(state.notes)
        if (patterns.isEmpty()) {
            Text("Not enough notes yet to suggest areas to review.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                patterns.entries.take(2).forEach { (category, count) ->
                    InsightCard(
                        title = "Potential area to review · $category",
                        description = "Observed in $count of ${state.notes.size} recent notes. May warrant additional observation.",
                        icon = Icons.Filled.Warning
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Care plan
        SectionHeader("Care plan")
        CareBriefCard {
            Column {
                val planLabel = when (person.planStatus) {
                    PlanStatus.ACTIVE -> "Active care plan"
                    PlanStatus.DRAFT -> "Draft care plan awaiting review"
                    PlanStatus.NONE -> "No care plan yet"
                }
                Text(planLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Based on recorded notes. Review before using. Not a diagnosis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Open care plan", onViewPlan, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))

        // Tasks
        SectionHeader("Tasks")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${person.pendingTasks} pending tasks", style = MaterialTheme.typography.titleMedium)
                Text("Today · meal intake logs and observations", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Activity
        SectionHeader("Activity")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.notes.take(4).forEach { note ->
                    Column {
                        Text("Note · ${note.dayLabel} ${note.timeLabel}", style = MaterialTheme.typography.titleMedium)
                        Text(note.categories.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
