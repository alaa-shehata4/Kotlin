package com.example.carebrief.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = MintContainer,
    onPrimaryContainer = MintOnContainer,
    secondary = SageSecondary,
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageOnContainer,
    background = AppBackground,
    surface = AppSurface,
    surfaceVariant = SurfaceVariantCool,
    outline = OutlineCool,
    onBackground = InkPrimary,
    onSurface = InkPrimary,
    onSurfaceVariant = InkSecondary,
    error = CriticalText,
    errorContainer = CriticalContainer
)

private val CareBriefShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun CareBriefTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CareBriefTypography,
        shapes = CareBriefShapes,
        content = content
    )
}
