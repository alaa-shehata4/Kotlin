package com.example.carebrief.core.ui.responsive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.carebrief.core.ui.theme.CareBriefSpacing

/**
 * Phase 25 — responsive helpers.
 *
 * - No hard-coded screen dimensions: every screen centers content in a
 *   max-width container so small phones, standard phones, large phones and
 *   emulators all look intentional.
 * - Compact (<600dp): single column, stacked metrics on very narrow screens.
 * - Medium (600–840dp): same single column but with wider max-width.
 * - Expanded (>840dp): centered column + optional side navigation in shell.
 */
enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberWidthClass(maxWidth: Dp): WindowWidthClass = when {
    maxWidth < 600.dp -> WindowWidthClass.COMPACT
    maxWidth < 840.dp -> WindowWidthClass.MEDIUM
    else -> WindowWidthClass.EXPANDED
}

/** Max content width so large phones/tablets/emulators don't stretch. */
val ResponsiveMaxWidth: Dp = 720.dp

/**
 * Centers [content] in a max-width column with responsive horizontal padding.
 * Handles long text (wraps), large fonts (no clipping) and different widths.
 */
@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ResponsiveMaxWidth,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthClass = rememberWidthClass(maxWidth = this.maxWidth)
        val horizontal = when (widthClass) {
            WindowWidthClass.COMPACT -> CareBriefSpacing.md
            WindowWidthClass.MEDIUM -> CareBriefSpacing.lg
            WindowWidthClass.EXPANDED -> CareBriefSpacing.xl
        }
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(horizontal = horizontal)
        ) {
            content()
        }
    }
}

/**
 * Three-up metrics that collapse to a 2+1 / stacked layout on narrow phones
 * instead of squeezing text. Used by the dashboard (Phase 25).
 */
@Composable
fun AdaptiveMetricsRow(
    modifier: Modifier = Modifier,
    metric: @Composable (Modifier) -> Unit,
    metric2: @Composable (Modifier) -> Unit,
    metric3: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // ~320dp small phones: stack to avoid clipped numbers/labels.
        if (maxWidth < 360.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm)) {
                metric(Modifier.fillMaxWidth())
                metric2(Modifier.fillMaxWidth())
                metric3(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(CareBriefSpacing.sm)) {
                metric(Modifier.weight(1f))
                metric2(Modifier.weight(1f))
                metric3(Modifier.weight(1f))
            }
        }
    }
}

/** Density-aware helper for tests: classifies a raw width in dp. */
fun widthClassFor(widthDp: Float): WindowWidthClass = when {
    widthDp < 600f -> WindowWidthClass.COMPACT
    widthDp < 840f -> WindowWidthClass.MEDIUM
    else -> WindowWidthClass.EXPANDED
}

@Composable
fun Dp.toSpAdaptive(): androidx.compose.ui.unit.TextUnit {
    // Keeps touch targets stable while text scales with system font size.
    return with(LocalDensity.current) { this@toSpAdaptive.toSp() }
}
