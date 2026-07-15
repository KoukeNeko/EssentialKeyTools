package dev.koukeneko.essentialkeytools.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import dev.koukeneko.essentialkeytools.ui.theme.NothingGray

private val DIALOG_SCREEN_MARGIN = 24.dp
private val DIALOG_CONTENT_PADDING = 24.dp
private val DIALOG_OUTLINE_WIDTH = 1.dp
private const val DIALOG_OUTLINE_ALPHA = 0.3f
private val TITLE_TO_BODY_GAP = 16.dp
private val BODY_TO_QUESTION_GAP = 16.dp
private val QUESTION_TO_ACTIONS_GAP = 16.dp
private val ACTION_GAP = 12.dp

/**
 * A dedicated prominent-disclosure surface shown immediately before an accessibility enable path.
 * Dismissing the dialog is deliberately equivalent to declining: only [onConsent] may continue.
 */
@Composable
internal fun AccessibilityDisclosureDialog(
    onDecline: () -> Unit,
    onConsent: () -> Unit
) {
    var decisionMade by remember { mutableStateOf(false) }

    fun decideOnce(decision: () -> Unit) {
        if (!decisionMade) {
            decisionMade = true
            decision()
        }
    }

    Dialog(
        onDismissRequest = { decideOnce(onDecline) },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // A full-screen, semantics-free dismiss target restores outside-tap behavior even
            // though the custom dialog window uses the full width to match the app's cards.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { decideOnce(onDecline) }
                    }
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DIALOG_SCREEN_MARGIN),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .pointerInput(Unit) { detectTapGestures { } },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        DIALOG_OUTLINE_WIDTH,
                        NothingGray.copy(alpha = DIALOG_OUTLINE_ALPHA)
                    )
                ) {
                    Column(modifier = Modifier.padding(DIALOG_CONTENT_PADDING)) {
                        Text(
                            text = stringResource(R.string.a11y_disclosure_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(TITLE_TO_BODY_GAP))
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = stringResource(R.string.a11y_disclosure_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(BODY_TO_QUESTION_GAP))
                        Text(
                            text = stringResource(R.string.a11y_disclosure_question),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(QUESTION_TO_ACTIONS_GAP))
                        NothingButton(
                            text = stringResource(R.string.a11y_disclosure_decline),
                            onClick = { decideOnce(onDecline) },
                            outlined = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(ACTION_GAP))
                        NothingButton(
                            text = stringResource(R.string.a11y_disclosure_agree),
                            onClick = { decideOnce(onConsent) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AccessibilityDisclosureDialogPreview() {
    EssentialKeyToolsTheme {
        AccessibilityDisclosureDialog(
            onDecline = {},
            onConsent = {}
        )
    }
}
