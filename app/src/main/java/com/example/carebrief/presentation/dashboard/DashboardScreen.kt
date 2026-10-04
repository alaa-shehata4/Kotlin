package com.example.carebrief.presentation.dashboard

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carebrief.core.ui.components.CareBriefCard
import com.example.carebrief.core.ui.components.InsightCard
import com.example.carebrief.core.ui.components.MetricCard
import com.example.carebrief.core.ui.components.OfflineIndicator
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.components.SecondaryButton
import com.example.carebrief.core.ui.components.SectionHeader
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import com.example.carebrief.data.local.DemoData
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onAddNote: () -> Unit,
    onAnalyze: () -> Unit,
    onViewPlan: () -> Unit,
    onSelectRecipient: () -> Unit
) {
    val greeting = greeting()
    val dateLine = dateLine()
    val scroll = rememberScrollState()

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(CareBriefSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CareBriefSpacing.md)
    ) {
        Text(greeting, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold)
        Text("Here's today's care overview.", style = MaterialTheme.typography.bodyLarge, color = InkSecondary)
        Text(dateLine, style = MaterialTheme.typography.labelMedium, color = InkSecondary)

        Row(horizontalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            MetricCard("3", "Active people", Modifier.weight(1f))
            MetricCard("5", "Notes today", Modifier.weight(1f))
            MetricCard("4", "Pending tasks", Modifier.weight(1f))
        }

        SectionHeader("Today's attention")
        DemoData.attention.forEach {
            InsightCard(title = it.title, description = it.description, icon = Icons.Filled.Warning)
        }

        SectionHeader("Recent activity")
        CareBriefCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DemoData.recentActivity.forEach {
                    Column {
                        Text(it.title, style = MaterialTheme.typography.titleMedium)
                        Text(it.subtitle, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    }
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

        OfflineIndicator()
        Spacer(Modifier.height(4.dp))
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
