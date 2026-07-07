package dev.koukeneko.essentialkeytools.settings

import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.core.KeyGesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureActionMapTest {

    @Test
    fun emptyMap_returnsNoneForEveryGesture() {
        val map = GestureActionMap.EMPTY

        for (gesture in KeyGesture.entries) {
            assertEquals(KeyAction.None, map.actionFor(gesture))
        }
    }

    @Test
    fun with_setsActionForGestureOnly() {
        val map = GestureActionMap.EMPTY.with(KeyGesture.DOUBLE_PRESS, KeyAction.TakeScreenshot)

        assertEquals(KeyAction.TakeScreenshot, map.actionFor(KeyGesture.DOUBLE_PRESS))
        assertEquals(KeyAction.None, map.actionFor(KeyGesture.SINGLE_PRESS))
    }

    @Test
    fun activeGestures_excludesNoneMappings() {
        val map = GestureActionMap.EMPTY
            .with(KeyGesture.SINGLE_PRESS, KeyAction.LockScreen)
            .with(KeyGesture.LONG_PRESS, KeyAction.MediaPlayPause)

        val active = map.activeGestures()

        assertEquals(setOf(KeyGesture.SINGLE_PRESS, KeyGesture.LONG_PRESS), active)
    }

    @Test
    fun with_overwritesPreviousActionForSameGesture() {
        val map = GestureActionMap.EMPTY
            .with(KeyGesture.SINGLE_PRESS, KeyAction.LockScreen)
            .with(KeyGesture.SINGLE_PRESS, KeyAction.None)

        assertTrue(map.activeGestures().isEmpty())
    }
}
