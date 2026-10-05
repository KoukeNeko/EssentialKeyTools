package dev.koukeneko.essentialkeytools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// Small circular indicator: the tertiary accent signals a live/active state, outline gray signals
// inactive. In the Nothing scheme tertiary is red, the single sanctioned red in the status UI.
private val DOT_SIZE = 10.dp

@Composable
fun StatusDot(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(DOT_SIZE)
            .clip(CircleShape)
            .background(color)
    )
}
