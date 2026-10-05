package com.example.carebrief.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

private val DarkColors = darkColorScheme(
    primary = MintContainer,
    onPrimary = MintOnContainer,
    primaryContainer = TealPrimaryDark,
    onPrimaryContainer = MintContainer,
    secondary = MintContainer,
    secondaryContainer = TealPrimaryDark,
    onSecondaryContainer = MintContainer,
    background = Color(0xFF101413),
    surface = Color(0xFF171C1B),
    surfaceVariant = Color(0xFF22302C),
    outline = Color(0xFF3A4A45),
    onBackground = Color(0xFFE8EDEB),
    onSurface = Color(0xFFE8EDEB),
    onSurfaceVariant = Color(0xFFB9C6C1),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

private val CareBriefShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun CareBriefTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CareBriefTypography,
        shapes = CareBriefShapes,
        content = content
    )
}
