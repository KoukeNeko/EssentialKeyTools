package dev.koukeneko.essentialkeytools.ui

import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.core.KeyGesture

/**
 * Single source of truth mapping domain values to their string resources, so screens never
 * hard-code a `when` over gestures/actions more than once (DRY).
 */
object UiLabels {

    fun gestureLabelRes(gesture: KeyGesture): Int = when (gesture) {
        KeyGesture.SINGLE_PRESS -> R.string.gesture_single_press
        KeyGesture.DOUBLE_PRESS -> R.string.gesture_double_press
        KeyGesture.TRIPLE_PRESS -> R.string.gesture_triple_press
        KeyGesture.LONG_PRESS -> R.string.gesture_long_press
    }

    /**
     * The label resource for a built-in action id. [KeyAction.LaunchApp] is deliberately omitted:
     * a launch action shows the resolved app name, which is not a static resource.
     */
    fun builtInActionLabelRes(actionId: String): Int = when (actionId) {
        KeyAction.None.ID -> R.string.action_none
        KeyAction.ToggleFlashlight.ID -> R.string.action_toggle_flashlight
        KeyAction.TakeScreenshot.ID -> R.string.action_take_screenshot
        KeyAction.LockScreen.ID -> R.string.action_lock_screen
        KeyAction.MediaPlayPause.ID -> R.string.action_media_play_pause
        KeyAction.RingerCycle.ID -> R.string.action_ringer_cycle
        else -> R.string.action_none
    }

    /** The built-in (non-app) actions offered in the picker, in display order. */
    val builtInActions: List<KeyAction> = listOf(
        KeyAction.None,
        KeyAction.ToggleFlashlight,
        KeyAction.TakeScreenshot,
        KeyAction.LockScreen,
        KeyAction.MediaPlayPause,
        KeyAction.RingerCycle
    )
}
