package dev.koukeneko.essentialkeytools.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.actions.KeyHaptics
import dev.koukeneko.essentialkeytools.settings.HapticPattern
import dev.koukeneko.essentialkeytools.settings.HapticStrength
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import kotlinx.coroutines.launch

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val OPTION_ROW_GAP = 4.dp
private val RADIO_TO_TEXT_GAP = 12.dp

/**
 * The app's preferences: haptic feedback and language. Choices come from [SettingsRepository]
 * reactively, so each card reflects a change the moment it is made.
 */
@Composable
fun SettingsScreen(
    onEditHapticPattern: () -> Unit,
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
        LanguageCard()
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
        val onActionOnlyEnabled = selected != HapticStrength.OFF
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = onActionOnly,
                    enabled = onActionOnlyEnabled,
                    role = Role.Checkbox,
                    onValueChange = onActionOnlyChange
                )
                .padding(vertical = OPTION_ROW_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = onActionOnly,
                onCheckedChange = null,
                enabled = onActionOnlyEnabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.tertiary
                )
            )
            Spacer(modifier = Modifier.width(RADIO_TO_TEXT_GAP))
            Text(
                text = stringResource(R.string.haptics_on_action_only),
                style = MaterialTheme.typography.labelLarge,
                color = if (onActionOnlyEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun RadioOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = OPTION_ROW_GAP),
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
