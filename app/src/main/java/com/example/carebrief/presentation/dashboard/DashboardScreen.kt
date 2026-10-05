package com.example.carebrief.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.InsightCard
import com.example.carebrief.core.ui.components.MetricCard
import com.example.carebrief.core.network.OfflineBanner
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SecondaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.core.model.ActivityItem
import com.example.carebrief.core.model.AttentionItem
import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.core.ui.state.GENERIC_LOAD_ERROR
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.TaskStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DashboardUiState(
    val recipients: List<CareRecipient> = emptyList(),
    val notesToday: Int = 0,
    val pendingTasks: Int = 0,
    val completedTasks: Int = 0,
    val reviewStatus: String = "Care plans are up to date",
    val attention: List<AttentionItem> = emptyList(),
    val recentActivity: List<ActivityItem> = emptyList()
)

/** Phase 28 — explicit dashboard state: Loading / Success / Empty / Error. */
sealed interface DashboardScreenState {
    data object Loading : DashboardScreenState
    data class Content(val data: DashboardUiState) : DashboardScreenState
    data class Empty(val data: DashboardUiState = DashboardUiState()) : DashboardScreenState
    data class Error(val message: String = GENERIC_LOAD_ERROR) : DashboardScreenState
}

class DashboardViewModel(
    repo: CareBriefRepository = DemoCareBriefRepository.shared,
    taskStore: TaskStore = TaskStore.shared
) : ViewModel() {
    private val retryTick = MutableStateFlow(0)
    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError

    private val taskCounts = repo.observeRecipients().flatMapLatest { recipients ->
        val taskFlows = recipients.map { taskStore.observe(it.id) }
        val tasksByRecipient = if (taskFlows.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(taskFlows) { taskLists -> taskLists.toList() }
        }
        tasksByRecipient.map { taskLists ->
            val pending = recipients.zip(taskLists).sumOf { (recipient, tasks) ->
                if (tasks.isEmpty()) recipient.pendingTasks else tasks.count { !it.completed }
            }
            val completed = taskLists.sumOf { tasks -> tasks.count { it.completed } }
            pending to completed
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.observeRecipients(),
        repo.observeAllNotes(),
        taskCounts,
        retryTick
    ) { recipients, notes, taskCount, _ ->
        buildDashboardState(recipients, notes).copy(
            pendingTasks = taskCount.first,
            completedTasks = taskCount.second
        )
    }
        .catch {
            _loadError.value = true
            emit(DashboardUiState())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /**
     * Phase 28 canonical state. `null` initial data maps to [DashboardScreenState.Loading];
     * repository errors map to [DashboardScreenState.Error]; zero recipients map to
     * [DashboardScreenState.Empty]; otherwise [DashboardScreenState.Content].
     * `uiState`/`loadError` above are kept as compat aliases for existing tests.
     */
    val screenState: StateFlow<DashboardScreenState> = combine(
        uiState,
        _loadError
    ) { data, failed ->
        when {
            failed -> DashboardScreenState.Error()
            data.recipients.isEmpty() && data.recentActivity.isEmpty() -> DashboardScreenState.Empty(data)
            else -> DashboardScreenState.Content(data)
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        DashboardScreenState.Loading
    )

    fun retry() {
        _loadError.value = false
        retryTick.value += 1
    }
}

private fun buildDashboardState(
    recipients: List<CareRecipient>,
    notes: List<DailyNote>
): DashboardUiState {
    val appetiteNotes = notes.filter { note ->
        val content = note.content.lowercase()
        content.contains("reduced") || content.contains("ate very little") || content.contains("untouched")
    }
    val fatigueNotes = notes.filter { note ->
        val content = note.content.lowercase()
        content.contains("fatigue") || content.contains("tired")
    }
    val attention = buildList {
        if (appetiteNotes.isNotEmpty()) add(
            AttentionItem(
                "Potential concern · Reduced appetite",
                "Reduced intake mentioned in ${appetiteNotes.size} recent notes. Review suggested."
            )
        )
        if (fatigueNotes.isNotEmpty()) add(
            AttentionItem(
                "Watch · Fatigue pattern",
                "Tiredness or fatigue noted in ${fatigueNotes.size} recent notes. Keep observing energy and sleep."
            )
        )
    }
    val byId = recipients.associateBy { it.id }
    val activity = notes.take(3).mapNotNull { note ->
        val recipient = byId[note.recipientId] ?: return@mapNotNull null
        ActivityItem(
            "${recipient.name} — Daily note added",
            "${note.dayLabel} · ${note.timeLabel} · ${note.categories.joinToString()}"
        )
    }
    val drafts = recipients.count { it.planStatus == PlanStatus.DRAFT }
    val reviewStatus = if (drafts == 0) "Care plans are up to date" else
        "$drafts care plan${if (drafts == 1) "" else "s"} need review"
    return DashboardUiState(
        recipients = recipients,
        notesToday = notes.count { it.dayLabel.equals("Today", ignoreCase = true) },
        pendingTasks = recipients.sumOf { it.pendingTasks },
        reviewStatus = reviewStatus,
        attention = attention,
        recentActivity = activity
    )
}

@Composable
fun DashboardScreen(
    onAddNote: () -> Unit,
    onAnalyze: () -> Unit,
    onViewPlan: () -> Unit,
    onSelectRecipient: () -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    // Phase 28: single explicit state drives Loading / Empty / Error / Success.
    val screenState by vm.screenState.collectAsState()
    val greeting = greeting()
    val dateLine = dateLine()
    val scroll = rememberScrollState()

    when (val s = screenState) {
        DashboardScreenState.Loading -> {
            Column(Modifier.fillMaxSize().padding(CareBriefSpacing.md).navigationBarsPadding()) {
                Text(greeting, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold, softWrap = true)
                Spacer(Modifier.height(12.dp))
                com.example.carebrief.core.ui.components.LoadingRow("Loading today's overview")
            }
            return
        }
        is DashboardScreenState.Error -> {
            Column(
                Modifier.fillMaxSize().padding(CareBriefSpacing.md).navigationBarsPadding()
            ) {
                Text(greeting, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold, softWrap = true)
                Spacer(Modifier.height(12.dp))
                com.example.carebrief.core.ui.components.ErrorState(
                    s.message,
                    onRetry = vm::retry
                )
            }
            return
        }
        is DashboardScreenState.Empty -> Unit // fall through: show zero-state dashboard below
        is DashboardScreenState.Content -> Unit // fall through
    }
    val dashboard = when (val s = screenState) {
        is DashboardScreenState.Content -> s.data
        is DashboardScreenState.Empty -> s.data
        else -> DashboardUiState()
    }
    // Phase 25: centered max-width so large phones/emulators don't stretch;
    // metrics collapse on very narrow phones; insets respect system bars.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = maxWidth
        val narrow = maxW < 360.dp
        Column(
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                .fillMaxSize()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(CareBriefSpacing.md)
                .widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(CareBriefSpacing.md)
        ) {
        Text(greeting, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold, softWrap = true)
        Text("Here's today's care overview.", style = MaterialTheme.typography.bodyLarge, color = InkSecondary, softWrap = true)
        Text(dateLine, style = MaterialTheme.typography.labelMedium, color = InkSecondary, softWrap = true)

        if (narrow) {
            Column(verticalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm)) {
                MetricCard(dashboard.recipients.size.toString(), "Active people", Modifier.fillMaxWidth())
                MetricCard(dashboard.notesToday.toString(), "Notes today", Modifier.fillMaxWidth())
                MetricCard(dashboard.pendingTasks.toString(), "Pending tasks", Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                MetricCard(dashboard.recipients.size.toString(), "Active people", Modifier.weight(1f))
                MetricCard(dashboard.notesToday.toString(), "Notes today", Modifier.weight(1f))
                MetricCard(dashboard.pendingTasks.toString(), "Pending tasks", Modifier.weight(1f))
            }
        }
        Text(dashboard.reviewStatus, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Text(
            "${dashboard.completedTasks} tasks completed",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )

        SectionHeader("Today's attention")
        dashboard.attention.forEach {
            InsightCard(title = it.title, description = it.description, icon = Icons.Filled.Warning)
        }
        if (dashboard.attention.isEmpty()) {
            Text("No potential areas to review from recorded notes.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }

        SectionHeader("Recent activity")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dashboard.recentActivity.forEach {
                    Column {
                        Text(it.title, style = MaterialTheme.typography.titleMedium)
                        Text(it.subtitle, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    }
                }
                if (dashboard.recentActivity.isEmpty()) {
                    Text("No recent activity.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                }
            }
        }

        SectionHeader("Quick actions")
        PrimaryButton("Add note", onAddNote, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton("Analyze notes", onAnalyze, Modifier.weight(1f))
            SecondaryButton("View care plan", onViewPlan, Modifier.weight(1f))
        }
        SecondaryButton("View Sarah Johnson", onSelectRecipient, Modifier.fillMaxWidth())

        OfflineBanner()
        Spacer(Modifier.height(4.dp))
        }
    }
}

private fun greeting(): String {
    val hour = try { java.time.LocalTime.now().hour } catch (_: Exception) { 10 }
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun dateLine(): String {
    return try {
        val d = LocalDate.now()
        "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}"
    } catch (_: Exception) {
        "Today's overview"
    }
}
