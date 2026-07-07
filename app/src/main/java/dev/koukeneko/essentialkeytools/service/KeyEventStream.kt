package dev.koukeneko.essentialkeytools.service

import dev.koukeneko.essentialkeytools.core.KeyGesture
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A raw key event observed by the service, flattened to primitives for the UI to display. */
data class ObservedKeyEvent(
    val keyCode: Int,
    val scanCode: Int,
    val action: Int,
    val timestampMs: Long
)

/** A gesture the service classified for the learned Essential Key, surfaced to the test screen. */
data class ObservedGesture(
    val gesture: KeyGesture,
    val timestampMs: Long
)

/**
 * In-process bridge from the accessibility service to the UI. The service publishes every raw key
 * event and every classified gesture; screens collect the streams. A process-wide singleton is used
 * because the service and the activity live in the same process but cannot pass references directly.
 *
 * [detectionModeActive] is set by the learning screen. While true the service emits ALL key events
 * (so the user can capture the Essential Key's scanCode) and suppresses action execution.
 */
object KeyEventStream {

    // replay = 0: screens only care about events observed while they are collecting.
    private val rawEvents = MutableSharedFlow<ObservedKeyEvent>(extraBufferCapacity = EVENT_BUFFER)
    private val gestures = MutableSharedFlow<ObservedGesture>(extraBufferCapacity = EVENT_BUFFER)

    val rawKeyEvents: SharedFlow<ObservedKeyEvent> = rawEvents.asSharedFlow()
    val classifiedGestures: SharedFlow<ObservedGesture> = gestures.asSharedFlow()

    @Volatile
    var detectionModeActive: Boolean = false

    fun publishRawEvent(event: ObservedKeyEvent) {
        rawEvents.tryEmit(event)
    }

    fun publishGesture(gesture: ObservedGesture) {
        gestures.tryEmit(gesture)
    }

    private const val EVENT_BUFFER = 32
}
