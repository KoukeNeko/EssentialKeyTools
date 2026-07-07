package dev.koukeneko.essentialkeytools.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Pill-shaped monochrome buttons. Filled = white-on-black primary action; outlined = secondary.
// CircleShape yields fully rounded ends regardless of button height.
private val BUTTON_HORIZONTAL_PADDING = 24.dp
private val BUTTON_VERTICAL_PADDING = 14.dp

private val buttonContentPadding = PaddingValues(
    horizontal = BUTTON_HORIZONTAL_PADDING,
    vertical = BUTTON_VERTICAL_PADDING
)

@Composable
fun NothingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = CircleShape,
            contentPadding = buttonContentPadding
        ) {
            NothingButtonLabel(text)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = buttonContentPadding
        ) {
            NothingButtonLabel(text)
        }
    }
}

@Composable
private fun NothingButtonLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge
    )
}
