package dev.koukeneko.essentialkeytools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.service.ObservedKeyEvent
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val ROW_GAP = 8.dp

/**
 * "Press your Essential Key" learning flow. Turns on detection mode so the service streams every
 * key press, shows the most recent captured event, and lets the user save its scanCode.
 */
@Composable
fun KeySetupScreen(
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var capturedEvent by remember { mutableStateOf<ObservedKeyEvent?>(null) }
    var savedScanCode by remember { mutableStateOf<Int?>(null) }
    var justSaved by remember { mutableStateOf(false) }

    // Detection mode must be on only while this screen is visible; toggle it around the effect.
    DetectionModeEffect { event ->
        capturedEvent = event
        justSaved = false
    }

    LaunchedEffect(Unit) {
        repository.essentialKeyScanCode.collectLatest { scanCode -> savedScanCode = scanCode }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))

        PromptCard(capturedEvent = capturedEvent)
        Spacer(modifier = Modifier.height(CARD_GAP))
        SavedScanCodeCard(savedScanCode = savedScanCode, justSaved = justSaved)
        Spacer(modifier = Modifier.height(CARD_GAP))

        val captured = capturedEvent
        if (captured != null) {
            NothingButton(
                text = stringResource(R.string.setup_confirm),
                onClick = {
                    coroutineScope.launch {
                        repository.setEssentialKeyScanCode(captured.scanCode)
                        justSaved = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PromptCard(capturedEvent: ObservedKeyEvent?) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.setup_prompt))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        if (capturedEvent == null) {
            Text(
                text = stringResource(R.string.setup_waiting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CapturedEventRows(capturedEvent)
        }
    }
}

@Composable
private fun CapturedEventRows(event: ObservedKeyEvent) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        LabelValueRow(
            label = stringResource(R.string.setup_key_code),
            value = event.keyCode.toString()
        )
        LabelValueRow(
            label = stringResource(R.string.setup_scan_code),
            value = event.scanCode.toString()
        )
    }
}

@Composable
private fun SavedScanCodeCard(savedScanCode: Int?, justSaved: Boolean) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.setup_current_scan_code))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = savedScanCode?.toString() ?: "—",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (justSaved) {
            Spacer(modifier = Modifier.height(LABEL_GAP))
            Text(
                text = stringResource(R.string.setup_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
