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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import dev.koukeneko.essentialkeytools.core.KeyGesture
import dev.koukeneko.essentialkeytools.service.EssentialKeyDetectionService
import dev.koukeneko.essentialkeytools.settings.GestureActionMap
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.AppLabelResolver
import dev.koukeneko.essentialkeytools.ui.UiLabels
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.components.StatusDot
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import dev.koukeneko.essentialkeytools.unlock.ServiceToggleResult
import dev.koukeneko.essentialkeytools.unlock.ShizukuAvailability
import dev.koukeneko.essentialkeytools.unlock.ShizukuGate
import dev.koukeneko.essentialkeytools.unlock.UnlockStatus
import dev.koukeneko.essentialkeytools.unlock.UnlockerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
private val ENABLE_PATH_GAP = 16.dp

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
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val actionMap by repository.gestureActionMap.collectAsState(initial = GestureActionMap.EMPTY)
    val serviceRunning = rememberServiceRunningState()
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

        ServiceStatusCard(serviceRunning = serviceRunning)
        Spacer(modifier = Modifier.height(CARD_GAP))
        UnlockStatusCard(status = unlockStatus, onUnlockWizard = onUnlockWizard)
        Spacer(modifier = Modifier.height(CARD_GAP))
        GesturesCard(
            actionMap = actionMap,
            singlePressLocked = unlockStatus != UnlockStatus.FREED,
            onEditGesture = onEditGesture
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        NavigationCard(onKeySetup = onKeySetup, onKeyTest = onKeyTest)
    }
}

/**
 * Reflects [EssentialKeyDetectionService.isRunning], refreshed each time the screen resumes so the
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
    val coroutineScope = rememberCoroutineScope()
    val controller = remember { UnlockerFactory.createServiceController() }
    // Read once per composition; the whole home screen recomposes on resume, so this stays current.
    val shizukuReady = remember(serviceRunning) {
        ShizukuGate.availability() == ShizukuAvailability.READY
    }

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
        if (serviceRunning) {
            EnabledServiceControls(
                shizukuReady = shizukuReady,
                onDisable = { toggleServiceViaShizuku(context, coroutineScope, controller, enable = false) }
            )
        } else {
            DisabledServiceControls(
                shizukuReady = shizukuReady,
                onEnableViaShizuku = {
                    toggleServiceViaShizuku(context, coroutineScope, controller, enable = true)
                },
                onOpenSettings = { openAccessibilitySettings(context) }
            )
        }
    }
}

/**
 * The service is off: always disclose what it does first, then offer the Shizuku one-tap path (if
 * ready) and the Settings fallback. The disclosure is shown before any enabling path per the
 * accessibility-service disclosure requirement.
 */
@Composable
private fun DisabledServiceControls(
    shizukuReady: Boolean,
    onEnableViaShizuku: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Spacer(modifier = Modifier.height(STATUS_TO_ACTION_GAP))
    ServiceDisclosure()
    if (shizukuReady) {
        Spacer(modifier = Modifier.height(ENABLE_PATH_GAP))
        Text(
            text = stringResource(R.string.a11y_shizuku_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(DISCLOSURE_GAP))
        NothingButton(
            text = stringResource(R.string.a11y_enable_via_shizuku),
            onClick = onEnableViaShizuku,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(DISCLOSURE_GAP))
        NothingButton(
            text = stringResource(R.string.a11y_open_settings),
            onClick = onOpenSettings,
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Spacer(modifier = Modifier.height(ENABLE_PATH_GAP))
        Text(
            text = stringResource(R.string.a11y_settings_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(DISCLOSURE_GAP))
        NothingButton(
            text = stringResource(R.string.action_open_accessibility_settings),
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The service is on: offer a symmetric one-tap disable via Shizuku only when Shizuku is ready. */
@Composable
private fun EnabledServiceControls(shizukuReady: Boolean, onDisable: () -> Unit) {
    if (!shizukuReady) {
        return
    }
    Spacer(modifier = Modifier.height(STATUS_TO_ACTION_GAP))
    NothingButton(
        text = stringResource(R.string.a11y_disable_via_shizuku),
        onClick = onDisable,
        outlined = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Collapsible "what/why" disclosure of the accessibility service, expanded by tapping the header. */
@Composable
private fun ServiceDisclosure() {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                if (expanded) R.string.a11y_disclosure_label else R.string.a11y_show_details
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = GESTURE_ROW_GAP)
        )
        if (expanded) {
            Spacer(modifier = Modifier.height(DISCLOSURE_GAP))
            Text(
                text = stringResource(R.string.a11y_disclosure_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
private fun NavigationCard(onKeySetup: () -> Unit, onKeyTest: () -> Unit) {
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
        }
    }
}

private const val HIGHLIGHT_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private const val HIGHLIGHT_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"

/**
 * Runs the enable/disable through Shizuku off the main thread. On any non-success outcome (shell
 * unavailable or a command failure) it falls back to the settings path with a toast, so the user is
 * never left without a way forward. The card refreshes on resume, reflecting the new state.
 */
private fun toggleServiceViaShizuku(
    context: Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    controller: dev.koukeneko.essentialkeytools.unlock.AccessibilityServiceController,
    enable: Boolean
) {
    coroutineScope.launch {
        val result = withContext(Dispatchers.IO) {
            if (enable) controller.enable() else controller.disable()
        }
        when (result) {
            ServiceToggleResult.SUCCEEDED -> {
                val message = if (enable) R.string.a11y_enabled else R.string.a11y_disabled
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            ServiceToggleResult.SHELL_UNAVAILABLE,
            ServiceToggleResult.COMMAND_FAILED -> {
                Toast.makeText(context, R.string.a11y_shizuku_failed, Toast.LENGTH_LONG).show()
                openAccessibilitySettings(context)
            }
        }
    }
}

/**
 * Opens the accessibility settings, best-effort highlighting our service via the settings-highlight
 * extras. Those extras are undocumented and rejected on some OEM builds, so a failure falls back to
 * the plain accessibility settings intent.
 */
private fun openAccessibilitySettings(context: Context) {
    val highlighted = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(HIGHLIGHT_FRAGMENT_ARG_KEY, ourServiceComponent)
        val highlightBundle = android.os.Bundle()
        highlightBundle.putString(HIGHLIGHT_FRAGMENT_ARG_KEY, ourServiceComponent)
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

private val ourServiceComponent: String
    get() = dev.koukeneko.essentialkeytools.unlock.AccessibilityServiceController.OUR_SERVICE_COMPONENT

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenPreview() {
    EssentialKeyToolsTheme {
        HomeScreen(
            onEditGesture = {},
            onUnlockWizard = {},
            onKeySetup = {},
            onKeyTest = {}
        )
    }
}
