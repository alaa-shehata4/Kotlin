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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(
    initialRecipientId: String = "sarah",
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared
) : ViewModel() {
    private val recipientId = MutableStateFlow(initialRecipientId)
    private val category = MutableStateFlow<String?>(null)
    private val day = MutableStateFlow<String?>(null)
    private val retryTick = MutableStateFlow(0)
    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError

    fun selectRecipient(id: String) {
        recipientId.value = id
        category.value = null
        day.value = null
    }
    fun currentRecipient(): StateFlow<String> = recipientId
    fun selectCategory(c: String?) { category.value = c }
    fun currentCategory(): StateFlow<String?> = category
    fun selectDay(d: String?) { day.value = d }
    fun currentDay(): StateFlow<String?> = day

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<DailyNote>?> =
        combine(recipientId, retryTick) { id, _ -> id }
            .flatMapLatest { id ->
                repo.observeNotes(id)
                    .catch {
                        _loadError.value = true
                        emit(emptyList())
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun retry() {
        _loadError.value = false
        retryTick.value += 1
    }

    val availableCategories: StateFlow<List<String>> =
        notes.filterNotNull().map { list -> list.flatMap { it.categories }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableDays: StateFlow<List<String>> =
        notes.filterNotNull().map { list -> list.map { it.dayLabel }.distinct() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groups: StateFlow<List<TimelineGroup>?> =
        combine(notes, category, day) { list, c, d ->
            list?.let { TimelineGrouper.group(filterNotes(it, c, d)) }
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
    val allNotes by vm.notes.collectAsState()
    val loadError by vm.loadError.collectAsState()
    val selectedId by vm.currentRecipient().collectAsState()
    val categories by vm.availableCategories.collectAsState()
    val days by vm.availableDays.collectAsState()
    val selectedCategory by vm.currentCategory().collectAsState()
    val selectedDay by vm.currentDay().collectAsState()
    val recipientName = remember(selectedId) {
        DemoData.recipients.find { it.id == selectedId }?.name ?: "Sarah Johnson"
    }
    val analyzedIds by TimelineAnalysisHistory.analyzedNoteIds.collectAsState()

    Column(
        Modifier.fillMaxSize()
            .padding(CareBriefSpacing.md)
            .navigationBarsPadding()
    ) {
        Text(
            "Daily notes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            softWrap = true
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$recipientName · recent observations",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            softWrap = true
        )
        Spacer(Modifier.height(8.dp))
        // Phase 25: LazyRow so small phones / large fonts don't overflow.
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DemoData.recipients.size) { index ->
                val person = DemoData.recipients[index]
                FilterChip(
                    selected = person.id == selectedId,
                    onClick = { vm.selectRecipient(person.id) },
                    label = { Text(person.name.substringBefore(" ")) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        DraftBadge()
        Spacer(Modifier.height(8.dp))
        if (categories.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { vm.selectCategory(null) },
                        label = { Text("All topics") }
                    )
                }
                items(categories.size) { i ->
                    FilterChip(
                        selected = categories[i] == selectedCategory,
                        onClick = {
                            vm.selectCategory(if (categories[i] == selectedCategory) null else categories[i])
                        },
                        label = { Text(categories[i]) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (days.size > 1) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedDay == null,
                        onClick = { vm.selectDay(null) },
                        label = { Text("All dates") }
                    )
                }
                items(days.size) { i ->
                    FilterChip(
                        selected = days[i] == selectedDay,
                        onClick = { vm.selectDay(if (days[i] == selectedDay) null else days[i]) },
                        label = { Text(days[i]) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(12.dp))
        when (val list = groups) {
            null -> LoadingRow("Loading notes")
            else -> {
                if (loadError && list.isEmpty() && allNotes.isNullOrEmpty()) {
                    com.example.carebrief.core.ui.components.ErrorState(
                        "Something went wrong while loading this information.",
                        onRetry = vm::retry
                    )
                } else if (list.isEmpty()) {
                    val filtering = selectedCategory != null || selectedDay != null
                    if (filtering && !allNotes.isNullOrEmpty()) {
                        EmptyState(
                            "No notes match these filters.",
                            "Try a different topic or date.",
                            "Clear filters"
                        ) { vm.selectCategory(null); vm.selectDay(null) }
                    } else {
                        EmptyState("No notes yet.", "Add your first note to start the timeline.", "Add your first note") {
                            onAddNote(selectedId)
                        }
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
                                        isAnalyzed = note.id in analyzedIds,
                                        note = note
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Add daily note", onClick = { onAddNote(selectedId) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Analyze recent notes", onClick = { onAnalyze(selectedId) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        com.example.carebrief.core.network.OfflineBanner()
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
private fun TimelineEntry(isAnalyzed: Boolean, note: DailyNote) {
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
                    if (isAnalyzed) "Included in last analysis" else "Not analyzed",
                    if (isAnalyzed) ChipKind.ACTIVE else ChipKind.NEUTRAL
                )
            }
            Spacer(Modifier.height(6.dp))
            NoteCard(note.dayLabel, note.timeLabel, note.author, note.content, note.categories, note.structuredObservations)
        }
    }
}
