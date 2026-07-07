package dev.koukeneko.essentialkeytools.settings

import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.core.KeyGesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsSerializationTest {

    // Simulates the DataStore key/value store the real repository reads from.
    private fun storeOf(vararg entries: Pair<String, String>): (String) -> String? {
        val map = entries.toMap()
        return { keyName -> map[keyName] }
    }

    @Test
    fun decodeMap_absentKeys_resolveToNone() {
        val map = SettingsSerialization.decodeMap { null }

        for (gesture in KeyGesture.entries) {
            assertEquals(KeyAction.None, map.actionFor(gesture))
        }
    }

    @Test
    fun decodeMap_readsBuiltInAction() {
        val idKey = SettingsSerialization.actionIdKeyName(KeyGesture.DOUBLE_PRESS)
        val map = SettingsSerialization.decodeMap(
            storeOf(idKey to KeyAction.ToggleFlashlight.ID)
        )

        assertEquals(KeyAction.ToggleFlashlight, map.actionFor(KeyGesture.DOUBLE_PRESS))
    }

    @Test
    fun decodeMap_readsLaunchAppWithPayload() {
        val idKey = SettingsSerialization.actionIdKeyName(KeyGesture.SINGLE_PRESS)
        val payloadKey = SettingsSerialization.actionPayloadKeyName(KeyGesture.SINGLE_PRESS)
        val map = SettingsSerialization.decodeMap(
            storeOf(
                idKey to KeyAction.LaunchApp.ID,
                payloadKey to "com.example.app"
            )
        )

        assertEquals(KeyAction.LaunchApp("com.example.app"), map.actionFor(KeyGesture.SINGLE_PRESS))
    }

    @Test
    fun decodeMap_launchAppWithoutPayload_fallsBackToNone() {
        val idKey = SettingsSerialization.actionIdKeyName(KeyGesture.LONG_PRESS)
        val map = SettingsSerialization.decodeMap(
            storeOf(idKey to KeyAction.LaunchApp.ID)
        )

        assertEquals(KeyAction.None, map.actionFor(KeyGesture.LONG_PRESS))
    }

    @Test
    fun decodeMap_unknownActionId_fallsBackToNone() {
        val idKey = SettingsSerialization.actionIdKeyName(KeyGesture.TRIPLE_PRESS)
        val map = SettingsSerialization.decodeMap(
            storeOf(idKey to "removed_action_from_old_version")
        )

        assertEquals(KeyAction.None, map.actionFor(KeyGesture.TRIPLE_PRESS))
    }

    @Test
    fun encodeAction_launchApp_carriesPackageNamePayload() {
        val encoded = SettingsSerialization.encodeAction(KeyAction.LaunchApp("com.foo.bar"))

        assertEquals(KeyAction.LaunchApp.ID, encoded.id)
        assertEquals("com.foo.bar", encoded.payload)
    }

    @Test
    fun encodeAction_builtIn_hasNoPayload() {
        val encoded = SettingsSerialization.encodeAction(KeyAction.LockScreen)

        assertEquals(KeyAction.LockScreen.ID, encoded.id)
        assertNull(encoded.payload)
    }

    @Test
    fun encodeThenDecode_roundTripsLaunchApp() {
        val gesture = KeyGesture.SINGLE_PRESS
        val original = KeyAction.LaunchApp("com.round.trip")
        val encoded = SettingsSerialization.encodeAction(original)

        val decoded = SettingsSerialization.decodeMap(
            storeOf(
                SettingsSerialization.actionIdKeyName(gesture) to encoded.id,
                SettingsSerialization.actionPayloadKeyName(gesture) to encoded.payload.orEmpty()
            )
        )

        assertEquals(original, decoded.actionFor(gesture))
    }
}
