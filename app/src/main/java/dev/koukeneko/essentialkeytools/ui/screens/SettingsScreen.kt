package dev.koukeneko.essentialkeytools.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.actions.KeyHaptics
import dev.koukeneko.essentialkeytools.contributors.Contributor
import dev.koukeneko.essentialkeytools.contributors.GitHubContributorsService
import dev.koukeneko.essentialkeytools.settings.HapticPattern
import dev.koukeneko.essentialkeytools.settings.HapticStrength
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.settings.ThemeStyle
import dev.koukeneko.essentialkeytools.ui.PRIVACY_POLICY_URL
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.openExternalUrl
import dev.koukeneko.essentialkeytools.ui.openPlayStoreListing
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import dev.koukeneko.essentialkeytools.updates.AppUpdateCheckerFactory
import dev.koukeneko.essentialkeytools.updates.UpdateCheckResult
import dev.koukeneko.essentialkeytools.updates.UpdateDestination
import dev.koukeneko.essentialkeytools.updates.UpdateSource
import kotlinx.coroutines.launch

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val ROW_GAP = 4.dp
private val LINK_ROW_VERTICAL_PADDING = 14.dp
private val STATUS_TO_ACTION_GAP = 16.dp
private val CONTRIBUTOR_SECTION_GAP = 20.dp
private val BUTTON_GAP = 12.dp
private val RADIO_TO_TEXT_GAP = 12.dp

// The repository is a proper noun, not translatable copy, so it lives in code; only the captions
// rendered around it come from string resources. The contributor list itself is fetched from the
// GitHub API at runtime by GitHubContributorsService.
private const val URL_SCHEME_PREFIX = "https://"
private const val REPOSITORY_DISPLAY_NAME = "KoukeNeko/EssentialKeyTools"
private const val REPOSITORY_URL = "https://github.com/KoukeNeko/EssentialKeyTools"

/**
 * The app's preferences: haptic feedback, whether actions run on a locked device, theme and
 * language, then updates, the pages that explain or repair the app, and the credits. Choices come
 * from [SettingsRepository] reactively, so each card reflects a change the moment it is made.
 */
@Composable
fun SettingsScreen(
    onEditHapticPattern: () -> Unit,
    onDiagnostics: () -> Unit,
    onReviewOnboarding: () -> Unit,
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val hapticStrength by repository.hapticStrength.collectAsState(initial = HapticStrength.OFF)
    val customHapticPattern by repository.customHapticPattern.collectAsState(
        initial = HapticPattern.DEFAULT
    )
    val hapticsOnActionOnly by repository.hapticsOnActionOnly.collectAsState(initial = false)
    val actionsOnlyWhenUnlocked by repository.actionsOnlyWhenUnlocked.collectAsState(
        initial = false
    )
    val themeStyle by repository.themeStyle.collectAsState(initial = ThemeStyle.NOTHING)
    val coroutineScope = rememberCoroutineScope()

    // Padding sits inside the scroll so the canvas extends under the bars and the last card clears
    // the nav bar as the content scrolls past it.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))

        HapticsCard(
            selected = hapticStrength,
            customPattern = customHapticPattern,
            onSelect = { strength ->
                coroutineScope.launch { repository.setHapticStrength(strength) }
            },
            onEditPattern = onEditHapticPattern,
            onActionOnly = hapticsOnActionOnly,
            onActionOnlyChange = { enabled ->
                coroutineScope.launch { repository.setHapticsOnActionOnly(enabled) }
            }
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        ScreenLockCard(
            actionsOnlyWhenUnlocked = actionsOnlyWhenUnlocked,
            onChange = { enabled ->
                coroutineScope.launch { repository.setActionsOnlyWhenUnlocked(enabled) }
            }
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        ThemeCard(
            selected = themeStyle,
            onSelect = { style -> coroutineScope.launch { repository.setThemeStyle(style) } }
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        LanguageCard()
        Spacer(modifier = Modifier.height(CARD_GAP))
        UpdateCard()
        Spacer(modifier = Modifier.height(CARD_GAP))
        HelpCard(onDiagnostics = onDiagnostics, onReviewOnboarding = onReviewOnboarding)
        Spacer(modifier = Modifier.height(CARD_GAP))
        ContributionCard()
    }
}

/** Picks the gesture vibration strength; choosing one plays it so the user can feel the difference. */
@Composable
private fun HapticsCard(
    selected: HapticStrength,
    customPattern: HapticPattern,
    onSelect: (HapticStrength) -> Unit,
    onEditPattern: () -> Unit,
    onActionOnly: Boolean,
    onActionOnlyChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val keyHaptics = remember(context) { KeyHaptics(context) }
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_haptics))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = stringResource(R.string.haptics_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(LABEL_GAP))
        for (strength in HapticStrength.entries) {
            RadioOptionRow(
                label = stringResource(hapticStrengthLabelRes(strength)),
                selected = strength == selected,
                onClick = {
                    keyHaptics.perform(strength, customPattern)
                    onSelect(strength)
                }
            )
        }
        NothingButton(
            text = stringResource(R.string.haptics_edit_pattern),
            onClick = onEditPattern,
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
        CheckboxOptionRow(
            label = stringResource(R.string.haptics_on_action_only),
            checked = onActionOnly,
            enabled = selected != HapticStrength.OFF,
            onCheckedChange = onActionOnlyChange
        )
    }
}

