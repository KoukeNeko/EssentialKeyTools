package dev.koukeneko.essentialkeytools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.koukeneko.essentialkeytools.settings.ThemeStyle

// Dark is the primary Nothing look: pure-black canvas, #1A1A1A cards, white ink, red as the
// single signal accent. The palette is fixed on purpose; Material You is the opt-in alternative.
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
    themeStyle: ThemeStyle = ThemeStyle.NOTHING,
    // The Nothing look leads with black; Material You follows the system like any Material You app.
    darkTheme: Boolean = themeStyle == ThemeStyle.NOTHING || isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeStyle) {
        ThemeStyle.NOTHING -> if (darkTheme) DarkColorScheme else LightColorScheme
        ThemeStyle.MATERIAL -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NothingShapes,
        content = content
    )
}
