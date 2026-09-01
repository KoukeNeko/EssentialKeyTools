package dev.koukeneko.essentialkeytools.ui

import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.actions.KeyAction
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Resource ids are plain ints, so the label mapping is checkable on the JVM without a device.
 */
class UiLabelsTest {

    @Test
    fun everyOfferedActionHasItsOwnLabel() {
        val actionsNeedingLabel =
            UiLabels.builtInActions.filter { action -> action != KeyAction.None }

        for (action in actionsNeedingLabel) {
            // builtInActionLabelRes falls back to action_none for ids it does not know, which would
            // render a second "None" row in the picker instead of failing loudly.
            assertNotEquals(
                "${action.id} has no label of its own",
                R.string.action_none,
                UiLabels.builtInActionLabelRes(action.id)
            )
        }
    }

    @Test
    fun offersEveryMediaAction() {
        val offeredIds = UiLabels.builtInActions.map { action -> action.id }

        assertTrue(
            offeredIds.containsAll(
                listOf(
                    KeyAction.MediaPlayPause.ID,
                    KeyAction.MediaNext.ID,
                    KeyAction.MediaPrevious.ID
                )
            )
        )
    }
}