/** Limits the gestures to a device that is not locked, so a press in a pocket does nothing. */
@Composable
private fun ScreenLockCard(actionsOnlyWhenUnlocked: Boolean, onChange: (Boolean) -> Unit) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_screen_lock))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        CheckboxOptionRow(
            label = stringResource(R.string.screen_lock_only_when_unlocked),
            checked = actionsOnlyWhenUnlocked,
            onCheckedChange = onChange
        )
    }
}

@Composable
private fun CheckboxOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(vertical = ROW_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.tertiary
            )
        )
        Spacer(modifier = Modifier.width(RADIO_TO_TEXT_GAP))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun RadioOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = ROW_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.tertiary
            )
        )
        Spacer(modifier = Modifier.width(RADIO_TO_TEXT_GAP))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Picks the color style. It applies at once because the theme wraps the whole app. */
@Composable
private fun ThemeCard(selected: ThemeStyle, onSelect: (ThemeStyle) -> Unit) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_theme))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        for (style in ThemeStyle.entries) {
            RadioOptionRow(
                label = stringResource(themeStyleLabelRes(style)),
                selected = style == selected,
                onClick = { onSelect(style) }
            )
        }
    }
}

private fun themeStyleLabelRes(style: ThemeStyle): Int = when (style) {
    ThemeStyle.NOTHING -> R.string.theme_nothing
    ThemeStyle.MATERIAL -> R.string.theme_material
}

private fun hapticStrengthLabelRes(strength: HapticStrength): Int = when (strength) {
    HapticStrength.OFF -> R.string.haptics_off
    HapticStrength.LIGHT -> R.string.haptics_light
    HapticStrength.MEDIUM -> R.string.haptics_medium
    HapticStrength.STRONG -> R.string.haptics_strong
    HapticStrength.CUSTOM -> R.string.haptics_custom
}

/**
 * Opens this app's per-app language screen in system settings when tapped. The languages offered
 * there come from the locale config AGP generates from the values-* folders (generateLocaleConfig),
 * so the list stays in sync with the translations the app actually ships.
 */
@Composable
private fun LanguageCard() {
    val context = LocalContext.current
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_language))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        NothingButton(
            text = stringResource(R.string.action_open_language_settings),
            onClick = { openAppLanguageSettings(context) },
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Opens the per-app language screen in system settings (Android 13+). Falls back to the app details
 * page, then a toast, on OEM builds that do not surface the locale screen directly.
 */
private fun openAppLanguageSettings(context: Context) {
    val packageUri = android.net.Uri.fromParts("package", context.packageName, null)
    val localeSettings = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, packageUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(localeSettings)
    } catch (error: android.content.ActivityNotFoundException) {
        openAppDetailsSettings(context, packageUri)
    }
}

