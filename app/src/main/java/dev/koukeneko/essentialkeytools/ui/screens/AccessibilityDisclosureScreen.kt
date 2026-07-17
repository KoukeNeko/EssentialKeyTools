package dev.koukeneko.essentialkeytools.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_DISCLOSURE_GAP = 24.dp
private val DISCLOSURE_TO_QUESTION_GAP = 24.dp
private val QUESTION_TO_ACTIONS_GAP = 24.dp
private val ACTION_GAP = 12.dp

/**
 * Full-page prominent disclosure shown immediately before Android accessibility settings may open.
 * Back and the explicit decline action continue without accessibility; only the explicit agree
 * action may proceed to system settings.
 */
@Composable
internal fun AccessibilityDisclosureScreen(
    onBackWithoutAccessibility: () -> Unit,
    onContinueWithoutAccessibility: () -> Unit,
    onUseAccessibility: () -> Unit,
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    var decisionMade by remember { mutableStateOf(false) }

    fun decideOnce(decision: () -> Unit) {
        if (!decisionMade) {
            decisionMade = true
            decision()
        }
    }

    BackHandler { decideOnce(onBackWithoutAccessibility) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.a11y_disclosure_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_DISCLOSURE_GAP))
        NothingCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.a11y_disclosure_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(DISCLOSURE_TO_QUESTION_GAP))
        Text(
            text = stringResource(R.string.a11y_disclosure_question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(QUESTION_TO_ACTIONS_GAP))
        NothingButton(
            text = stringResource(R.string.a11y_disclosure_use_accessibility),
            onClick = { decideOnce(onUseAccessibility) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(ACTION_GAP))
        NothingButton(
            text = stringResource(R.string.a11y_disclosure_continue_without_accessibility),
            onClick = { decideOnce(onContinueWithoutAccessibility) },
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AccessibilityDisclosureScreenPreview() {
    EssentialKeyToolsTheme {
        AccessibilityDisclosureScreen(
            onBackWithoutAccessibility = {},
            onContinueWithoutAccessibility = {},
            onUseAccessibility = {}
        )
    }
}
