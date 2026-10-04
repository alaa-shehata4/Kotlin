package com.example.carebrief.presentation.recipients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.CareRecipientAvatar
import com.example.carebrief.core.ui.components.CareBriefTextField
import com.example.carebrief.core.ui.components.ChipKind
import com.example.carebrief.core.ui.components.EmptyState
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.components.StatusChip
import com.example.carebrief.core.ui.components.presentation
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class RecipientsViewModel(
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val retryTick = MutableStateFlow(0)
    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<List<CareRecipient>> =
        combine(query.debounce(150), retryTick) { q, _ -> q }
            .flatMapLatest { q ->
                repo.searchRecipients(q)
                    .catch {
                        _error.value = true
                        emit(emptyList())
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(q: String) { query.value = q }
    fun currentQuery(): StateFlow<String> = query
    fun retry() {
        _error.value = false
        retryTick.value += 1
    }
}

@Composable
fun RecipientsScreen(
    onOpenRecipient: (String) -> Unit,
    vm: RecipientsViewModel = viewModel()
) {
    val people by vm.uiState.collectAsState()
    val query by vm.currentQuery().collectAsState()
    val loadError by vm.error.collectAsState()
    var firstLoad by remember { mutableStateOf(true) }
    if (people.isNotEmpty()) firstLoad = false

    Column(Modifier.fillMaxSize().padding(CareBriefSpacing.md)) {
        Text("People", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Everyone you support, at a glance.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Spacer(Modifier.height(12.dp))
        CareBriefTextField(
            value = query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true,
            shape = MaterialTheme.shapes.small
        )
        Spacer(Modifier.height(12.dp))
        if (loadError && people.isEmpty()) {
            com.example.carebrief.core.ui.components.ErrorState(
                "Something went wrong while loading this information.",
                onRetry = vm::retry
            )
        } else if (firstLoad && people.isEmpty()) {
            LoadingRow("Loading people")
        } else if (people.isEmpty()) {
            EmptyState(
                title = "No care recipients yet.",
                description = if (query.isBlank()) "Add your first person to get started."
                else "No one matches \"$query\". Try another name.",
                cta = "Clear search",
                onCta = { vm.onQueryChange("") }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm)) {
                items(people, key = { it.id }) { person ->
                    CareBriefCard(modifier = Modifier.clickable(onClick = { onOpenRecipient(person.id) })) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CareRecipientAvatar(person.initials)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${person.age} years old · ${person.careStatus}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val status = person.planStatus.presentation()
                                    StatusChip(status.listLabel, status.kind)
                                    StatusChip("${person.pendingTasks} pending tasks", ChipKind.NEUTRAL)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Last note: ${person.lastNoteLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionHeader("Demo data", actionLabel = null)
        Text(
            "Fictional demo profiles so you can try the full flow immediately.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
    }
}
