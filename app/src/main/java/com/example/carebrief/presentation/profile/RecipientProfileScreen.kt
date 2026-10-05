package com.example.carebrief.presentation.profile

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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.carebrief.core.model.CarePlanDraft
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
import com.example.carebrief.core.ui.components.TaskCard
import com.example.carebrief.core.ui.components.presentation
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.EditableCarePlan
import com.example.carebrief.data.TaskStore
import com.example.carebrief.data.CareTask
import com.example.carebrief.data.ai.AiProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ProfileUiState(
    val recipient: CareRecipient? = null,
    val notes: List<DailyNote> = emptyList(),
    val carePlan: EditableCarePlan? = null,
    val hasSavedCarePlan: Boolean = false,
    val tasks: List<CareTask> = emptyList(),
    val activity: List<ProfileActivityItem> = emptyList()
)

data class ProfileActivityItem(val timestamp: Long, val title: String, val detail: String)

/** Phase 28 — explicit profile state: Loading / Content / Error. */
sealed interface ProfileScreenState {
    data object Loading : ProfileScreenState
    data class Content(val data: ProfileUiState) : ProfileScreenState
    data class Error(val message: String = "Something went wrong while loading this information.") : ProfileScreenState
}

class RecipientProfileViewModel(
    private val recipientId: String,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared
) : ViewModel() {
    private val plans = CarePlanStore.shared
    private val tasks = TaskStore.shared
    private val profile = combine(
        repo.observeRecipient(recipientId),
        repo.observeNotes(recipientId)
    ) { recipient, notes -> ProfileUiState(recipient, notes) }
    private val loadFailed = MutableStateFlow(false)
    private val retryTick = MutableStateFlow(0)
    val uiState: StateFlow<ProfileUiState> = combine(
        profile, plans.observe(recipientId), tasks.observe(recipientId), retryTick
    ) { state, savedPlan, currentTasks, _ ->
        val plan = savedPlan ?: plans.defaultFor(recipientId, state.notes)
        val activity = buildList {
            state.notes.forEach { note ->
                add(ProfileActivityItem(noteTimestamp(note), "Daily note added", "${note.author} · ${note.categories.joinToString()}"))
            }
            if (savedPlan != null) add(ProfileActivityItem(
                savedPlan.updatedAtMillis,
                "Care plan ${savedPlan.status.lowercase()}",
                savedPlan.goal
            ))
            currentTasks.forEach { task ->
                add(ProfileActivityItem(
                    task.updatedAtMillis,
                    if (task.completed) "Task completed" else "Task pending",
                    task.title
                ))
            }
        }.sortedByDescending(ProfileActivityItem::timestamp).take(8)
        state.copy(
            carePlan = plan,
            hasSavedCarePlan = savedPlan != null,
            tasks = currentTasks,
            activity = activity
        )
    }
        .catch {
            loadFailed.value = true
            emit(ProfileUiState())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /**
     * Phase 28 canonical state: Loading until the first emission with a
     * recipient; Content afterwards; Error when the repository flow fails.
     * [uiState] is kept as a compat alias for existing callers.
     */
    val screenState: StateFlow<ProfileScreenState> = combine(uiState, loadFailed) { state, failed ->
        when {
            failed && state.recipient == null -> ProfileScreenState.Error() as ProfileScreenState
            state.recipient == null -> ProfileScreenState.Loading as ProfileScreenState
            else -> ProfileScreenState.Content(state)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileScreenState.Loading)

    fun retry() {
        loadFailed.value = false
        retryTick.value += 1
    }

    fun ensureTasks(plan: EditableCarePlan?) {
        if (plan == null) return
        tasks.ensureGenerated(
            recipientId,
            CarePlanDraft(plan.goal, plan.reason, plan.actions, plan.monitoring)
        )
    }

    fun toggleTask(taskId: String) = tasks.toggle(recipientId, taskId)

    class Factory(private val recipientId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecipientProfileViewModel(recipientId) as T
    }
}

private fun noteTimestamp(note: DailyNote): Long {
    if (note.recordedAtMillis > 0L) return note.recordedAtMillis
    val label = note.dayLabel.lowercase()
    val daysAgo = when (label) {
        "today" -> 0L
        "yesterday" -> 1L
        else -> Regex("(\\d+) days ago").matchEntire(label)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }
    val time = runCatching { LocalTime.parse(note.timeLabel, DateTimeFormatter.ofPattern("HH:mm")) }
        .getOrDefault(LocalTime.NOON)
    return LocalDate.now().minusDays(daysAgo).atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun activityTime(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val date = when (dateTime.toLocalDate()) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> dateTime.format(DateTimeFormatter.ofPattern("d MMM"))
    }
    return "$date · ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipientProfileScreen(
    recipientId: String,
    onBack: () -> Unit,
    onAddNote: (String) -> Unit,
    onAnalyze: () -> Unit,
    onViewPlan: () -> Unit,
    onViewTasks: (String) -> Unit = {},
    vm: RecipientProfileViewModel = viewModel(factory = RecipientProfileViewModel.Factory(recipientId))
) {
    // Phase 28: explicit Loading / Content / Error drives the hero section.
    val screenState by vm.screenState.collectAsState()
    val fallback by vm.uiState.collectAsState()
    val state = when (val s = screenState) {
        is ProfileScreenState.Content -> s.data
        else -> fallback
    }
    val scroll = rememberScrollState()
    val ai = remember { AiProviders.current() }
    LaunchedEffect(state.carePlan) { vm.ensureTasks(state.carePlan) }

    Column(Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(scroll).padding(CareBriefSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))

        if (screenState is ProfileScreenState.Error && state.recipient == null) {
            com.example.carebrief.core.ui.components.ErrorState(
                (screenState as ProfileScreenState.Error).message,
                onRetry = vm::retry
            )
            return@Column
        }
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
                    // Phase 30: wraps on narrow phones / large fonts.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val status = person.planStatus.presentation()
                        StatusChip(status.profileLabel, status.kind)
                        StatusChip("${state.tasks.count { !it.completed }} pending tasks", ChipKind.NEUTRAL)
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
                    NoteCard(note.dayLabel, note.timeLabel, note.author, note.content, note.categories, note.structuredObservations)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Current concerns (AI, cautious)
        SectionHeader("Current concerns")
        DraftBadge()
        Spacer(Modifier.height(8.dp))
        val concerns = ai.analyzePotentialConcerns(state.notes)
        if (concerns.isEmpty()) {
            Text(
                if (state.notes.isEmpty()) "Not enough notes yet to suggest areas to review."
                else "No potential concerns were identified in the recent notes.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                concerns.forEach { concern ->
                    InsightCard(
                        title = "Potential area to review · ${concern.category}",
                        description = "Relevant observations appeared in ${concern.evidenceCount} of ${state.notes.size} recent notes. Review suggested.",
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
                val status = person.planStatus.presentation()
                Text(status.profileLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val plan = state.carePlan
                if (plan != null && person.planStatus != com.example.carebrief.core.model.PlanStatus.NONE) {
                    Spacer(Modifier.height(6.dp))
                    Text("Current goal", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                    Text(plan.goal, style = MaterialTheme.typography.bodyLarge)
                    Text(plan.reason, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                } else if (!state.hasSavedCarePlan) {
                    Spacer(Modifier.height(6.dp))
                    Text("No current goal yet.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                }
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
        SectionHeader("Tasks", actionLabel = "View all", onAction = { onViewTasks(person.id) })
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${state.tasks.count { !it.completed }} pending · ${state.tasks.count { it.completed }} completed", style = MaterialTheme.typography.titleMedium)
                state.tasks.forEach { task ->
                    TaskCard(
                        title = task.title,
                        category = task.category,
                        frequency = task.frequency,
                        dueLabel = task.dueLabel,
                        priority = task.priority,
                        completed = task.completed,
                        onToggle = { vm.toggleTask(task.id) }
                    )
                }
                SecondaryButton("Open tasks", onClick = { onViewTasks(person.id) }, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))

        // Activity
        SectionHeader("Activity · most recent first")
        if (state.activity.isEmpty()) {
            Text("No activity recorded yet.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.activity.forEach { item ->
                    CareBriefCard {
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.detail, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                            Text(activityTime(item.timestamp), style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
