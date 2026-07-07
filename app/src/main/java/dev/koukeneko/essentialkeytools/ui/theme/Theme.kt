package dev.koukeneko.essentialkeytools.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Dark is the primary Nothing look: pure-black canvas, #1A1A1A cards, white ink, red as the
// single signal accent. No dynamic color — the palette is intentional and brand-consistent.
private val DarkColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingBlack,
    onSurface = NothingWhite,
    surfaceContainer = NothingDarkSurface,
    surfaceContainerHigh = NothingDarkSurface,
    surfaceContainerHighest = NothingDarkSurface,
    surfaceContainerLow = NothingDarkSurface,
    surfaceContainerLowest = NothingBlack,
    onSurfaceVariant = NothingGray,
    outline = NothingGray,
    error = NothingRed,
    onError = NothingWhite,
    tertiary = NothingRed,
    onTertiary = NothingWhite
)

// Light mirror: off-white canvas, white cards, black ink; red stays the lone accent.
private val LightColorScheme = lightColorScheme(
    primary = NothingBlack,
    onPrimary = NothingWhite,
    background = NothingOffWhite,
    onBackground = NothingBlack,
    surface = NothingOffWhite,
    onSurface = NothingBlack,
    surfaceContainer = NothingWhite,
    surfaceContainerHigh = NothingWhite,
    surfaceContainerHighest = NothingWhite,
    surfaceContainerLow = NothingWhite,
    surfaceContainerLowest = NothingOffWhite,
    onSurfaceVariant = NothingGray,
    outline = NothingGray,
    error = NothingRed,
    onError = NothingWhite,
    tertiary = NothingRed,
    onTertiary = NothingWhite
)

@Composable
fun EssentialKeyToolsTheme(
    // Default to dark; the app leads with the black Nothing aesthetic.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NothingShapes,
        content = content
    )
}
