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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.core.KeyGesture
import dev.koukeneko.essentialkeytools.service.KeyEventStream
import dev.koukeneko.essentialkeytools.service.ObservedGesture
import dev.koukeneko.essentialkeytools.service.ObservedKeyEvent
import dev.koukeneko.essentialkeytools.ui.UiLabels
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import kotlinx.coroutines.flow.collectLatest

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val LOG_ROW_GAP = 6.dp

// The live log is a verification harness, not history: keep only the most recent entries.
private const val MAX_LOG_ENTRIES = 20

/**
 * On-device verification harness: shows matching key events and the gestures the classifier emits
 * for the learned Essential Key, plus an entry point into the action picker for each gesture.
 */
@Composable
fun KeyTestScreen(
    onEditGesture: (KeyGesture) -> Unit,
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val recentEvents = remember { mutableStateListOf<ObservedKeyEvent>() }
    val recentGestures = remember { mutableStateListOf<ObservedGesture>() }

    DisposableEffect(Unit) {
        KeyEventStream.actionExecutionSuppressed = true
        onDispose { KeyEventStream.actionExecutionSuppressed = false }
    }

    LaunchedEffect(Unit) {
        KeyEventStream.rawKeyEvents.collectLatest { event ->
            prependCapped(recentEvents, event)
        }
    }
    LaunchedEffect(Unit) {
        KeyEventStream.classifiedGestures.collectLatest { gesture ->
            prependCapped(recentGestures, gesture)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.test_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))

        GestureMappingCard(onEditGesture = onEditGesture)
        Spacer(modifier = Modifier.height(CARD_GAP))
        GestureLogCard(recentGestures = recentGestures)
        Spacer(modifier = Modifier.height(CARD_GAP))
        EventLogCard(recentEvents = recentEvents)
    }
}

private fun <T> prependCapped(target: MutableList<T>, item: T) {
    target.add(0, item)
    while (target.size > MAX_LOG_ENTRIES) {
        target.removeAt(target.size - 1)
    }
}

@Composable
private fun GestureMappingCard(onEditGesture: (KeyGesture) -> Unit) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.test_edit_mapping))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(LOG_ROW_GAP)) {
            for (gesture in KeyGesture.entries) {
                NothingButton(
                    text = stringResource(UiLabels.gestureLabelRes(gesture)),
                    onClick = { onEditGesture(gesture) },
                    outlined = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GestureLogCard(recentGestures: List<ObservedGesture>) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.test_section_gestures))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        if (recentGestures.isEmpty()) {
            EmptyLogText()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(LOG_ROW_GAP)) {
                items(recentGestures) { observed ->
                    Text(
                        text = stringResource(UiLabels.gestureLabelRes(observed.gesture)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun EventLogCard(recentEvents: List<ObservedKeyEvent>) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.test_section_events))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        if (recentEvents.isEmpty()) {
            EmptyLogText()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(LOG_ROW_GAP)) {
                items(recentEvents) { event -> EventLogRow(event) }
            }
        }
    }
}

@Composable
private fun EventLogRow(event: ObservedKeyEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.setup_scan_code) + " " + event.scanCode,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.setup_key_code) + " " + event.keyCode,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLogText() {
    Text(
        text = stringResource(R.string.test_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
