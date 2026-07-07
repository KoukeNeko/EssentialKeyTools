package dev.koukeneko.essentialkeytools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.ui.theme.NothingGray
import dev.koukeneko.essentialkeytools.ui.theme.NothingRed

// Small circular indicator. Per Nothing convention red signals a live/active state while gray
// signals inactive. This is the single sanctioned place for red in the status UI.
private val DOT_SIZE = 10.dp

@Composable
fun StatusDot(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (active) NothingRed else NothingGray
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(DOT_SIZE)
            .clip(CircleShape)
            .background(color)
    )
}
