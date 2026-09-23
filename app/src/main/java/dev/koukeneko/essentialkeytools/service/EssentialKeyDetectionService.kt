package dev.koukeneko.essentialkeytools.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dev.koukeneko.essentialkeytools.actions.ActionExecutor
import dev.koukeneko.essentialkeytools.actions.KeyHaptics
import dev.koukeneko.essentialkeytools.core.KeyGesture
import dev.koukeneko.essentialkeytools.core.KeyGestureClassifier
import dev.koukeneko.essentialkeytools.settings.DEFAULT_ESSENTIAL_KEY_SCAN_CODE
import dev.koukeneko.essentialkeytools.settings.GestureActionMap
import dev.koukeneko.essentialkeytools.settings.HapticStrength
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Observes hardware key events, isolates the Essential Key by its learned scanCode, classifies the
 * gesture and runs the mapped action. Consuming the event does NOT stop Nothing OS's own single
 * press handling (that needs the separate unlock flow); this service only observes and reacts.
 *
 * While the UI's detection screen is active it instead publishes every key event to
 * [KeyEventStream] so the user can capture the Essential Key's scanCode, and suppresses actions.
 */
class EssentialKeyDetectionService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var actionExecutor: ActionExecutor
    private lateinit var keyHaptics: KeyHaptics

    // Rebuilt whenever settings change so the classifier reflects the current active gestures.
    private var classifier: KeyGestureClassifier? = null
    private var learnedScanCode = DEFAULT_ESSENTIAL_KEY_SCAN_CODE
    private var gestureActionMap = GestureActionMap.EMPTY
    private var hapticStrength = HapticStrength.OFF
    private var suppressNextClassifiedAction = false
    // Tracks which suppression state the current classifier was built for, so a new press can pick
    // up a screen transition without disturbing a multi-tap sequence already in flight.
    private var classifierBuiltWhileSuppressed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        actionExecutor = ActionExecutor(context = this, accessibilityService = this)
        keyHaptics = KeyHaptics(this)
        collectSettings()
    }

    private fun collectSettings() {
        val repository = SettingsRepository.getInstance(this)
        serviceScope.launch {
            combine(
                repository.essentialKeyScanCode,
                repository.gestureActionMap
            ) { scanCode, actionMap -> scanCode to actionMap }
                .collect { (scanCode, actionMap) ->
                    learnedScanCode = scanCode
                    gestureActionMap = actionMap
                    rebuildClassifier(actionMap)
                }
        }
        serviceScope.launch {
            repository.hapticStrength.collect { strength -> hapticStrength = strength }
        }
    }

    private fun rebuildClassifier(actionMap: GestureActionMap) {
        classifier?.reset()
        suppressNextClassifiedAction = false
        classifierBuiltWhileSuppressed = KeyEventStream.actionExecutionSuppressed
        classifier = KeyGestureClassifier(
            enabledGestures = enabledGesturesFor(actionMap),
            scheduler = HandlerGestureScheduler(mainHandler),
            onGesture = ::onGestureClassified,
            onPress = ::onPress
        )
    }

    /**
     * Only the gestures with a real mapping need recognising; this lets the classifier resolve
     * simpler gestures without waiting for the multi-tap window. The key-test screen suppresses
     * action execution precisely so it can show the raw gesture the user pressed, so it needs every
     * gesture recognised regardless of what is currently mapped.
     */
    private fun enabledGesturesFor(actionMap: GestureActionMap): Set<KeyGesture> =
        if (KeyEventStream.actionExecutionSuppressed) {
            KeyGesture.entries.toSet()
        } else {
            actionMap.activeGestures()
        }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (KeyEventStream.detectionModeActive) {
            publishForLearning(event)
            // Do not consume: let the system keep behaving normally while learning.
            return false
        }
        return handleOperationalEvent(event)
    }

    private fun publishForLearning(event: KeyEvent) {
        if (!KeyEventFilter.isLearnableCandidate(event.keyCode)) {
            return
        }
        KeyEventStream.publishRawEvent(
            ObservedKeyEvent(
                keyCode = event.keyCode,
                scanCode = event.scanCode,
                action = event.action,
                timestampMs = event.eventTime
            )
        )
    }

    private fun handleOperationalEvent(event: KeyEvent): Boolean {
        if (!KeyEventFilter.matchesLearnedKey(event.scanCode, learnedScanCode)) {
            return false
        }
        // Entering or leaving the key-test screen changes which gestures the classifier should
        // recognise. Only apply that at the start of a new press so an in-flight multi-tap sequence
        // is never disturbed mid-way through.
        if (event.action == KeyEvent.ACTION_DOWN &&
            classifierBuiltWhileSuppressed != KeyEventStream.actionExecutionSuppressed
        ) {
            rebuildClassifier(gestureActionMap)
        }
        // Remember that this gesture began on the test screen. Some gestures resolve after a short
        // timeout, when the user may already have navigated away and cleared the screen-wide flag.
        if (KeyEventStream.actionExecutionSuppressed) {
            suppressNextClassifiedAction = true
        }
        // Mirror matching events to the stream so the key-test screen can log them live.
        KeyEventStream.publishRawEvent(
            ObservedKeyEvent(
                keyCode = event.keyCode,
                scanCode = event.scanCode,
                action = event.action,
                timestampMs = event.eventTime
            )
        )
        feedClassifier(event)
        return true
    }

    private fun feedClassifier(event: KeyEvent) {
        val activeClassifier = classifier ?: return
        when (event.action) {
            KeyEvent.ACTION_DOWN -> activeClassifier.onKeyDown(event.downTime)
            KeyEvent.ACTION_UP -> activeClassifier.onKeyUp(event.downTime, event.eventTime)
        }
    }

    // Like a physical button, feedback confirms the press itself rather than waiting for the
    // gesture to resolve. Presses are ignored while nothing is mapped, since they do nothing.
    private fun onPress() {
        if (gestureActionMap.activeGestures().isNotEmpty()) {
            keyHaptics.perform(hapticStrength)
        }
    }

    private fun onGestureClassified(gesture: KeyGesture) {
        KeyEventStream.publishGesture(
            ObservedGesture(gesture = gesture, timestampMs = System.currentTimeMillis())
        )
        val actionSuppressed = KeyEventStream.actionExecutionSuppressed || suppressNextClassifiedAction
        suppressNextClassifiedAction = false
        if (!actionSuppressed) {
            actionExecutor.execute(gestureActionMap.actionFor(gesture))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This service filters key events only; window/content events are irrelevant.
    }

    override fun onInterrupt() {
        classifier?.reset()
        suppressNextClassifiedAction = false
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    private fun tearDown() {
        isRunning = false
        classifier?.reset()
        suppressNextClassifiedAction = false
        mainHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
    }

    companion object {
        /**
         * Whether the service is connected and running. The home screen reads this to show the
         * live status without an IPC round-trip; it is set on connect and cleared on teardown.
         */
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
