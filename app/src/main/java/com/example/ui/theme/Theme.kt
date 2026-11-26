package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FireZoneColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = TacticalObsidian,
    primaryContainer = FieryOrange,
    onPrimaryContainer = TextPrimary,
    secondary = DiamondCyan,
    onSecondary = TacticalObsidian,
    secondaryContainer = DiamondDeep,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSafe,
    onTertiary = TacticalObsidian,
    background = TacticalObsidian,
    onBackground = TextPrimary,
    surface = CharcoalPanel,
    onSurface = TextPrimary,
    surfaceVariant = SteelSlate,
    onSurfaceVariant = TextSecondary,
    outline = SteelBorder,
    error = CrimsonAlert,
    onError = Color.White,
)

private val FireZoneShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
)

@Composable
fun FireZoneTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FireZoneColorScheme,
        typography = FireZoneTypography,
        shapes = FireZoneShapes,
        content = content,
    )
}
