package com.example.carebrief.presentation.tasks

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carebrief.core.model.CarePlanDraft
import com.example.carebrief.core.ui.components.EmptyState
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.components.TaskCard
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.TaskStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TaskFilter { TODAY, UPCOMING, COMPLETED }
private val TASK_CATEGORIES = listOf(
    "Nutrition", "Energy", "Sleep", "Mood", "Mobility", "Medication",
    "Medication adherence", "Pain", "Behavior", "Review", "General"
)
private val TASK_PRIORITIES = listOf("Low", "Normal", "High")

/** Phase 28 — explicit tasks state: Loading / Content / Empty / Error. */
sealed interface TasksScreenState {
    data object Loading : TasksScreenState
    data class Content(val tasks: List<com.example.carebrief.data.CareTask>) : TasksScreenState
    data object Empty : TasksScreenState
    data class Error(val message: String = "Something went wrong while loading this information.") : TasksScreenState
}

class TasksViewModel(
    private val recipientId: String,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared,
    private val plans: CarePlanStore = CarePlanStore.shared,
    private val tasks: TaskStore = TaskStore.shared
) : ViewModel() {
    val taskList = tasks.observe(recipientId)

    val draft = combine(
        repo.observeNotes(recipientId),
        plans.observe(recipientId)
    ) { notes, override ->
        runCatching {
            val current = override ?: plans.defaultFor(recipientId, notes)
            CarePlanDraft(current.goal, current.reason, current.actions, current.monitoring)
        }.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Phase 28 canonical state; [taskList]/[draft] kept as compat aliases. */
    val screenState: StateFlow<TasksScreenState> = combine(taskList, draft) { all, d ->
        when {
            d == null -> TasksScreenState.Loading as TasksScreenState
            all.isEmpty() -> TasksScreenState.Empty as TasksScreenState
            else -> TasksScreenState.Content(all)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksScreenState.Loading)

    fun ensureTasks() {
        val d = draft.value ?: return
        tasks.ensureGenerated(recipientId, d)
    }

    fun toggle(taskId: String) = tasks.toggle(recipientId, taskId)

    fun update(task: com.example.carebrief.data.CareTask) = tasks.update(task)

    class Factory(private val recipientId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TasksViewModel(recipientId) as T
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    recipientId: String,
    recipientName: String,
    onBack: () -> Unit,
    vm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(recipientId))
) {
    // Phase 28: explicit state drives Loading / Empty / Content.
    val screenState by vm.screenState.collectAsState()
    val taskList by vm.taskList.collectAsState()
    val all = when (val s = screenState) {
        is TasksScreenState.Content -> s.tasks
        else -> taskList
    }
    val draft by vm.draft.collectAsState()
    var filter by remember { mutableStateOf(TaskFilter.TODAY) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(draft) {
        if (draft != null) vm.ensureTasks()
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding().padding(CareBriefSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Today's tasks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(recipientName, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Phase 30: horizontally scrollable so small phones / large fonts never clip.
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = filter == TaskFilter.TODAY, onClick = { filter = TaskFilter.TODAY }, label = { Text("Today") }) }
            item { FilterChip(selected = filter == TaskFilter.UPCOMING, onClick = { filter = TaskFilter.UPCOMING }, label = { Text("Upcoming") }) }
            item { FilterChip(selected = filter == TaskFilter.COMPLETED, onClick = { filter = TaskFilter.COMPLETED }, label = { Text("Completed") }) }
        }
        Spacer(Modifier.height(12.dp))
        if (draft == null) {
            LoadingRow("Loading tasks")
            return@Column
        }
        val visible = when (filter) {
            TaskFilter.TODAY -> all.filter { !it.completed && !taskDate(it).isAfter(LocalDate.now()) }
            TaskFilter.UPCOMING -> all.filter { !it.completed && taskDate(it).isAfter(LocalDate.now()) }
            TaskFilter.COMPLETED -> all.filter { it.completed }
        }
        if (all.isEmpty()) {
            EmptyState(
                "No tasks yet.",
                "Generate practical tasks from the current care-plan draft.",
                "Generate tasks"
            ) { vm.ensureTasks() }
            return@Column
        }
        if (visible.isEmpty()) {
            val (title, cta, target) = when (filter) {
                TaskFilter.TODAY -> Triple("Nothing due today.", "View completed", TaskFilter.COMPLETED)
                TaskFilter.UPCOMING -> Triple("Nothing upcoming.", "View today's tasks", TaskFilter.TODAY)
                TaskFilter.COMPLETED -> Triple("No completed tasks yet.", "View today's tasks", TaskFilter.TODAY)
            }
            EmptyState(title, "Tasks generated from the care plan appear here.", cta) {
                filter = target
            }
            return@Column
        }
        SectionHeader(
            when (filter) {
                TaskFilter.TODAY -> "Due today (${visible.size})"
                TaskFilter.UPCOMING -> "Upcoming (${visible.size})"
                TaskFilter.COMPLETED -> "Completed (${visible.size})"
            }
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(visible, key = { it.id }) { task ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TaskCard(
                        title = task.title,
                        category = task.category,
                        frequency = task.frequency,
                        dueLabel = task.dueLabel,
                        priority = task.priority,
                        completed = task.completed,
                        onToggle = {
                            val wasCompleted = task.completed
                            vm.toggle(task.id)
                            scope.launch {
                                snackbar.showSnackbar(if (wasCompleted) "Task reopened" else "Task completed")
                            }
                        }
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            val date = taskDate(task)
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = LocalDate.of(year, month + 1, day)
                                    val millis = selected.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    vm.update(task.copy(dueDateMillis = millis, dueLabel = taskDateLabel(selected), updatedAtMillis = System.currentTimeMillis()))
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth
                            ).show()
                        }) { Text("Due: ${task.dueLabel} · change") }
                        TASK_PRIORITIES.forEach { priority ->
                            FilterChip(
                                selected = task.priority == priority,
                                onClick = { vm.update(task.copy(priority = priority, updatedAtMillis = System.currentTimeMillis())) },
                                label = { Text(priority) }
                            )
                        }
                        TASK_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = task.category == category,
                                onClick = { vm.update(task.copy(category = category, updatedAtMillis = System.currentTimeMillis())) },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.padding(top = 8.dp))
    }
}

private fun taskDate(task: com.example.carebrief.data.CareTask): LocalDate =
    if (task.dueDateMillis > 0L) {
        Instant.ofEpochMilli(task.dueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    } else when (task.dueLabel) {
        "Tomorrow" -> LocalDate.now().plusDays(1)
        "This week" -> LocalDate.now().plusDays(7)
        else -> LocalDate.now()
    }

private fun taskDateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
}
