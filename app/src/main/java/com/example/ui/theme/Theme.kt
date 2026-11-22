package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class JarvisHudColors(
    val cyanGlow: Color = ArcCyan,
    val amberAccent: Color = StarkAmber,
    val emeraldStatus: Color = SignalEmerald,
    val crimsonAlert: Color = AlertCrimson,
    val panelBorder: Color = HudBorder,
    val panelElevated: Color = HudSurfaceElevated,
    val voidBackdrop: Color = ObsidianVoid,
    val mutedSteel: Color = SteelMuted
)

val LocalJarvisHudColors = staticCompositionLocalOf { JarvisHudColors() }

private val JarvisDarkColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = ArcCyanContainer,
    onPrimaryContainer = IceWhite,
    secondary = StarkAmber,
    onSecondary = ObsidianVoid,
    secondaryContainer = StarkAmberContainer,
    onSecondaryContainer = IceWhite,
    tertiary = SignalEmerald,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = IceWhite,
    surface = HudSurface,
    onSurface = IceWhite,
    surfaceVariant = HudSurfaceElevated,
    onSurfaceVariant = SteelBlue,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianDeep,
    surfaceContainer = HudSurface,
    surfaceContainerHigh = HudSurfaceElevated,
    surfaceContainerHighest = HudSurfaceBright,
    outline = HudBorder,
    outlineVariant = SteelMuted,
    error = AlertCrimson,
    onError = ObsidianVoid,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = IceWhite
)

private val JarvisShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun JarvisAiTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalJarvisHudColors provides JarvisHudColors()) {
        MaterialTheme(
            colorScheme = JarvisDarkColorScheme,
            typography = Typography,
            shapes = JarvisShapes,
            content = content
        )
    }
}
