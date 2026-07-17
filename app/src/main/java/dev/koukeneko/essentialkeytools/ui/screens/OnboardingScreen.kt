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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme

private const val INTRO_STEP = 0
private const val PERMISSION_STEP = 1

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_STEP_GAP = 12.dp
private val STEP_TO_HEADLINE_GAP = 24.dp
private val HEADLINE_TO_BODY_GAP = 12.dp
private val BODY_TO_CARD_GAP = 24.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val ACTION_GAP = 12.dp
private val CONTENT_TO_ACTIONS_GAP = 24.dp

/**
 * First-run setup. The second page is the prominent disclosure and affirmative consent step that
 * appears before Android accessibility settings are opened. Declining, skipping, or pressing Back
 * never opens settings and still leaves the same setup guide reachable from the home screen.
 */
@Composable
fun OnboardingScreen(
    onExit: () -> Unit,
    onAgreeAndOpenSettings: () -> Unit,
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableIntStateOf(INTRO_STEP) }

    BackHandler {
        if (step == PERMISSION_STEP) {
            step = INTRO_STEP
        } else {
            onExit()
        }
    }

    when (step) {
        INTRO_STEP -> OnboardingIntro(
            onContinue = { step = PERMISSION_STEP },
            onSkip = onExit,
            systemBarsPadding = systemBarsPadding,
            modifier = modifier
        )

        else -> OnboardingPermission(
            onNotNow = onExit,
            onAgreeAndOpenSettings = onAgreeAndOpenSettings,
            systemBarsPadding = systemBarsPadding,
            modifier = modifier
        )
    }
}

@Composable
private fun OnboardingIntro(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    systemBarsPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    OnboardingPage(
        title = stringResource(R.string.onboarding_title),
        step = stringResource(R.string.onboarding_step_intro),
        headline = stringResource(R.string.onboarding_intro_headline),
        body = stringResource(R.string.onboarding_intro_body),
        systemBarsPadding = systemBarsPadding,
        modifier = modifier
    ) {
        OnboardingInfoCard(
            label = stringResource(R.string.onboarding_intro_gestures_label),
            body = stringResource(R.string.onboarding_intro_gestures_body)
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        OnboardingInfoCard(
            label = stringResource(R.string.onboarding_intro_setup_label),
            body = stringResource(R.string.onboarding_intro_setup_body)
        )
        Spacer(modifier = Modifier.height(CONTENT_TO_ACTIONS_GAP))
        NothingButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(ACTION_GAP))
        NothingButton(
            text = stringResource(R.string.onboarding_skip),
            onClick = onSkip,
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingPermission(
    onNotNow: () -> Unit,
    onAgreeAndOpenSettings: () -> Unit,
    systemBarsPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var decisionMade by remember { mutableStateOf(false) }

    fun decideOnce(decision: () -> Unit) {
        if (!decisionMade) {
            decisionMade = true
            decision()
        }
    }

    OnboardingPage(
        title = stringResource(R.string.onboarding_permission_title),
        step = stringResource(R.string.onboarding_step_permission),
        headline = stringResource(R.string.onboarding_permission_headline),
        body = stringResource(R.string.onboarding_permission_purpose),
        systemBarsPadding = systemBarsPadding,
        modifier = modifier
    ) {
        OnboardingInfoCard(
            label = stringResource(R.string.onboarding_permission_data_label),
            body = stringResource(R.string.onboarding_permission_data_body)
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        OnboardingInfoCard(
            label = stringResource(R.string.onboarding_permission_actions_label),
            body = stringResource(R.string.onboarding_permission_actions_body)
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        OnboardingInfoCard(
            label = stringResource(R.string.onboarding_permission_privacy_label),
            body = stringResource(R.string.onboarding_permission_privacy_body)
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        OnboardingInfoCard(
            label = stringResource(R.string.onboarding_permission_next_label),
            body = stringResource(R.string.onboarding_permission_next_body)
        )
        Spacer(modifier = Modifier.height(CONTENT_TO_ACTIONS_GAP))
        Text(
            text = stringResource(R.string.a11y_disclosure_question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(CONTENT_TO_ACTIONS_GAP))
        NothingButton(
            text = stringResource(R.string.onboarding_not_now),
            onClick = { decideOnce(onNotNow) },
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(ACTION_GAP))
        NothingButton(
            text = stringResource(R.string.onboarding_agree_open_settings),
            onClick = { decideOnce(onAgreeAndOpenSettings) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingPage(
    title: String,
    step: String,
    headline: String,
    body: String,
    systemBarsPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_STEP_GAP))
        NothingSectionLabel(text = step)
        Spacer(modifier = Modifier.height(STEP_TO_HEADLINE_GAP))
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(HEADLINE_TO_BODY_GAP))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(BODY_TO_CARD_GAP))
        content()
    }
}

@Composable
private fun OnboardingInfoCard(label: String, body: String) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = label)
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OnboardingScreenPreview() {
    EssentialKeyToolsTheme {
        OnboardingScreen(
            onExit = {},
            onAgreeAndOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OnboardingPermissionPreview() {
    EssentialKeyToolsTheme {
        OnboardingPermission(
            onNotNow = {},
            onAgreeAndOpenSettings = {},
            systemBarsPadding = PaddingValues()
        )
    }
}
