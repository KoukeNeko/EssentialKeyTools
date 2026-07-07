package dev.koukeneko.essentialkeytools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.koukeneko.essentialkeytools.ui.theme.NothingGray

// Small uppercase monospace section header (overline style). The text is force-uppercased so
// callers can pass ordinary strings and still get the Nothing treatment.
@Composable
fun NothingSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = NothingGray
    )
}
