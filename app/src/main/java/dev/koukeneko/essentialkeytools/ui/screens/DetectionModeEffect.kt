package dev.koukeneko.essentialkeytools.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import dev.koukeneko.essentialkeytools.service.KeyEventStream
import dev.koukeneko.essentialkeytools.service.ObservedKeyEvent
import kotlinx.coroutines.flow.collectLatest

/**
 * Turns the service's detection (learning) mode on for as long as the calling screen is composed,
 * and delivers each raw key event to [onEvent]. Detection mode makes the service stream ALL key
 * events and suppress action execution, so it must be scoped tightly to the setup screen.
 */
@Composable
fun DetectionModeEffect(onEvent: (ObservedKeyEvent) -> Unit) {
    DisposableEffect(Unit) {
        KeyEventStream.detectionModeActive = true
        onDispose { KeyEventStream.detectionModeActive = false }
    }
    LaunchedEffect(Unit) {
        KeyEventStream.rawKeyEvents.collectLatest { event -> onEvent(event) }
    }
}
