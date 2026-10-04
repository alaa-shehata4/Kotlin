package com.example.carebrief.presentation.analysis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.DraftBadge
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.core.ui.theme.SuccessText
import kotlinx.coroutines.delay

@Composable
fun AnalysisScreen(
    recipientName: String,
    noteCount: Int,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var currentStage by remember { mutableIntStateOf(0) }
    var done by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        AnalysisStages.stages.forEachIndexed { index, stage ->
            currentStage = index
            delay(stage.durationMs)
            done = index + 1
        }
        onComplete()
    }

    val progress by animateFloatAsState(
        targetValue = done.toFloat() / AnalysisStages.stages.size.toFloat(),
        label = "analysis-progress"
    )

    Column(
        Modifier.fillMaxSize().padding(CareBriefSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Analyzing notes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "$recipientName · $noteCount recent notes",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Spacer(Modifier.height(8.dp))
        DraftBadge()
        Spacer(Modifier.height(20.dp))

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(72.dp),
            strokeWidth = 6.dp
        )
        Spacer(Modifier.height(20.dp))

        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalysisStages.stages.forEachIndexed { index, stage ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            index < done -> Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Done",
                                tint = SuccessText,
                                modifier = Modifier.size(22.dp)
                            )
                            index == currentStage -> CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            else -> Spacer(Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stage.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (index == currentStage) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (index <= currentStage) MaterialTheme.colorScheme.onSurface else InkSecondary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Working offline on this device. This usually takes a few seconds.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel) { Text("Continue reviewing notes") }
    }
}
