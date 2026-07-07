package dev.koukeneko.essentialkeytools.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.unlock.NothingConsumerPackages
import dev.koukeneko.essentialkeytools.unlock.PackageActionResult
import dev.koukeneko.essentialkeytools.unlock.PackageState
import dev.koukeneko.essentialkeytools.unlock.ShizukuAvailability
import dev.koukeneko.essentialkeytools.unlock.ShizukuGate
import dev.koukeneko.essentialkeytools.unlock.UnlockCommands
import dev.koukeneko.essentialkeytools.unlock.UnlockRunResult
import dev.koukeneko.essentialkeytools.unlock.UnlockerFactory
import dev.koukeneko.essentialkeytools.ui.AppLabelResolver
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val ROW_GAP = 8.dp
private val BUTTON_GAP = 12.dp
private val COMMAND_BLOCK_GAP = 12.dp

private const val SHIZUKU_URL = "https://shizuku.rikka.app"
private const val SHIZUKU_PERMISSION_REQUEST_CODE = 4201
private const val CLIP_LABEL = "adb-commands"

/**
 * Guides the user through freeing the Essential Key's single press. Presents the live consumer-
 * package state plus two paths: Shizuku (on-device) and ADB (from a PC), each with a matching
 * restore. All state is re-read on resume so drift caused by an OS update is reflected.
 */
@Composable
fun UnlockWizardScreen(
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val unlocker = remember { UnlockerFactory.create(context) }
    val coroutineScope = rememberCoroutineScope()

    var packageStates by remember { mutableStateOf(unlocker.readPackageStates()) }
    var shizukuAvailability by remember { mutableStateOf(ShizukuGate.availability()) }
    var lastRun by remember { mutableStateOf<UnlockRunResult?>(null) }

    fun refresh() {
        packageStates = unlocker.readPackageStates()
        shizukuAvailability = ShizukuGate.availability()
    }

    RefreshOnResume(::refresh)
    ShizukuPermissionListener(onResult = { refresh() })

    fun runFree() {
        coroutineScope.launch {
            lastRun = withContext(Dispatchers.IO) { unlocker.freeSinglePress() }
            refresh()
        }
    }

    fun runRestore() {
        coroutineScope.launch {
            lastRun = withContext(Dispatchers.IO) { unlocker.restoreSinglePress() }
            refresh()
        }
    }

    // Padding sits inside the scroll so the black canvas reaches under the bars and the last card
    // clears the nav bar as the content scrolls past it.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.unlock_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))

        WarningCard()
        Spacer(modifier = Modifier.height(CARD_GAP))
        PackageStatesCard(states = packageStates)
        Spacer(modifier = Modifier.height(CARD_GAP))
        ShizukuPathCard(
            availability = shizukuAvailability,
            lastRun = lastRun,
            onRequestPermission = {
                ShizukuGate.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            },
            onFree = ::runFree,
            onRestore = ::runRestore,
            onGetShizuku = { openUrl(context, SHIZUKU_URL) }
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        ManualPathCard(states = packageStates)
        Spacer(modifier = Modifier.height(CARD_GAP))
        AdbPathCard()
    }
}

@Composable
private fun RefreshOnResume(onResume: () -> Unit) {
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
private fun ShizukuPermissionListener(onResult: () -> Unit) {
    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, _ -> onResult() }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }
}

@Composable
private fun WarningCard() {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.unlock_warning_label))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = stringResource(R.string.unlock_warning_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PackageStatesCard(states: List<PackageState>) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.unlock_packages_label))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
            for (state in states) {
                PackageStateRow(state)
            }
        }
    }
}

