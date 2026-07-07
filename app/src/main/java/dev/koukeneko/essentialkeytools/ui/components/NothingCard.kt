package dev.koukeneko.essentialkeytools.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import dev.koukeneko.essentialkeytools.ui.theme.NothingGray

// Nothing cards are flat: a #1A1A1A surface with a large corner radius and a hairline outline
// instead of any elevation shadow. Shadows and gradients are deliberately absent.
private val CARD_CORNER_RADIUS = 24.dp
private val CARD_OUTLINE_WIDTH = 1.dp
private const val CARD_OUTLINE_ALPHA = 0.3f
private val CARD_CONTENT_PADDING = 20.dp

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    showOutline: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val outline = if (showOutline) {
        BorderStroke(CARD_OUTLINE_WIDTH, NothingGray.copy(alpha = CARD_OUTLINE_ALPHA))
    } else {
        null
    }

    androidx.compose.material3.Card(
        modifier = modifier,
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = outline
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(CARD_CONTENT_PADDING),
            content = content
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun NothingCardPreview() {
    EssentialKeyToolsTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NothingCard(modifier = Modifier.padding(16.dp)) {
                Text("CARD CONTENT", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
