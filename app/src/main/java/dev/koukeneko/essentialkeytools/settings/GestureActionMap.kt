package dev.koukeneko.essentialkeytools.settings

import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.core.KeyGesture

/**
 * The full gesture -> action configuration: one [KeyAction] per [KeyGesture]. Missing entries
 * resolve to [KeyAction.None] so callers never deal with nullability.
 *
 * This is a plain immutable value with no Android or DataStore dependency, so the persistence
 * mapping in [SettingsSerialization] can be unit-tested in isolation.
 */
data class GestureActionMap(
    private val actionsByGesture: Map<KeyGesture, KeyAction> = emptyMap()
) {
    fun actionFor(gesture: KeyGesture): KeyAction =
        actionsByGesture[gesture] ?: KeyAction.None

    fun with(gesture: KeyGesture, action: KeyAction): GestureActionMap =
        GestureActionMap(actionsByGesture + (gesture to action))

    /** Gestures whose mapped action is anything other than [KeyAction.None]. */
    fun activeGestures(): Set<KeyGesture> =
        KeyGesture.entries
            .filter { gesture -> actionFor(gesture) != KeyAction.None }
            .toSet()

    companion object {
        val EMPTY = GestureActionMap()
    }
}
