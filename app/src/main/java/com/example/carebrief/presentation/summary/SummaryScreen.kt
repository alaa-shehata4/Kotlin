package com.example.carebrief.presentation.summary

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.ChipKind
import com.example.carebrief.core.ui.components.DraftBadge
import com.example.carebrief.core.ui.components.EmptyState
import com.example.carebrief.core.ui.components.ErrorState
import com.example.carebrief.core.ui.components.EvidenceCard
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.components.StatusChip
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.ai.AiCareAssistant
import com.example.carebrief.data.ai.AiProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface SummaryUiState {
    data object Loading : SummaryUiState
    data class Empty(val recipientName: String) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
    data class Ready(
        val recipientName: String,
        val observations: List<String>,
        val patterns: List<Pair<String, Int>>,
        val totalNotes: Int,
        val evidence: List<InsightEvidence>
    ) : SummaryUiState
}

class SummaryViewModel(
    private val recipientId: String,
    private val recipientName: String,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared,
    private val ai: AiCareAssistant = AiProviders.current()
) : ViewModel() {
    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<SummaryUiState> = combine(
        repo.observeNotes(recipientId),
        retryTick
    ) { notes, _ ->
        try {
            if (notes.isEmpty()) SummaryUiState.Empty(recipientName)
            else {
                val patterns = ai.analyzePatterns(notes).entries.map { it.key to it.value }
                val concerns = ai.analyzePotentialConcerns(notes)
                SummaryUiState.Ready(
                    recipientName = recipientName,
                    observations = ai.summarizeNotes(notes),
                    patterns = patterns,
                    totalNotes = notes.size,
                    evidence = concerns.map { concern ->
                        buildInsightEvidence(concern.category, notes, concern.supportingNoteIds)
                    }
                )
            }
        } catch (e: Exception) {
            SummaryUiState.Error("We couldn't generate the analysis right now.")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState.Loading)

    fun retry() { retryTick.value += 1 }

    class Factory(
        private val recipientId: String,
        private val recipientName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SummaryViewModel(recipientId, recipientName) as T
    }
}

@Composable
fun SummaryScreen(
    recipientId: String,
    recipientName: String,
    onBack: () -> Unit,
    onAddNote: () -> Unit,
    onViewPlan: () -> Unit,
    vm: SummaryViewModel = viewModel(factory = SummaryViewModel.Factory(recipientId, recipientName))
) {
    val state by vm.uiState.collectAsState()
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("AI Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        when (val s = state) {
            SummaryUiState.Loading -> Column(Modifier.padding(CareBriefSpacing.md)) { LoadingRow("Preparing summary") }
            is SummaryUiState.Empty -> Column(Modifier.padding(CareBriefSpacing.md)) {
                DraftBadge()
                Spacer(Modifier.height(12.dp))
                EmptyState("No notes yet.", "Add notes for ${s.recipientName} to generate a summary.", "Add your first note", onAddNote)
            }
            is SummaryUiState.Error -> Column(Modifier.padding(CareBriefSpacing.md)) {
                ErrorState(s.message, onRetry = { vm.retry() })
                Spacer(Modifier.height(8.dp))
                Text("You can also continue reviewing notes.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
            is SummaryUiState.Ready -> androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            ) {
                Column(
                    Modifier.verticalScroll(scroll).padding(CareBriefSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${s.recipientName} · based on ${s.totalNotes} recorded notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
                DraftBadge()
                Text(
                    "Suggested patterns only. Review before using. Not a diagnosis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )

                SectionHeader("Key observations")
                s.observations.forEach { obs ->
                    CareBriefCard {
                        Row {
                            Text("•  ", style = MaterialTheme.typography.bodyLarge)
                            Text(obs, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                SectionHeader("Emerging patterns")
                CareBriefCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.patterns.forEach { (category, count) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        evidenceLabel(count, s.totalNotes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkSecondary
                                    )
                                }
                                StatusChip("Mentioned $count×", ChipKind.INFO)
                            }
                        }
                    }
                }

                SectionHeader("Potential areas to review")
                if (s.evidence.isEmpty()) {
                    Text(
                        "No potential concerns were identified in the available notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                } else {
                    s.evidence.forEach { item ->
                        EvidenceCard(
                            observation = "${item.category} — ${item.observation}",
                            evidence = "Observed in ${item.matchCount} of ${item.totalNotes} recent notes.",
                            frequency = item.frequencyLabel,
                            supportingQuotes = item.supportingNotes.map {
                                "${it.dayLabel} ${it.timeLabel} · ${it.content}"
                            }
                        )
                    }
                }

                PrimaryButton("View suggested care plan", onClick = onViewPlan, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