@Composable
private fun PackageStateRow(state: PackageState) {
    val statusRes = when {
        !state.installed -> R.string.unlock_package_absent
        state.isFreed -> R.string.unlock_package_freed
        else -> R.string.unlock_package_active
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = state.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(ROW_GAP))
        Text(
            text = stringResource(statusRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShizukuPathCard(
    availability: ShizukuAvailability,
    lastRun: UnlockRunResult?,
    onRequestPermission: () -> Unit,
    onFree: () -> Unit,
    onRestore: () -> Unit,
    onGetShizuku: () -> Unit
) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.unlock_shizuku_label))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        when (availability) {
            ShizukuAvailability.READY -> ShizukuReadyContent(lastRun, onFree, onRestore)
            ShizukuAvailability.PERMISSION_REQUIRED -> ShizukuActionText(
                message = null,
                buttonRes = R.string.unlock_shizuku_permission,
                onClick = onRequestPermission
            )
            ShizukuAvailability.NOT_RUNNING -> ShizukuActionText(
                message = stringResource(R.string.unlock_shizuku_not_running),
                buttonRes = null,
                onClick = {}
            )
            ShizukuAvailability.NOT_INSTALLED -> ShizukuActionText(
                message = stringResource(R.string.unlock_shizuku_not_installed),
                buttonRes = R.string.unlock_shizuku_get,
                onClick = onGetShizuku
            )
        }
        // Shizuku itself can be started without a PC, so surface that regardless of the current state.
        if (availability != ShizukuAvailability.READY) {
            Spacer(modifier = Modifier.height(LABEL_GAP))
            Text(
                text = stringResource(R.string.unlock_shizuku_wireless_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShizukuReadyContent(
    lastRun: UnlockRunResult?,
    onFree: () -> Unit,
    onRestore: () -> Unit
) {
    Text(
        text = stringResource(R.string.unlock_shizuku_ready),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(BUTTON_GAP))
    NothingButton(
        text = stringResource(R.string.unlock_action_free),
        onClick = onFree,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(BUTTON_GAP))
    NothingButton(
        text = stringResource(R.string.unlock_action_restore),
        onClick = onRestore,
        outlined = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (lastRun != null) {
        Spacer(modifier = Modifier.height(BUTTON_GAP))
        RunResults(lastRun)
    }
}

@Composable
private fun RunResults(result: UnlockRunResult) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        for (perPackage in result.perPackage) {
            PackageResultRow(perPackage)
        }
        val summaryRes =
            if (result.anyFailed) R.string.unlock_result_partial else R.string.unlock_result_success
        Text(
            text = stringResource(summaryRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PackageResultRow(result: PackageActionResult) {
    // Red only appears here on a failed package — the one sanctioned accent on this screen.
    val markerColor =
        if (result.succeeded) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = result.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (result.succeeded) "OK" else "FAIL",
            style = MaterialTheme.typography.labelSmall,
            color = markerColor
        )
    }
}

@Composable
private fun ShizukuActionText(message: String?, buttonRes: Int?, onClick: () -> Unit) {
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (buttonRes != null) {
            Spacer(modifier = Modifier.height(BUTTON_GAP))
        }
    }
    if (buttonRes != null) {
        NothingButton(
            text = stringResource(buttonRes),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The manual path: no PC and no Shizuku. For each installed consumer package it shows the package
 * label, its current freed/active state, and a button into that package's App Info where the user
 * can tap Disable. States come from the same list the wizard re-reads on resume, so returning from
 * Settings refreshes each row.
 */
@Composable
private fun ManualPathCard(states: List<PackageState>) {
    val context = LocalContext.current
    val installed = states.filter { state -> state.installed }
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.unlock_manual_label))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = stringResource(R.string.unlock_manual_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(BUTTON_GAP)) {
            for (state in installed) {
                ManualPackageRow(
                    state = state,
                    label = AppLabelResolver.labelFor(context, state.packageName),
                    onOpenAppInfo = { openAppInfo(context, state.packageName) }
                )
            }
        }
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = stringResource(R.string.unlock_manual_system_caption),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ManualPackageRow(state: PackageState, label: String, onOpenAppInfo: () -> Unit) {
    val statusRes = if (state.isFreed) R.string.unlock_package_freed else R.string.unlock_package_active
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(ROW_GAP))
            Text(
                text = stringResource(statusRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(ROW_GAP))
        NothingButton(
            text = stringResource(R.string.unlock_manual_open_app_info),
            onClick = onOpenAppInfo,
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdbPathCard() {
    val context = LocalContext.current
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.unlock_adb_label))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        CommandBlock(
            hintRes = R.string.unlock_adb_free_hint,
            commands = adbCommandsFor(UnlockCommands::disable),
            onCopy = { text -> copyToClipboard(context, text) }
        )
        Spacer(modifier = Modifier.height(COMMAND_BLOCK_GAP))
        CommandBlock(
            hintRes = R.string.unlock_adb_restore_hint,
            commands = adbCommandsFor(UnlockCommands::enable),
            onCopy = { text -> copyToClipboard(context, text) }
        )
    }
}

@Composable
private fun CommandBlock(hintRes: Int, commands: String, onCopy: (String) -> Unit) {
    Text(
        text = stringResource(hintRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(ROW_GAP))
    Text(
        text = commands,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    )
    Spacer(modifier = Modifier.height(ROW_GAP))
    NothingButton(
        text = stringResource(R.string.unlock_copy),
        onClick = { onCopy(commands) },
        outlined = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Builds the full multi-line `adb shell …` block for every candidate consumer package. */
private fun adbCommandsFor(commandFor: (String) -> List<String>): String =
    NothingConsumerPackages.CANDIDATES.joinToString(separator = "\n") { packageName ->
        "adb shell " + commandFor(packageName).joinToString(separator = " ")
    }

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard != null) {
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, text))
        Toast.makeText(context, R.string.unlock_copied, Toast.LENGTH_SHORT).show()
    }
}

private fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (error: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.unlock_manual_open_failed, Toast.LENGTH_LONG).show()
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (error: android.content.ActivityNotFoundException) {
        Toast.makeText(context, url, Toast.LENGTH_LONG).show()
    }
}
