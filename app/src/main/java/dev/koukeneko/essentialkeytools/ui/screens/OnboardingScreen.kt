package dev.koukeneko.essentialkeytools.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.settings.OnboardingStep
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme

private const val DEVICE_LANGUAGE_TAG = ""
private const val ENGLISH_LANGUAGE_TAG = "en-US"
private const val TRADITIONAL_CHINESE_LANGUAGE_TAG = "zh-TW"

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_STEP_GAP = 12.dp
private val STEP_TO_HEADLINE_GAP = 24.dp
private val HEADLINE_TO_BODY_GAP = 12.dp
private val BODY_TO_CARD_GAP = 24.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val LANGUAGE_OPTION_GAP = 12.dp
private val RADIO_TO_TEXT_GAP = 16.dp
private val ACTION_GAP = 12.dp
private val CONTENT_TO_ACTIONS_GAP = 24.dp

/**
 * First-run setup. Language comes first so every following explanation is readable, followed by an
 * introduction and a separate prominent disclosure immediately before Android accessibility
 * settings can open. Each page transition reports its progress for persistence. Skipping or leaving
 * keeps that progress incomplete; either accessibility choice completes the guide, and only the
 * positive choice opens settings.
 */
@Composable
fun OnboardingScreen(
    initialStep: OnboardingStep,
    onStepChanged: (OnboardingStep) -> Unit,
    initialLanguageTag: String,
    onLanguageSelected: (String) -> Unit,
    onLeaveOnboarding: () -> Unit,
    onContinueWithoutAccessibility: () -> Unit,
    onUseAccessibility: () -> Unit,
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableStateOf(initialStep) }
    var selectedLanguageTag by rememberSaveable {
        mutableStateOf(supportedLanguageTag(initialLanguageTag))
    }

    fun navigateTo(nextStep: OnboardingStep) {
        step = nextStep
        onStepChanged(nextStep)
    }

    BackHandler {
        when (step) {
            OnboardingStep.ACCESSIBILITY -> navigateTo(OnboardingStep.INTRODUCTION)
            OnboardingStep.INTRODUCTION -> navigateTo(OnboardingStep.LANGUAGE)
            OnboardingStep.LANGUAGE -> onLeaveOnboarding()
        }
    }

    when (step) {
        OnboardingStep.LANGUAGE -> OnboardingLanguage(
            selectedLanguageTag = selectedLanguageTag,
            onLanguageSelected = { languageTag ->
                selectedLanguageTag = languageTag
                onLanguageSelected(languageTag)
            },
            onContinue = { navigateTo(OnboardingStep.INTRODUCTION) },
            systemBarsPadding = systemBarsPadding,
            modifier = modifier
        )

        OnboardingStep.INTRODUCTION -> OnboardingIntro(
            onContinue = { navigateTo(OnboardingStep.ACCESSIBILITY) },
            onSkip = onLeaveOnboarding,
            systemBarsPadding = systemBarsPadding,
            modifier = modifier
        )

        OnboardingStep.ACCESSIBILITY -> OnboardingPermission(
            onContinueWithoutAccessibility = onContinueWithoutAccessibility,
            onUseAccessibility = onUseAccessibility,
            systemBarsPadding = systemBarsPadding,
            modifier = modifier
        )
    }
}

private fun supportedLanguageTag(languageTags: String): String {
    val primaryLanguageTag = languageTags.substringBefore(',')
    return when {
        primaryLanguageTag.startsWith("en", ignoreCase = true) -> ENGLISH_LANGUAGE_TAG
        primaryLanguageTag.startsWith("zh", ignoreCase = true) ->
            TRADITIONAL_CHINESE_LANGUAGE_TAG
        else -> DEVICE_LANGUAGE_TAG
    }
}

@Composable
private fun OnboardingLanguage(
    selectedLanguageTag: String,
    onLanguageSelected: (String) -> Unit,
    onContinue: () -> Unit,
    systemBarsPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    OnboardingPage(
        title = stringResource(R.string.onboarding_language_title),
        step = stringResource(R.string.onboarding_step_language),
        headline = stringResource(R.string.onboarding_language_headline),
        body = stringResource(R.string.onboarding_language_body),
        systemBarsPadding = systemBarsPadding,
        modifier = modifier
    ) {
        LanguageOptionCard(
            title = stringResource(R.string.onboarding_language_device),
            body = stringResource(R.string.onboarding_language_device_body),
            selected = selectedLanguageTag == DEVICE_LANGUAGE_TAG,
            onClick = { onLanguageSelected(DEVICE_LANGUAGE_TAG) }
        )
        Spacer(modifier = Modifier.height(LANGUAGE_OPTION_GAP))
        LanguageOptionCard(
            title = stringResource(R.string.onboarding_language_english),
            body = stringResource(R.string.onboarding_language_english_body),
            selected = selectedLanguageTag == ENGLISH_LANGUAGE_TAG,
            onClick = { onLanguageSelected(ENGLISH_LANGUAGE_TAG) }
        )
        Spacer(modifier = Modifier.height(LANGUAGE_OPTION_GAP))
        LanguageOptionCard(
            title = stringResource(R.string.onboarding_language_traditional_chinese),
            body = stringResource(R.string.onboarding_language_traditional_chinese_body),
            selected = selectedLanguageTag == TRADITIONAL_CHINESE_LANGUAGE_TAG,
            onClick = { onLanguageSelected(TRADITIONAL_CHINESE_LANGUAGE_TAG) }
        )
        Spacer(modifier = Modifier.height(CONTENT_TO_ACTIONS_GAP))
        NothingButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.tertiary
                )
            )
            Spacer(modifier = Modifier.width(RADIO_TO_TEXT_GAP))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    onContinueWithoutAccessibility: () -> Unit,
    onUseAccessibility: () -> Unit,
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
            text = stringResource(R.string.onboarding_use_accessibility),
            onClick = { decideOnce(onUseAccessibility) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(ACTION_GAP))
        NothingButton(
            text = stringResource(R.string.onboarding_continue_without_accessibility),
            onClick = { decideOnce(onContinueWithoutAccessibility) },
            outlined = true,
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
            initialStep = OnboardingStep.LANGUAGE,
            onStepChanged = {},
            initialLanguageTag = DEVICE_LANGUAGE_TAG,
            onLanguageSelected = {},
            onLeaveOnboarding = {},
            onContinueWithoutAccessibility = {},
            onUseAccessibility = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OnboardingPermissionPreview() {
    EssentialKeyToolsTheme {
        OnboardingPermission(
            onContinueWithoutAccessibility = {},
            onUseAccessibility = {},
            systemBarsPadding = PaddingValues()
        )
    }
}
