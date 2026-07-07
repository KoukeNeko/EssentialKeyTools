package dev.koukeneko.essentialkeytools.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.koukeneko.essentialkeytools.R

// Doto (OFL, Google Fonts): dot-matrix display face for clock-style numerals and large titles.
// NDot/NType 82 are Nothing-brand-only, so Doto is the legal stand-in (see docs/RESEARCH.md).
val DotoFontFamily = FontFamily(
    Font(R.font.doto_variable, FontWeight.Normal),
    Font(R.font.doto_variable, FontWeight.Medium),
    Font(R.font.doto_variable, FontWeight.Bold)
)

// Space Mono (OFL): monospace face for uppercase section labels and overline-style headers.
val SpaceMonoFontFamily = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold)
)

// Body copy uses the platform sans (Roboto), matching Nothing OS 3.0+ body typography.
val BodyFontFamily = FontFamily.Default

// Letter-spacing is expressed in sp because Compose text styles take absolute tracking.
private val DISPLAY_LETTER_SPACING = 2.sp
private val LABEL_LETTER_SPACING = 1.5.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = DISPLAY_LETTER_SPACING
    ),
    displayMedium = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = DISPLAY_LETTER_SPACING
    ),
    headlineLarge = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = DISPLAY_LETTER_SPACING
    ),
    headlineMedium = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = DISPLAY_LETTER_SPACING
    ),
    titleLarge = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = DISPLAY_LETTER_SPACING
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // labelLarge is used for pill button text: uppercase monospace reads as Nothing-native.
    labelLarge = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = LABEL_LETTER_SPACING
    ),
    // labelSmall is the overline-style section header (small, uppercase, tracked, gray).
    labelSmall = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = LABEL_LETTER_SPACING
    )
)
