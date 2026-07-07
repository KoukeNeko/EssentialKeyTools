package dev.koukeneko.essentialkeytools.settings

import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.core.KeyGesture

/**
 * Pure translation between a [GestureActionMap] and the flat string keys DataStore stores. Kept
 * free of Android/DataStore types so the round-trip mapping is unit-testable on the JVM.
 *
 * Each gesture persists two entries: an action id and an optional payload, under deterministic
 * preference keys derived from the gesture name.
 */
object SettingsSerialization {

    private const val ACTION_ID_SUFFIX = "_action_id"
    private const val ACTION_PAYLOAD_SUFFIX = "_action_payload"

    fun actionIdKeyName(gesture: KeyGesture): String =
        gesture.name.lowercase() + ACTION_ID_SUFFIX

    fun actionPayloadKeyName(gesture: KeyGesture): String =
        gesture.name.lowercase() + ACTION_PAYLOAD_SUFFIX

    /**
     * Rebuilds the map from a lookup of persisted string values (e.g. DataStore preferences).
     * [readValue] returns null for absent keys, which resolves to [KeyAction.None].
     */
    fun decodeMap(readValue: (String) -> String?): GestureActionMap {
        var map = GestureActionMap.EMPTY
        for (gesture in KeyGesture.entries) {
            val id = readValue(actionIdKeyName(gesture)) ?: KeyAction.None.id
            val payload = readValue(actionPayloadKeyName(gesture))
            map = map.with(gesture, KeyAction.fromPersisted(id, payload))
        }
        return map
    }

    /**
     * The persisted (id, payload) pair for a single gesture's action. Payload is null when the
     * action carries none; callers should clear the payload key in that case.
     */
    fun encodeAction(action: KeyAction): PersistedAction =
        PersistedAction(id = action.id, payload = KeyAction.payloadOf(action))

    data class PersistedAction(val id: String, val payload: String?)
}
