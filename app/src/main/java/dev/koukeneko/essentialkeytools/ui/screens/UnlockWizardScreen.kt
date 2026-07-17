package dev.koukeneko.essentialkeytools.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.unlock.PackageState
import dev.koukeneko.essentialkeytools.unlock.UnlockerFactory
import dev.koukeneko.essentialkeytools.ui.AppLabelResolver
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val ROW_GAP = 8.dp
private val BUTTON_GAP = 12.dp

/**
 * Guides the user through freeing the Essential Key's single press. Presents the live consumer-
 * package state plus a manual on-device path for disabling or restoring each consumer. All state is
 * re-read on resume so drift caused by an OS update is reflected.
 */
@Composable
fun UnlockWizardScreen(
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val unlocker = remember { UnlockerFactory.create(context) }

    var packageStates by remember { mutableStateOf(unlocker.readPackageStates()) }

    fun refresh() {
        packageStates = unlocker.readPackageStates()
    }

    RefreshOnResume(::refresh)

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
        ManualPathCard(states = packageStates)
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

/**
 * The manual path: for each installed consumer package it shows the package label, its current
 * freed/active state, and a button into that package's App Info where the user can tap Disable.
 * States come from the same list the wizard re-reads on resume, so returning from Settings refreshes
 * each row.
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

private fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (error: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.unlock_manual_open_failed, Toast.LENGTH_LONG).show()
    }
}
