package dev.koukeneko.essentialkeytools.ui.screens

import android.content.ComponentName
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.contributors.Contributor
import dev.koukeneko.essentialkeytools.contributors.GitHubContributorsService
import dev.koukeneko.essentialkeytools.core.KeyGesture
import dev.koukeneko.essentialkeytools.service.EssentialKeyDetectionService
import dev.koukeneko.essentialkeytools.settings.GestureActionMap
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.AppLabelResolver
import dev.koukeneko.essentialkeytools.ui.PRIVACY_POLICY_URL
import dev.koukeneko.essentialkeytools.ui.UiLabels
import dev.koukeneko.essentialkeytools.ui.openExternalUrl
import dev.koukeneko.essentialkeytools.ui.openPlayStoreListing
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import dev.koukeneko.essentialkeytools.ui.components.AccessibilityDisclosureDialog
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.components.StatusDot
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import dev.koukeneko.essentialkeytools.unlock.UnlockStatus
import dev.koukeneko.essentialkeytools.unlock.UnlockerFactory
import dev.koukeneko.essentialkeytools.updates.AppUpdateCheckerFactory
import dev.koukeneko.essentialkeytools.updates.UpdateCheckResult
import dev.koukeneko.essentialkeytools.updates.UpdateDestination
import dev.koukeneko.essentialkeytools.updates.UpdateSource
import kotlinx.coroutines.launch

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val DOT_TO_TEXT_GAP = 12.dp
private val GESTURE_ROW_GAP = 4.dp
private val GESTURE_ROW_VERTICAL_PADDING = 14.dp
private val NAV_BUTTON_GAP = 12.dp
private val STATUS_TO_ACTION_GAP = 16.dp
private val DISCLOSURE_GAP = 12.dp
private val CONTRIBUTOR_SECTION_GAP = 20.dp
private val UPDATE_ACTION_GAP = 12.dp

// The repository is a proper noun, not translatable copy, so it lives in code; only the captions
// rendered around it come from string resources. The contributor list itself is fetched from the
// GitHub API at runtime by GitHubContributorsService.
private const val URL_SCHEME_PREFIX = "https://"
private const val REPOSITORY_DISPLAY_NAME = "KoukeNeko/EssentialKeyTools"
private const val REPOSITORY_URL = "https://github.com/KoukeNeko/EssentialKeyTools"

/**
 * The main control panel. Surfaces live service and single-press-unlock status, one card per
 * gesture showing its mapped action (tap to reassign), and footer links into setup and test.
 * Mappings come from [SettingsRepository] reactively so a card updates the moment an action is
 * picked; the unlock status is re-read on resume to catch drift from an OS update.
 */
@Composable
fun HomeScreen(
    onEditGesture: (KeyGesture) -> Unit,
    onUnlockWizard: () -> Unit,
    onKeySetup: () -> Unit,
    onKeyTest: () -> Unit,
    onReviewOnboarding: () -> Unit,
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val actionMap by repository.gestureActionMap.collectAsState(initial = GestureActionMap.EMPTY)
    val serviceRunningState = rememberServiceRunningState()
    val unlockStatus = rememberUnlockStatus()

    // Padding sits inside the scroll so the black canvas extends under the bars and the last card
    // clears the nav bar as the content scrolls past it.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))

        ServiceStatusCard(serviceRunning = serviceRunningState)
        Spacer(modifier = Modifier.height(CARD_GAP))
        UnlockStatusCard(status = unlockStatus, onUnlockWizard = onUnlockWizard)
        Spacer(modifier = Modifier.height(CARD_GAP))
        GesturesCard(
            actionMap = actionMap,
            singlePressLocked = unlockStatus != UnlockStatus.FREED,
            onEditGesture = onEditGesture
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        NavigationCard(
            onKeySetup = onKeySetup,
            onKeyTest = onKeyTest,
            onReviewOnboarding = onReviewOnboarding
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        UpdateCard()
        Spacer(modifier = Modifier.height(CARD_GAP))
        LanguageCard()
        Spacer(modifier = Modifier.height(CARD_GAP))
        ContributionCard()
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
            Spacer(modifier = Modifier.height(UPDATE_ACTION_GAP))
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
 * Reads [EssentialKeyDetectionService.isRunning] and refreshes it whenever the screen resumes so the
 * status is current after the user returns from the system accessibility settings.
 */
@Composable
private fun rememberServiceRunningState(): Boolean {
    var running by remember { mutableStateOf(EssentialKeyDetectionService.isRunning) }
    OnResume { running = EssentialKeyDetectionService.isRunning }
    return running
}

/** Reads the live single-press unlock status, re-checked on resume to catch OS-update drift. */
@Composable
private fun rememberUnlockStatus(): UnlockStatus {
    val context = LocalContext.current
    val unlocker = remember { UnlockerFactory.create(context) }
    var status by remember { mutableStateOf(unlocker.readStatus()) }
    OnResume { status = unlocker.readStatus() }
    return status
}

@Composable
private fun OnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun ServiceStatusCard(serviceRunning: Boolean) {
    val context = LocalContext.current
    var disclosureRequested by rememberSaveable { mutableStateOf(false) }

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_service))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(active = serviceRunning)
            Spacer(modifier = Modifier.width(DOT_TO_TEXT_GAP))
            Text(
                text = stringResource(
                    if (serviceRunning) R.string.service_running else R.string.service_not_running
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (!serviceRunning) {
            DisabledServiceControls(
                onRequestOpenSettings = { disclosureRequested = true }
            )
        }
    }

    if (disclosureRequested) {
        AccessibilityDisclosureDialog(
            onContinueWithoutAccessibility = { disclosureRequested = false },
            onUseAccessibility = {
                disclosureRequested = false
                openAccessibilitySettings(context)
            }
        )
    }
}

/** The service is off: explain the system-settings path and request consent before opening it. */
@Composable
private fun DisabledServiceControls(
    onRequestOpenSettings: () -> Unit
) {
    Spacer(modifier = Modifier.height(STATUS_TO_ACTION_GAP))
    Text(
        text = stringResource(R.string.a11y_settings_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(DISCLOSURE_GAP))
    NothingButton(
        text = stringResource(R.string.action_open_accessibility_settings),
        onClick = onRequestOpenSettings,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun UnlockStatusCard(status: UnlockStatus, onUnlockWizard: () -> Unit) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_unlock))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Red dot marks the freed (live) state; gray marks locked/partial — matches StatusDot.
            StatusDot(active = status == UnlockStatus.FREED)
            Spacer(modifier = Modifier.width(DOT_TO_TEXT_GAP))
            Text(
                text = stringResource(unlockStatusLabelRes(status)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(STATUS_TO_ACTION_GAP))
        NothingButton(
            text = stringResource(R.string.action_open_unlock_wizard),
            onClick = onUnlockWizard,
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun unlockStatusLabelRes(status: UnlockStatus): Int = when (status) {
    UnlockStatus.FREED -> R.string.unlock_status_freed
    UnlockStatus.PARTIALLY_FREED -> R.string.unlock_status_partial
    UnlockStatus.LOCKED -> R.string.unlock_status_locked
    UnlockStatus.NO_CONSUMERS -> R.string.unlock_status_none
}

@Composable
private fun GesturesCard(
    actionMap: GestureActionMap,
    singlePressLocked: Boolean,
    onEditGesture: (KeyGesture) -> Unit
) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_gestures))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(GESTURE_ROW_GAP)) {
            for (gesture in KeyGesture.entries) {
                GestureRow(
                    gesture = gesture,
                    action = actionMap.actionFor(gesture),
                    showLockedHint = gesture == KeyGesture.SINGLE_PRESS && singlePressLocked,
                    onClick = { onEditGesture(gesture) }
                )
            }
        }
    }
}

