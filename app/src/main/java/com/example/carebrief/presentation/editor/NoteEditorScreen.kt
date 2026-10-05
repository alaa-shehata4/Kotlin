package com.example.carebrief.presentation.editor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SecondaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.NoteValidator
import com.example.carebrief.core.model.StructuredObservations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val NOTE_CATEGORIES = listOf(
    "Mood", "Mobility", "Nutrition", "Sleep",
    "Medication adherence", "Behavior", "Pain", "Other"
)

/** Phase 28 — explicit editor state: Idle / Saving / Saved / Error. */
sealed interface NoteEditorScreenState {
    data object Idle : NoteEditorScreenState
    data object Saving : NoteEditorScreenState
    data object Saved : NoteEditorScreenState
    data class Error(val message: String) : NoteEditorScreenState
}

class NoteEditorViewModel(
    private val repo: CareBriefRepository = DemoCareBriefRepository.shared
) : ViewModel() {
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Phase 28 canonical state; [saved]/[error] kept as compat aliases. */
    private val _screenState = MutableStateFlow<NoteEditorScreenState>(NoteEditorScreenState.Idle)
    val screenState: StateFlow<NoteEditorScreenState> = _screenState

    fun save(
        recipientId: String,
        content: String,
        categories: List<String>,
        author: String,
        structuredObservations: StructuredObservations?
    ) {
        when (val v = NoteValidator.validate(content, categories)) {
            is NoteValidator.Result.Invalid -> {
                _error.value = v.message
                _screenState.value = NoteEditorScreenState.Error(v.message)
                return
            }
            NoteValidator.Result.Valid -> Unit
        }
        _screenState.value = NoteEditorScreenState.Saving
        viewModelScope.launch {
            try {
                repo.addNote(recipientId, content, categories, author, structuredObservations)
                _saved.value = true
                _screenState.value = NoteEditorScreenState.Saved
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
                _screenState.value = NoteEditorScreenState.Error(e.message ?: "Couldn't save the note.")
            } catch (e: Exception) {
                _error.value = "Couldn't save the note. Please try again."
                _screenState.value = NoteEditorScreenState.Error("Couldn't save the note. Please try again.")
            }
        }
    }

    fun clearError() {
        _error.value = null
        if (_screenState.value is NoteEditorScreenState.Error) {
            _screenState.value = NoteEditorScreenState.Idle
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    recipientId: String,
    recipientName: String,
    onBack: () -> Unit,
    onSavedAnalyze: () -> Unit,
    vm: NoteEditorViewModel = viewModel()
) {
    var observation by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }
    var mood by remember { mutableStateOf<String?>(null) }
    var mobility by remember { mutableStateOf<String?>(null) }
    var appetite by remember { mutableStateOf<String?>(null) }
    var sleep by remember { mutableStateOf<String?>(null) }
    val saved by vm.saved.collectAsState()
    val error by vm.error.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scroll = rememberScrollState()
    val nowLabel = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM · HH:mm"))
    }

    LaunchedEffect(saved) {
        if (saved) snackbar.showSnackbar("Note saved")
    }

    // Phase 25: imePadding keeps the CTA visible with the keyboard up;
    // heightIn (not fixed height) supports large system fonts and long text.
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text("New daily note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, softWrap = true)
                Text("$recipientName · $nowLabel", style = MaterialTheme.typography.bodyMedium, color = InkSecondary, softWrap = true)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(CareBriefSpacing.md),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Observation")
            OutlinedTextField(
                value = observation,
                onValueChange = { observation = it; vm.clearError() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                placeholder = { Text("Describe what you observed today...") },
                shape = MaterialTheme.shapes.small,
                isError = error != null
            )
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            SectionHeader("Categories (optional)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NOTE_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = selected.contains(cat),
                        onClick = { if (selected.contains(cat)) selected.remove(cat) else selected.add(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            SectionHeader("Structured observations (optional)")
            CareBriefCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StructuredRow("Mood", listOf("Good", "Neutral", "Low", "Agitated"), mood) { mood = it }
                    StructuredRow("Mobility", listOf("Independent", "Assisted", "Limited"), mobility) { mobility = it }
                    StructuredRow("Appetite", listOf("Good", "Reduced", "Poor"), appetite) { appetite = it }
                    StructuredRow("Sleep", listOf("Good", "Interrupted", "Poor"), sleep) { sleep = it }
                }
            }

            PrimaryButton(
                if (saved) "Note saved" else "Save note",
                onClick = {
                    val cats = buildList {
                        addAll(selected)
                        if (appetite != null && !contains("Nutrition")) add("Nutrition")
                        if (sleep != null && !contains("Sleep")) add("Sleep")
                        if (mood != null && !contains("Mood")) add("Mood")
                        if (mobility != null && !contains("Mobility")) add("Mobility")
                    }
                    val structured = StructuredObservations(mood, mobility, appetite, sleep)
                        .takeIf { mood != null || mobility != null || appetite != null || sleep != null }
                    vm.save(recipientId, observation, cats, "Caregiver", structured)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saved
            )
            if (saved) {
                SecondaryButton("Analyze recent notes", onSavedAnalyze, Modifier.fillMaxWidth())
            }
            Text(
                "Saved notes stay on this device and power the AI draft. AI output is a draft requiring review — not a diagnosis.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        }
        SnackbarHost(snackbar, Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StructuredRow(label: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(selected = selected == opt, onClick = { onSelect(opt) }, label = { Text(opt) })
            }
        }
    }
}
