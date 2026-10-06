package dev.koukeneko.essentialkeytools.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingNavBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private val items = listOf(
        NavBarItem(R.string.nav_home, R.drawable.ic_nav_home),
        NavBarItem(R.string.action_key_setup, R.drawable.ic_nav_setup),
        NavBarItem(R.string.action_key_test, R.drawable.ic_nav_test),
        NavBarItem(R.string.action_open_settings, R.drawable.ic_nav_settings)
    )

    @Test
    fun everyDestinationIsATabAndOnlyTheSelectedOneIsSelected() {
        showBar(selectedIndex = 1)

        val tabs = composeRule.onAllNodes(isSelectable())
        tabs.assertCountEquals(items.size)
        tabs.assertAll(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        tabs[0].assertIsNotSelected()
        tabs[1].assertIsSelected()
        tabs[2].assertIsNotSelected()
        tabs[3].assertIsNotSelected()
    }

    @Test
    fun everyIconOnlyTabIsNamedForScreenReaders() {
        showBar(selectedIndex = 2)

        for (item in items) {
            composeRule.onNodeWithContentDescription(resources.getString(item.labelRes)).assertExists()
        }
    }

    @Test
    fun everyTabIsAtLeastAFingertipWide() {
        showBar(selectedIndex = 0)

        val tabs = composeRule.onAllNodes(isSelectable())
        for (index in items.indices) {
            tabs[index].assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun tappingATabReportsItsIndex() {
        val selected = mutableListOf<Int>()
        showBar(selectedIndex = 0, onSelect = { selected.add(it) })

        composeRule.onAllNodes(isSelectable())[3].performClick()

        composeRule.runOnIdle { assertEquals(listOf(3), selected) }
    }

    @Test
    fun screenReadersCanActivateATabThroughItsClickAction() {
        val selected = mutableListOf<Int>()
        showBar(selectedIndex = 0, onSelect = { selected.add(it) })

        composeRule.onAllNodes(isSelectable())[2].performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle { assertEquals(listOf(2), selected) }
    }

    @Test
    fun draggingAcrossTheBarPicksTheTabUnderTheFingerWhenItLifts() {
        val selected = mutableListOf<Int>()
        showBar(selectedIndex = 0, onSelect = { selected.add(it) })
        val tabs = composeRule.onAllNodes(isSelectable())
        val distance = tabs[2].fetchSemanticsNode().boundsInRoot.center.x -
            tabs[0].fetchSemanticsNode().boundsInRoot.center.x

        tabs[0].performTouchInput {
            down(center)
            moveBy(Offset(distance / 2, 0f))
            moveBy(Offset(distance / 2, 0f))
            up()
        }

        // Only the tab under the finger on release is picked, not the ones passed over on the way.
        composeRule.runOnIdle { assertEquals(listOf(2), selected) }
    }

    @Test
    fun releasingFarAboveTheBarCancelsThePress() {
        val selected = mutableListOf<Int>()
        showBar(selectedIndex = 0, onSelect = { selected.add(it) })

        composeRule.onAllNodes(isSelectable())[2].performTouchInput {
            down(center)
            moveBy(Offset(0f, -1000f))
            up()
        }

        composeRule.runOnIdle { assertEquals(emptyList<Int>(), selected) }
    }

    private fun showBar(selectedIndex: Int, onSelect: (Int) -> Unit = {}) {
        composeRule.setContent {
            EssentialKeyToolsTheme {
                FloatingNavBar(
                    items = items,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                    backdrop = rememberNavBarBackdrop()
                )
            }
        }
    }
}
