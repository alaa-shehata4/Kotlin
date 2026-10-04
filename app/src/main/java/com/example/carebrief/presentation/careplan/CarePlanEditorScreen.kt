package com.example.carebrief.presentation.careplan

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.DraftBadge
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SecondaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.EditableCarePlan
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val MONITORING_OPTIONS = listOf(
    "Nutrition", "Energy", "Mood", "Sleep",
    "Mobility", "Medication adherence", "Pain", "Behavior"
)
private val PRIORITIES = listOf("Low", "Normal", "High")
private val REVIEW_PRESETS = listOf(3, 7, 14, 30)

private fun defaultReviewDate() = LocalDate.now().plusDays(7)

private fun reviewDateLabel(date: LocalDate): String = try {
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
} catch (_: Exception) {
    date.toString()
}

private fun restoredReviewDate(plan: EditableCarePlan): LocalDate = runCatching {
    if (plan.reviewDateMillis > 0L) {
        java.time.Instant.ofEpochMilli(plan.reviewDateMillis)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    } else {
        LocalDate.parse(
            plan.reviewDateLabel,
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        )
    }
}.getOrDefault(defaultReviewDate())

class CarePlanEditorViewModel(
    private val recipientId: String,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared,
    private val store: CarePlanStore = CarePlanStore.shared
) : ViewModel() {
    val notes = repo.observeNotes(recipientId)
    val existing = store.observe(recipientId)

    fun save(plan: EditableCarePlan) = store.save(plan)

    suspend fun approve(plan: EditableCarePlan) {
        store.save(plan.copy(status = "ACTIVE"))
        store.approve(recipientId)
        repo.setPlanStatus(recipientId, PlanStatus.ACTIVE)
    }

    class Factory(private val recipientId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CarePlanEditorViewModel(recipientId) as T
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarePlanEditorScreen(
    recipientId: String,
    recipientName: String,
    onBack: () -> Unit,
    onApproved: () -> Unit,
    vm: CarePlanEditorViewModel = viewModel(factory = CarePlanEditorViewModel.Factory(recipientId))
) {
    val notes by vm.notes.collectAsState(initial = null)
    val existing by vm.existing.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scroll = rememberScrollState()

    var seeded by remember { mutableStateOf(false) }
    var goal by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val actions = remember { mutableStateListOf<String>() }
    val monitoring = remember { mutableStateListOf<String>() }
    var priority by remember { mutableStateOf("Normal") }
    var reviewDate by remember { mutableStateOf(defaultReviewDate()) }
    var newAction by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showApproveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(notes, existing) {
        val list = notes ?: return@LaunchedEffect
        if (!seeded) {
            val plan = existing ?: CarePlanStore.shared.defaultFor(recipientId, list)
            goal = plan.goal
            reason = plan.reason
            actions.clear(); actions.addAll(plan.actions)
            monitoring.clear(); monitoring.addAll(plan.monitoring)
            priority = plan.priority
            reviewDate = restoredReviewDate(plan)
            seeded = true
        }
    }

    fun currentPlan(status: String) = EditableCarePlan(
        recipientId = recipientId,
        goal = goal.trim(),
        reason = reason.trim(),
        actions = actions.map { it.trim() }.filter { it.isNotEmpty() },
        monitoring = monitoring.toList(),
        priority = priority,
        reviewDateLabel = reviewDateLabel(reviewDate),
        status = status,
        reviewDateMillis = reviewDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )

    // Phase 25: keyboard + system-bar aware so fields/CTAs stay reachable.
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text("Edit care plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(recipientName, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
        }
        if (!seeded) {
            Column(Modifier.padding(CareBriefSpacing.md)) { LoadingRow("Loading draft") }
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(CareBriefSpacing.md),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DraftBadge()
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            SectionHeader("Goal")
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Support consistent nutrition monitoring.") },
                shape = MaterialTheme.shapes.small
            )

            SectionHeader("Why this goal?")
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it; error = null },
                // Phase 25: heightIn supports large fonts / long text.
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                placeholder = { Text("Based on repeated mentions of...") },
                shape = MaterialTheme.shapes.small
            )

            SectionHeader("Suggested actions")
            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEachIndexed { index, action ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = action,
                                onValueChange = { actions[index] = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small
                            )
                            IconButton(
                                onClick = {
                                    val moved = PlanEditOps.move(actions.toList(), index, index - 1)
                                    actions.clear(); actions.addAll(moved)
                                },
                                enabled = index > 0
                            ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up") }
                            IconButton(
                                onClick = {
                                    val moved = PlanEditOps.move(actions.toList(), index, index + 1)
                                    actions.clear(); actions.addAll(moved)
                                },
                                enabled = index < actions.lastIndex
                            ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down") }
                            IconButton(onClick = { actions.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete action")
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newAction,
                            onValueChange = { newAction = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add an action...") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            if (newAction.trim().isNotEmpty()) {
                                actions.add(newAction.trim()); newAction = ""
                            }
                        }) { Icon(Icons.Filled.Add, contentDescription = "Add action") }
                    }
                }
            }

            SectionHeader("Monitoring")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MONITORING_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = monitoring.contains(option),
                        onClick = { if (monitoring.contains(option)) monitoring.remove(option) else monitoring.add(option) },
                        label = { Text(option) }
                    )
                }
            }

            SectionHeader("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRIORITIES.forEach { option ->
                    FilterChip(selected = priority == option, onClick = { priority = option }, label = { Text(option) })
                }
            }

            SectionHeader("Review date")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REVIEW_PRESETS.forEach { days ->
                    FilterChip(
                        selected = reviewDate == LocalDate.now().plusDays(days.toLong()),
                        onClick = { reviewDate = LocalDate.now().plusDays(days.toLong()) },
                        label = { Text("In $days days") }
                    )
                }
            }
            TextButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> reviewDate = LocalDate.of(year, month + 1, day) },
                    reviewDate.year,
                    reviewDate.monthValue - 1,
                    reviewDate.dayOfMonth
                ).apply {
                    datePicker.minDate = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                }.show()
            }) { Text("Choose exact date") }
            Text("Selected: ${reviewDateLabel(reviewDate)}", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)

            PrimaryButton(
                "Save draft",
                onClick = {
                    val problem = validatePlan(goal, actions.toList(), monitoring.toList())
                    if (problem != null) { error = problem; return@PrimaryButton }
                    vm.save(currentPlan("DRAFT"))
                    scope.launch { snackbar.showSnackbar("Draft saved") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton(
                "Approve care plan",
                onClick = {
                    val problem = validatePlan(goal, actions.toList(), monitoring.toList())
                    if (problem != null) { error = problem; return@SecondaryButton }
                    showApproveDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        SnackbarHost(snackbar, Modifier.padding(16.dp))
    }

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Activate this care plan?") },
            text = {
                Text("You're about to activate this care plan. Please confirm that the information has been reviewed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showApproveDialog = false
                    scope.launch {
                        vm.approve(currentPlan("DRAFT"))
                        snackbar.showSnackbar("Care plan activated")
                        onApproved()
                    }
                }) { Text("Approve") }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) { Text("Keep editing") }
            }
        )
    }
}
