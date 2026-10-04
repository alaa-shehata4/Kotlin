package com.example.carebrief.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carebrief.core.ui.theme.AmberBorder
import com.example.carebrief.core.ui.theme.AmberContainer
import com.example.carebrief.core.ui.theme.AmberText
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.CriticalBorder
import com.example.carebrief.core.ui.theme.CriticalContainer
import com.example.carebrief.core.ui.theme.CriticalText
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.core.ui.theme.MintContainer
import com.example.carebrief.core.ui.theme.SageContainer
import com.example.carebrief.core.ui.theme.SuccessBorder
import com.example.carebrief.core.ui.theme.SuccessContainer
import com.example.carebrief.core.ui.theme.SuccessText
import com.example.carebrief.core.ui.theme.TealPrimary
import com.example.carebrief.core.model.StructuredObservations

@Composable
fun CareBriefCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(Modifier.padding(CareBriefSpacing.md)) { content() }
    }
}

@Composable
fun CareBriefTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.small
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        singleLine = singleLine,
        shape = shape
    )
}

@Composable
fun CareBriefDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = content,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(dismissLabel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareBriefBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareBriefTopAppBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon,
        actions = actions
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (actionLabel != null && onAction != null) {
            androidx.compose.material3.TextButton(onClick = onAction) {
                Text(actionLabel, color = TealPrimary)
            }
        }
    }
}

@Composable
fun StatusChip(text: String, kind: ChipKind = ChipKind.NEUTRAL) {
    val (bg, fg) = when (kind) {
        ChipKind.NEUTRAL -> SageContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ChipKind.DRAFT -> AmberContainer to AmberText
        ChipKind.ACTIVE -> SuccessContainer to SuccessText
        ChipKind.CONCERN -> AmberContainer to AmberText
        ChipKind.CRITICAL -> CriticalContainer to CriticalText
        ChipKind.INFO -> MintContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.extraSmall,
        tonalElevation = 0.dp
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

enum class ChipKind { NEUTRAL, DRAFT, ACTIVE, CONCERN, CRITICAL, INFO }

@Composable
fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(CareBriefSpacing.md)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = TealPrimary)
    }
}

@Composable
fun CareRecipientAvatar(initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MintContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TealPrimary)
    }
}

@Composable
fun InsightCard(title: String, description: String, icon: ImageVector = Icons.Filled.Info) {
    CareBriefCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = AmberText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
        }
    }
}

@Composable
fun NoteCard(
    dayLabel: String,
    timeLabel: String,
    author: String,
    content: String,
    categories: List<String>,
    structuredObservations: StructuredObservations? = null
) {
    CareBriefCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip("$dayLabel · $timeLabel", ChipKind.INFO)
                Spacer(Modifier.width(8.dp))
                Text(author, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Text(content, style = MaterialTheme.typography.bodyLarge)
            val structured = listOfNotNull(
                structuredObservations?.mood?.let { "Mood: $it" },
                structuredObservations?.mobility?.let { "Mobility: $it" },
                structuredObservations?.appetite?.let { "Appetite: $it" },
                structuredObservations?.sleep?.let { "Sleep: $it" }
            )
            if (structured.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(structured.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { StatusChip(it, ChipKind.NEUTRAL) }
                }
            }
        }
    }
}

@Composable
fun DraftBadge() {
    Surface(color = AmberContainer, shape = MaterialTheme.shapes.extraSmall) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "DRAFT — REVIEW REQUIRED",
                style = MaterialTheme.typography.labelMedium,
                color = AmberText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, cta: String, onCta: () -> Unit) {
    CareBriefCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            Spacer(Modifier.height(12.dp))
            SecondaryButton(cta, onCta)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    CareBriefCard {
        Column {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Retry", onRetry)
        }
    }
}

@Composable
fun LoadingRow(label: String = "Loading") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = TealPrimary)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
    }
}

@Composable
fun AIProcessingIndicator(label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = TealPrimary)
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = InkSecondary
        )
    }
}

@Composable
fun OfflineIndicator(
    text: String = "Offline-ready · demo data stored on device",
    modifier: Modifier = Modifier
) {
    Surface(color = SageContainer, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = SuccessText)
        }
    }
}

@Composable
fun EvidenceCard(    observation: String,
    evidence: String,
    frequency: String,
    supportingQuotes: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    CareBriefCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = AmberText, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Potential concern", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text(observation, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Evidence: $evidence", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text(frequency, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                }
            }
            if (supportingQuotes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide supporting notes" else "Show supporting notes (${supportingQuotes.size})")
                }
                if (expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        supportingQuotes.forEach { quote ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    "“$quote”",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    title: String,
    category: String,
    frequency: String,
    dueLabel: String,
    priority: String,
    completed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    CareBriefCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(
                checked = completed,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = if (completed) {
                        MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    } else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (completed) FontWeight.Normal else FontWeight.Medium,
                    color = if (completed) InkSecondary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(category, ChipKind.INFO)
                    StatusChip(frequency, ChipKind.NEUTRAL)
                    if (priority == "High") StatusChip("High priority", ChipKind.CONCERN)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (completed) "Completed · tap to undo" else "Due: $dueLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
            }
        }
    }
}
