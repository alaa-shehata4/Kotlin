package com.example.carebrief.presentation.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.ui.components.ChipKind
import com.example.carebrief.core.ui.components.DraftBadge
import com.example.carebrief.core.ui.components.EmptyState
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.components.NoteCard
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SecondaryButton
import com.example.carebrief.core.ui.components.StatusChip
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.core.ui.theme.TealPrimary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.local.DemoData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(
    initialRecipientId: String = "sarah",
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared
) : ViewModel() {
    private val recipientId = MutableStateFlow(initialRecipientId)

    fun selectRecipient(id: String) { recipientId.value = id }
    fun currentRecipient(): StateFlow<String> = recipientId

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<DailyNote>?> =
        recipientId.flatMapLatest { repo.observeNotes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val groups: StateFlow<List<TimelineGroup>?> =
        combine(notes) { (list) ->
            list?.let { TimelineGrouper.group(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    class Factory(private val recipientId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotesViewModel(recipientId) as T
    }
}

@Composable
fun NotesScreen(
    recipientId: String = "sarah",
    onAddNote: (String) -> Unit,
    onAnalyze: (String) -> Unit,
    vm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(recipientId))
) {
    val groups by vm.groups.collectAsState()
    val selectedId by vm.currentRecipient().collectAsState()
    val recipientName = remember(selectedId) {
        DemoData.recipients.find { it.id == selectedId }?.name ?: "Sarah Johnson"
    }
    var analyzedIds by remember { mutableStateOf(setOf<String>()) }

    Column(Modifier.fillMaxSize().padding(CareBriefSpacing.md)) {
        Text("Daily notes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("$recipientName · recent observations", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Spacer(Modifier.height(8.dp))
        // Recipient switcher keeps the timeline useful for all demo people.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoData.recipients.forEach { person ->
                FilterChip(
                    selected = person.id == selectedId,
                    onClick = { vm.selectRecipient(person.id) },
                    label = { Text(person.name.substringBefore(" ")) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        DraftBadge()
        Spacer(Modifier.height(12.dp))
        when (val list = groups) {
            null -> LoadingRow("Loading notes")
            else -> {
                if (list.isEmpty()) {
                    EmptyState("No notes yet.", "Add your first note to start the timeline.", "Add your first note") {
                        onAddNote(selectedId)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm),
                        modifier = Modifier.weight(1f)
                    ) {
                        list.forEach { group ->
                            item(key = "header-${group.dayLabel}-$selectedId") {
                                TimelineDayHeader(group.dayLabel.uppercase(), "${group.notes.size} note(s)")
                            }
                            group.notes.forEach { note ->
                                item(key = note.id) {
                                    TimelineEntry(
                                        isNew = note.id !in analyzedIds && group.dayLabel == "Today",
                                        note = note
                                    )
                                }
                            }
                        }
                    }
                    // Mark visible notes as included once rendered (demo AI status).
                    analyzedIds = list.flatMap { it.notes }.map { it.id }.toSet()
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Add daily note", onClick = { onAddNote(selectedId) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Analyze recent notes", onClick = { onAnalyze(selectedId) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TimelineDayHeader(day: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(TealPrimary)
        )
        Spacer(Modifier.width(8.dp))
        Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
    }
}

@Composable
private fun TimelineEntry(isNew: Boolean, note: DailyNote) {
    Row {
        // Chronology rail.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 14.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(TealPrimary))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(2.dp).height(56.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${note.timeLabel} · ${note.author}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(
                    if (isNew) "New — included next run" else "Included in draft",
                    if (isNew) ChipKind.DRAFT else ChipKind.ACTIVE
                )
            }
            Spacer(Modifier.height(6.dp))
            NoteCard(note.dayLabel, note.timeLabel, note.author, note.content, note.categories)
        }
    }
}