private fun openAppDetailsSettings(context: Context, packageUri: android.net.Uri) {
    val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(details)
    } catch (error: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.language_settings_unavailable, Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun HelpCard(onDiagnostics: () -> Unit, onReviewOnboarding: () -> Unit) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_help))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(BUTTON_GAP)) {
            NothingButton(
                text = stringResource(R.string.action_open_diagnostics),
                onClick = onDiagnostics,
                outlined = true,
                modifier = Modifier.fillMaxWidth()
            )
            NothingButton(
                text = stringResource(R.string.action_review_onboarding),
                onClick = onReviewOnboarding,
                outlined = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Manual, source-aware update check. No executable content is downloaded by the app itself. */
@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    val checker = remember(context) { AppUpdateCheckerFactory.create(context) }
    val coroutineScope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

    fun checkForUpdate() {
        if (state == UpdateUiState.Checking) return
        state = UpdateUiState.Checking
        coroutineScope.launch {
            state = checker.check().fold(
                onSuccess = { result ->
                    when (result) {
                        UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                        is UpdateCheckResult.Available -> UpdateUiState.Available(
                            versionName = result.versionName,
                            destination = result.destination
                        )
                    }
                },
                onFailure = { UpdateUiState.Error }
            )
        }
    }

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_updates))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = stringResource(R.string.update_current_version, checker.currentVersionName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(updateSourceLabelRes(checker.source)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(STATUS_TO_ACTION_GAP))
        Text(
            text = updateStatusText(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(STATUS_TO_ACTION_GAP))

        val availableState = state as? UpdateUiState.Available
        if (availableState != null) {
            NothingButton(
                text = stringResource(
                    when (availableState.destination) {
                        UpdateDestination.PlayStore -> R.string.action_update_on_play
                        is UpdateDestination.GitHubRelease -> R.string.action_view_github_release
                    }
                ),
                onClick = {
                    when (val destination = availableState.destination) {
                        UpdateDestination.PlayStore -> openPlayStoreListing(context)
                        is UpdateDestination.GitHubRelease ->
                            openExternalUrl(context, destination.url)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(BUTTON_GAP))
        }

        NothingButton(
            text = stringResource(
                if (state == UpdateUiState.Idle) {
                    R.string.action_check_updates
                } else {
                    R.string.action_check_updates_again
                }
            ),
            onClick = ::checkForUpdate,
            outlined = availableState != null,
            enabled = state != UpdateUiState.Checking,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data object Error : UpdateUiState
    data class Available(
        val versionName: String?,
        val destination: UpdateDestination
    ) : UpdateUiState
}

@Composable
private fun updateStatusText(state: UpdateUiState): String = when (state) {
    UpdateUiState.Idle -> stringResource(R.string.update_status_idle)
    UpdateUiState.Checking -> stringResource(R.string.update_status_checking)
    UpdateUiState.UpToDate -> stringResource(R.string.update_status_up_to_date)
    UpdateUiState.Error -> stringResource(R.string.update_status_error)
    is UpdateUiState.Available -> if (state.versionName == null) {
        stringResource(R.string.update_status_available_play)
    } else {
        stringResource(R.string.update_status_available_version, state.versionName)
    }
}

private fun updateSourceLabelRes(source: UpdateSource): Int = when (source) {
    UpdateSource.PLAY_STORE -> R.string.update_source_play
    UpdateSource.GITHUB_STABLE -> R.string.update_source_github_stable
    UpdateSource.GITHUB_PREVIEW -> R.string.update_source_github_preview
}

/**
 * Footer credit: a tappable link to the open-source repository, followed by the contributor list
 * fetched live from the GitHub API. Each row opens the relevant GitHub page in the browser.
 */
@Composable
private fun ContributionCard() {
    val context = LocalContext.current
    val contributorsState = rememberContributorsState()
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_contribute))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        ContributionLinkRow(
            title = REPOSITORY_DISPLAY_NAME,
            caption = stringResource(R.string.contribute_repository_caption),
            onClick = { openExternalUrl(context, REPOSITORY_URL) }
        )
        ContributionLinkRow(
            title = stringResource(R.string.privacy_policy_title),
            caption = stringResource(R.string.privacy_policy_caption),
            onClick = { openExternalUrl(context, PRIVACY_POLICY_URL) }
        )
        Spacer(modifier = Modifier.height(CONTRIBUTOR_SECTION_GAP))
        NothingSectionLabel(text = stringResource(R.string.contribute_contributors))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        ContributorsSection(
            state = contributorsState,
            onOpenProfile = { profileUrl -> openExternalUrl(context, profileUrl) }
        )
    }
}

/** Snapshot of the asynchronous contributor fetch, driving what the contributors section renders. */
private sealed interface ContributorsUiState {
    data object Loading : ContributorsUiState
    data class Loaded(val contributors: List<Contributor>) : ContributorsUiState
    data object Error : ContributorsUiState
}

/** Fetches the contributor list once when the card enters composition, off the main thread. */
@Composable
private fun rememberContributorsState(): ContributorsUiState {
    val service = remember { GitHubContributorsService() }
    return produceState<ContributorsUiState>(ContributorsUiState.Loading, service) {
        value = service.fetchContributors().fold(
            onSuccess = { contributors -> ContributorsUiState.Loaded(contributors) },
            onFailure = { ContributorsUiState.Error }
        )
    }.value
}

/**
 * Renders the contributor rows once loaded, a muted caption while loading, and the same caption on
 * failure so an offline device still shows a coherent card with the repository link intact.
 */
@Composable
private fun ContributorsSection(state: ContributorsUiState, onOpenProfile: (String) -> Unit) {
    when (state) {
        ContributorsUiState.Loading ->
            ContributionCaption(text = stringResource(R.string.contribute_contributors_loading))
        ContributorsUiState.Error ->
            ContributionCaption(text = stringResource(R.string.contribute_contributors_error))
        is ContributorsUiState.Loaded ->
            if (state.contributors.isEmpty()) {
                ContributionCaption(text = stringResource(R.string.contribute_contributors_error))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
                    for (contributor in state.contributors) {
                        ContributionLinkRow(
                            title = contributor.handle,
                            caption = contributor.profileUrl.removePrefix(URL_SCHEME_PREFIX),
                            onClick = { onOpenProfile(contributor.profileUrl) }
                        )
                    }
                }
            }
    }
}

/** A single muted caption line used for the loading and error states of the contributor list. */
@Composable
private fun ContributionCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = ROW_GAP)
    )
}

/** A single tappable credit row: a primary name over a muted caption, mirroring the gesture rows. */
@Composable
private fun ContributionLinkRow(title: String, caption: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = LINK_ROW_VERTICAL_PADDING)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
