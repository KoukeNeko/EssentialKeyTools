package dev.koukeneko.essentialkeytools.actions

/**
 * A thing the app can do in response to a classified key gesture. Each variant is a pure data
 * description of intent; running it is [ActionExecutor]'s job. Keeping the model free of Android
 * types lets it be serialised for DataStore and unit-tested on the JVM.
 */
sealed interface KeyAction {

    /** Stable identifier persisted in settings. Payload (if any) is stored separately. */
    val id: String

    /** Does nothing. The default mapping for every gesture. */
    data object None : KeyAction {
        override val id: String = ID
        const val ID: String = "none"
    }

    /** Launches an installed app by its package name. */
    data class LaunchApp(val packageName: String) : KeyAction {
        override val id: String = ID

        companion object {
            const val ID: String = "launch_app"
        }
    }

    /** Toggles the device flashlight (back-camera torch). */
    data object ToggleFlashlight : KeyAction {
        override val id: String = ID
        const val ID: String = "toggle_flashlight"
    }

    /** Takes a screenshot via the accessibility global action. */
    data object TakeScreenshot : KeyAction {
        override val id: String = ID
        const val ID: String = "take_screenshot"
    }

    /** Locks the screen via the accessibility global action. */
    data object LockScreen : KeyAction {
        override val id: String = ID
        const val ID: String = "lock_screen"
    }

    /** Sends a play/pause media key to the active media session. */
    data object MediaPlayPause : KeyAction {
        override val id: String = ID
        const val ID: String = "media_play_pause"
    }

    /** Cycles the ringer mode: normal -> vibrate -> silent -> normal. */
    data object RingerCycle : KeyAction {
        override val id: String = ID
        const val ID: String = "ringer_cycle"
    }

    companion object {
        /**
         * Rebuilds a [KeyAction] from its persisted [id] and optional [payload]. Unknown ids fall
         * back to [None] so a settings schema change can never crash the reader.
         */
        fun fromPersisted(id: String, payload: String?): KeyAction = when (id) {
            None.ID -> None
            LaunchApp.ID -> if (payload.isNullOrEmpty()) None else LaunchApp(payload)
            ToggleFlashlight.ID -> ToggleFlashlight
            TakeScreenshot.ID -> TakeScreenshot
            LockScreen.ID -> LockScreen
            MediaPlayPause.ID -> MediaPlayPause
            RingerCycle.ID -> RingerCycle
            else -> None
        }

        /** The optional payload to persist alongside a [KeyAction]'s id, or null if it has none. */
        fun payloadOf(action: KeyAction): String? = when (action) {
            is LaunchApp -> action.packageName
            else -> null
        }
    }
}
