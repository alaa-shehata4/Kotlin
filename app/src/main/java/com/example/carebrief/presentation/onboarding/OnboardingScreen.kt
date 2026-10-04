package com.example.carebrief.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carebrief.core.ui.components.PrimaryButton
import com.example.carebrief.core.ui.theme.CareBriefSpacing
import com.example.carebrief.core.ui.theme.InkSecondary
import kotlinx.coroutines.launch

private data class Page(val title: String, val body: String)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        Page(
            "Care documentation, made simpler.",
            "Capture daily observations and turn them into structured care-plan drafts."
        ),
        Page(
            "Turn notes into useful insights.",
            "CareBrief helps identify recurring observations, trends, and potential areas requiring attention."
        ),
        Page(
            "Keep humans in control.",
            "Every AI suggestion is a draft that must be reviewed by a caregiver. Not a diagnosis."
        )
    )
    val icons = listOf(Icons.Filled.EditNote, Icons.Filled.AutoAwesome, Icons.Filled.VerifiedUser)
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(CareBriefSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("Skip") }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Icon(
                        icons[i],
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(28.dp).padding(4.dp)
                    )
                }
                Text(
                    pages[i].title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    pages[i].body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "AI-generated content is a draft and should be reviewed by an appropriate human professional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { i ->
                val selected = pager.currentPage == i
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Spacer(Modifier.padding(horizontal = if (selected) 14.dp else 6.dp, vertical = 3.dp))
                }
            }
        }
        if (pager.currentPage < pages.lastIndex) {
            PrimaryButton(
                "Continue",
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            PrimaryButton("Get Started", onClick = onFinish, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
    }
}