@Composable
private fun GestureRow(
    gesture: KeyGesture,
    action: KeyAction,
    showLockedHint: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = GESTURE_ROW_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(UiLabels.gestureLabelRes(gesture)),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (showLockedHint) {
                Text(
                    text = stringResource(R.string.home_locked_by_nothing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ActionLabel(action = action, context = context)
    }
}

@Composable
private fun ActionLabel(action: KeyAction, context: Context) {
    if (action == KeyAction.None) {
        Text(
            text = stringResource(R.string.action_none_caption),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val label = when (action) {
        is KeyAction.LaunchApp -> AppLabelResolver.labelFor(context, action.packageName)
        else -> stringResource(UiLabels.builtInActionLabelRes(action.id))
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.End
    )
}

@Composable
private fun NavigationCard(
    onKeySetup: () -> Unit,
    onKeyTest: () -> Unit,
    onReviewOnboarding: () -> Unit
) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.section_navigation))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(NAV_BUTTON_GAP)) {
            NothingButton(
                text = stringResource(R.string.action_key_setup),
                onClick = onKeySetup,
                outlined = true,
                modifier = Modifier.fillMaxWidth()
            )
            NothingButton(
                text = stringResource(R.string.action_key_test),
                onClick = onKeyTest,
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
                Column(verticalArrangement = Arrangement.spacedBy(GESTURE_ROW_GAP)) {
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
        modifier = Modifier.padding(vertical = GESTURE_ROW_GAP)
    )
}

/** A single tappable credit row: a primary name over a muted caption, mirroring the gesture rows. */
@Composable
private fun ContributionLinkRow(title: String, caption: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = GESTURE_ROW_VERTICAL_PADDING)
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

private const val HIGHLIGHT_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private const val HIGHLIGHT_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"

/**
 * Opens the accessibility settings, best-effort highlighting our service via the settings-highlight
 * extras. Those extras are undocumented and rejected on some OEM builds, so a failure falls back to
 * the plain accessibility settings intent.
 */
internal fun openAccessibilitySettings(context: Context) {
    val serviceComponent = ComponentName(
        context,
        EssentialKeyDetectionService::class.java
    ).flattenToString()
    val highlighted = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(HIGHLIGHT_FRAGMENT_ARG_KEY, serviceComponent)
        val highlightBundle = android.os.Bundle()
        highlightBundle.putString(HIGHLIGHT_FRAGMENT_ARG_KEY, serviceComponent)
        putExtra(HIGHLIGHT_SHOW_FRAGMENT_ARGS, highlightBundle)
    }
    try {
        context.startActivity(highlighted)
    } catch (error: android.content.ActivityNotFoundException) {
        openPlainAccessibilitySettings(context)
    }
}

private fun openPlainAccessibilitySettings(context: Context) {
    val plain = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    plain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(plain)
    } catch (error: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.a11y_settings_body, Toast.LENGTH_LONG).show()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenPreview() {
    EssentialKeyToolsTheme {
        HomeScreen(
            onEditGesture = {},
            onUnlockWizard = {},
            onKeySetup = {},
            onKeyTest = {},
            onReviewOnboarding = {}
        )
    }
}
