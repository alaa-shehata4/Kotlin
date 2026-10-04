package com.example.carebrief.presentation.careplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.components.StatusChip
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.core.ui.theme.SuccessText
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.EditableCarePlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Phase 28 — explicit care-plan state: Loading / Draft / Active / Error. */
sealed interface CarePlanScreenState {
    data object Loading : CarePlanScreenState
    data class Draft(val plan: EditableCarePlan) : CarePlanScreenState
    data class Active(val plan: EditableCarePlan) : CarePlanScreenState
    data class Error(val message: String = "We couldn't generate the analysis right now.") : CarePlanScreenState
}

class CarePlanViewModel(
    private val recipientId: String,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared,
    private val store: CarePlanStore = CarePlanStore.shared,
    private val taskStore: com.example.carebrief.data.TaskStore = com.example.carebrief.data.TaskStore.shared
) : ViewModel() {
    private val retryTick = MutableStateFlow(0)
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed

    val plan = combine(
        repo.observeNotes(recipientId),
        store.observe(recipientId),
        retryTick
    ) { notes, override, _ ->
        runCatching { override ?: store.defaultFor(recipientId, notes) }
            .getOrElse {
                _failed.value = true
                null
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Phase 28 canonical state; [plan]/[failed] kept as compat aliases. */
    val screenState: StateFlow<CarePlanScreenState> = combine(plan, _failed) { p, failed ->
        when {
            failed && p == null -> CarePlanScreenState.Error() as CarePlanScreenState
            p == null -> CarePlanScreenState.Loading as CarePlanScreenState
            p.status == "ACTIVE" -> CarePlanScreenState.Active(p)
            else -> CarePlanScreenState.Draft(p)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CarePlanScreenState.Loading)

    fun retry() {
        _failed.value = false
        retryTick.value += 1
    }

    val taskList = taskStore.observe(recipientId)
    val noteList = repo.observeNotes(recipientId)

    fun ensureTasks() {
        val draft = plan.value ?: return
        taskStore.ensureGenerated(
            recipientId,
            com.example.carebrief.core.model.CarePlanDraft(
                draft.goal, draft.reason, draft.actions, draft.monitoring
            )
        )
    }

    fun toggleTask(taskId: String) = taskStore.toggle(recipientId, taskId)

    class Factory(private val recipientId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CarePlanViewModel(recipientId) as T
    }
}

@Composable
fun CarePlanScreen(
    recipientId: String = "sarah",
    recipientName: String = "Sarah Johnson",
    onBack: (() -> Unit)? = null,
    onEdit: (String) -> Unit = {},
    onViewTasks: (String) -> Unit = {},
    vm: CarePlanViewModel = viewModel(factory = CarePlanViewModel.Factory(recipientId))
) {
    // Phase 28: single explicit state drives Loading / Draft / Active / Error.
    val screenState by vm.screenState.collectAsState()
    val tasks by vm.taskList.collectAsState()
    val notes by vm.noteList.collectAsState(initial = null)

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Care plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        when (val s = screenState) {
            CarePlanScreenState.Loading -> {
                Column(Modifier.padding(CareBriefSpacing.md)) { LoadingRow("Loading care plan") }
                return@Column
            }
            is CarePlanScreenState.Error -> {
                Column(Modifier.padding(CareBriefSpacing.md)) {
                    com.example.carebrief.core.ui.components.ErrorState(
                        s.message,
                        onRetry = vm::retry
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You can also continue reviewing notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
                return@Column
            }
            is CarePlanScreenState.Draft -> Unit // fall through to PlanContent below
            is CarePlanScreenState.Active -> Unit // fall through to PlanContent below
        }
        val draft = when (val s = screenState) {
            is CarePlanScreenState.Draft -> s.plan
            is CarePlanScreenState.Active -> s.plan
            else -> null
        } ?: run { return@Column }
        PlanContent(
            draft = draft,
            recipientId = recipientId,
            recipientName = recipientName,
            tasks = tasks,
            recentNotes = notes ?: emptyList(),
            onEdit = { onEdit(recipientId) },
            onToggleTask = vm::toggleTask,
            onEnsureTasks = vm::ensureTasks,
            onViewTasks = { onViewTasks(recipientId) }
        )
    }
}

@Composable
private fun PlanContent(
    draft: EditableCarePlan,
    recipientId: String,
    recipientName: String,
    tasks: List<com.example.carebrief.data.CareTask>,
    recentNotes: List<com.example.carebrief.core.model.DailyNote>,
    onEdit: () -> Unit,
    onToggleTask: (String) -> Unit,
    onEnsureTasks: () -> Unit,
    onViewTasks: () -> Unit
) {
    if (draft.status == "ACTIVE") {
        ActivePlanContent(
            draft = draft,
            recipientName = recipientName,
            tasks = tasks,
            recentNotes = recentNotes,
            onEdit = onEdit,
            onToggleTask = onToggleTask,
            onEnsureTasks = onEnsureTasks,
            onViewTasks = onViewTasks
        )
        return
    }
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).navigationBarsPadding().padding(CareBriefSpacing.md)
    ) {
        Text(
            "Suggested care plan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            softWrap = true
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$recipientName · based on recorded notes",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            softWrap = true
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (draft.status == "ACTIVE") StatusChip("ACTIVE CARE PLAN", ChipKind.ACTIVE)
            else DraftBadge()
            StatusChip("Priority: ${draft.priority}", ChipKind.NEUTRAL)
        }
        if (draft.status != "ACTIVE") {
            Spacer(Modifier.height(4.dp))
            Text(
                "DRAFT — HUMAN REVIEW REQUIRED",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary
            )
        }
        Spacer(Modifier.height(12.dp))

        EditableSection(title = "Goal", onEdit = onEdit) {
            Text(draft.goal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))

        EditableSection(title = "Why this goal?", onEdit = onEdit) {
            Text(draft.reason, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }
        Spacer(Modifier.height(12.dp))

        EditableSection(title = "Suggested actions", onEdit = onEdit) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.actions.forEach { action ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessText)
                        Text(action, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        EditableSection(title = "Monitoring", onEdit = onEdit) {
            // Phase 25: wraps on narrow phones / large fonts instead of clipping.
            MonitoringChips(draft.monitoring)
        }
        Spacer(Modifier.height(12.dp))

        EditableSection(title = "Review date", onEdit = onEdit) {
            Text(draft.reviewDateLabel, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))

        PrimaryButton("Edit care plan", onClick = onEdit, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        com.example.carebrief.core.ui.components.SecondaryButton(
            "View tasks",
            onClick = onViewTasks,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "AI-generated draft. Review before using. Not a diagnosis. Requires human approval.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Spacer(Modifier.height(8.dp))
        com.example.carebrief.core.network.OfflineBanner()
    }
}

@Composable
private fun EditableSection(
    title: String,
    onEdit: () -> Unit,
    content: @Composable () -> Unit
) {
    SectionHeader(title)
    CareBriefCard {
        Column {
            content()
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Text("Edit", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ActivePlanContent(
    draft: EditableCarePlan,
    recipientName: String,
    tasks: List<com.example.carebrief.data.CareTask>,
    recentNotes: List<com.example.carebrief.core.model.DailyNote>,
    onEdit: () -> Unit,
    onToggleTask: (String) -> Unit,
    onEnsureTasks: () -> Unit,
    onViewTasks: () -> Unit
) {
    val scroll = rememberScrollState()
    val done = tasks.count { it.completed }
    val today = LocalDate.now()
    val documentedDays = recentNotes.mapNotNull { note ->
        if (note.recordedAtMillis > 0L) {
            Instant.ofEpochMilli(note.recordedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            val daysAgo = when (val label = note.dayLabel.lowercase()) {
                "today" -> 0L
                "yesterday" -> 1L
                else -> Regex("(\\d+) days ago").matchEntire(label)?.groupValues?.get(1)?.toLongOrNull()
            }
            daysAgo?.let(today::minusDays)
        }
    }.toSet().count { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) }
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).navigationBarsPadding().padding(CareBriefSpacing.md)
    ) {
        Text(
            draft.goal.substringBefore("."),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text("$recipientName · reviewed and activated by caregiver", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("ACTIVE CARE PLAN", ChipKind.ACTIVE)
            StatusChip("Priority: ${draft.priority}", ChipKind.NEUTRAL)
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("Progress")
        CareBriefCard {
            Column {
                Text("$documentedDays of 7 days documented", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { documentedDays / 7f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (tasks.isEmpty()) {
                    Text("No tasks generated yet.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Generate tasks", onClick = onEnsureTasks, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("$done of ${tasks.size} tasks completed", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("Actions", actionLabel = "View all", onAction = onViewTasks)
        if (tasks.isEmpty()) {
            Text("Tasks will appear here once generated.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.take(4).forEach { task ->
                    com.example.carebrief.core.ui.components.TaskCard(
                        title = task.title,
                        category = task.category,
                        frequency = task.frequency,
                        dueLabel = task.dueLabel,
                        priority = task.priority,
                        completed = task.completed,
                        onToggle = { onToggleTask(task.id) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("Monitoring")
        CareBriefCard {
            MonitoringChips(draft.monitoring)
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("Review date")
        CareBriefCard {
            Text(draft.reviewDateLabel, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(12.dp))

        SectionHeader("History")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Draft created · ${planHistoryTime(draft.createdAtMillis)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (draft.status == "ACTIVE" && draft.approvedAtMillis > 0L)
                        "Approved · ${planHistoryTime(draft.approvedAtMillis)}"
                    else "Awaiting caregiver approval",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Last updated · ${planHistoryTime(draft.updatedAtMillis)}", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                recentNotes.take(3).forEach { note ->
                    Text(
                        "Note · ${note.dayLabel} ${note.timeLabel} — ${note.categories.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        com.example.carebrief.core.ui.components.SecondaryButton(
            "View all tasks",
            onClick = onViewTasks,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Edit care plan", onClick = onEdit, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        com.example.carebrief.core.network.OfflineBanner()
    }
}

private fun planHistoryTime(timestamp: Long): String = runCatching {
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}.getOrDefault("Unknown date")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonitoringChips(items: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { StatusChip(it, ChipKind.INFO) }
    }
}
