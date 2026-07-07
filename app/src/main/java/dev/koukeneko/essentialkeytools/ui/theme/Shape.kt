package dev.koukeneko.essentialkeytools.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Nothing surfaces favour large, soft corners. Buttons are separately made fully round (pill)
// at the component level via CircleShape; these tokens cover cards, dialogs and sheets.
val NothingShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
