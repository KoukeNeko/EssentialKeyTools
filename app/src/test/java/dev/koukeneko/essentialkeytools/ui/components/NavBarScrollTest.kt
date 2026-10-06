package dev.koukeneko.essentialkeytools.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavBarScrollTest {

    private fun scroll() = NavBarScroll(thresholdPx = 10f)

    @Test
    fun startsFullSize() {
        assertFalse(scroll().collapsed)
    }

    @Test
    fun scrollingTowardTheEndPastTheThresholdShrinksTheBar() {
        val scroll = scroll()

        scroll.onScrolled(6f)
        assertFalse(scroll.collapsed)
        scroll.onScrolled(6f)
        assertTrue(scroll.collapsed)
    }

    @Test
    fun scrollingBackPastTheThresholdGrowsItAgain() {
        val scroll = scroll()
        scroll.onScrolled(20f)

        scroll.onScrolled(-6f)
        assertTrue(scroll.collapsed)
        scroll.onScrolled(-6f)
        assertFalse(scroll.collapsed)
    }

    @Test
    fun aChangeOfDirectionRestartsTheCount() {
        val scroll = scroll()

        scroll.onScrolled(8f)
        scroll.onScrolled(-3f)
        scroll.onScrolled(8f)

        // Only 8 since the last change of direction, so a wobble does not shrink the bar.
        assertFalse(scroll.collapsed)
    }

    @Test
    fun expandingClearsWhatWasCounted() {
        val scroll = scroll()
        scroll.onScrolled(8f)

        scroll.expand()
        scroll.onScrolled(8f)

        assertFalse(scroll.collapsed)
    }

    @Test
    fun expandingGrowsAShrunkBar() {
        val scroll = scroll()
        scroll.onScrolled(20f)

        scroll.expand()

        assertFalse(scroll.collapsed)
    }

    @Test
    fun theContentScrollingTowardItsEndArrivesAsNegativeDeltas() {
        val scroll = scroll()

        scroll.connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)

        assertTrue(scroll.collapsed)
    }

    @Test
    fun onlyTheUsersOwnScrollingCounts() {
        val scroll = scroll()

        scroll.connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.SideEffect)

        assertFalse(scroll.collapsed)
    }

    @Test
    fun watchingTheScrollNeverConsumesAnyOfIt() {
        val consumed = scroll().connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)

        assertTrue(consumed == Offset.Zero)
    }
}
