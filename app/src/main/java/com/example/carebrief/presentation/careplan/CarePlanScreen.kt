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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CarePlanViewModel(
    private val recipientId: String,
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared,
    private val store: CarePlanStore = CarePlanStore.shared
) : ViewModel() {
    val plan = combine(
        repo.observeNotes(recipientId),
        store.observe(recipientId)
    ) { notes, override ->
        override ?: store.defaultFor(recipientId, notes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
    vm: CarePlanViewModel = viewModel(factory = CarePlanViewModel.Factory(recipientId))
) {
    val plan by vm.plan.collectAsState()
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Care plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        val draft = plan
        if (draft == null) {
            Column(Modifier.padding(CareBriefSpacing.md)) { LoadingRow("Loading care plan") }
            return@Column
        }
        PlanContent(
            draft = draft,
            recipientName = recipientName,
            onEdit = { onEdit(recipientId) }
        )
    }
}

@Composable
private fun PlanContent(
    draft: EditableCarePlan,
    recipientName: String,
    onEdit: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(CareBriefSpacing.md)) {
        Text("Suggested care plan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("$recipientName · based on recorded notes", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.monitoring.forEach { StatusChip(it, ChipKind.INFO) }
            }
        }
        Spacer(Modifier.height(12.dp))

        EditableSection(title = "Review date", onEdit = onEdit) {
            Text(draft.reviewDateLabel, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))

        PrimaryButton("Edit care plan", onClick = onEdit, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "AI-generated draft. Review before using. Not a diagnosis. Requires human approval.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
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
