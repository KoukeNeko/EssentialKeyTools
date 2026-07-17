package dev.koukeneko.essentialkeytools.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityDisclosureDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun disclosure_showsFullCopyAndBothChoices() {
        showDialog()

        composeRule.onNodeWithText(resources.getString(R.string.a11y_disclosure_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(resources.getString(R.string.a11y_disclosure_body))
            .assertIsDisplayed()
        composeRule.onNodeWithText(resources.getString(R.string.a11y_disclosure_question))
            .assertIsDisplayed()
        composeRule.onNodeWithText(buttonText(R.string.a11y_disclosure_use_accessibility))
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            buttonText(R.string.a11y_disclosure_continue_without_accessibility)
        )
            .assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun continueWithoutAccessibilityOnlyInvokesThatChoiceOnce() {
        val continuedWithoutAccessibility = AtomicInteger()
        val usedAccessibility = AtomicInteger()
        showDialog(
            onContinueWithoutAccessibility = { continuedWithoutAccessibility.incrementAndGet() },
            onUseAccessibility = { usedAccessibility.incrementAndGet() },
            hideOnDecision = false
        )

        val continueButton = composeRule.onNodeWithText(
            buttonText(R.string.a11y_disclosure_continue_without_accessibility)
        )
        continueButton.performClick()
        continueButton.performClick()

        composeRule.runOnIdle {
            assertEquals(1, continuedWithoutAccessibility.get())
            assertEquals(0, usedAccessibility.get())
        }
    }

    @Test
    fun useAccessibilityOnlyInvokesThatChoiceOnce() {
        val continuedWithoutAccessibility = AtomicInteger()
        val usedAccessibility = AtomicInteger()
        showDialog(
            onContinueWithoutAccessibility = { continuedWithoutAccessibility.incrementAndGet() },
            onUseAccessibility = { usedAccessibility.incrementAndGet() },
            hideOnDecision = false
        )

        val useAccessibilityButton = composeRule.onNodeWithText(
            buttonText(R.string.a11y_disclosure_use_accessibility)
        )
        useAccessibilityButton.assertIsDisplayed()
        useAccessibilityButton.performClick()
        useAccessibilityButton.performClick()

        composeRule.runOnIdle {
            assertEquals(0, continuedWithoutAccessibility.get())
            assertEquals(1, usedAccessibility.get())
        }
    }

    @Test
    fun backPressContinuesWithoutAccessibility() {
        val continuedWithoutAccessibility = AtomicInteger()
        val usedAccessibility = AtomicInteger()
        showDialog(
            onContinueWithoutAccessibility = { continuedWithoutAccessibility.incrementAndGet() },
            onUseAccessibility = { usedAccessibility.incrementAndGet() }
        )

        Espresso.pressBack()

        composeRule.runOnIdle {
            assertEquals(1, continuedWithoutAccessibility.get())
            assertEquals(0, usedAccessibility.get())
        }
    }

    private fun showDialog(
        onContinueWithoutAccessibility: () -> Unit = {},
        onUseAccessibility: () -> Unit = {},
        hideOnDecision: Boolean = true
    ) {
        composeRule.setContent {
            EssentialKeyToolsTheme {
                var visible by remember { mutableStateOf(true) }
                if (visible) {
                    AccessibilityDisclosureDialog(
                        onContinueWithoutAccessibility = {
                            if (hideOnDecision) visible = false
                            onContinueWithoutAccessibility()
                        },
                        onUseAccessibility = {
                            if (hideOnDecision) visible = false
                            onUseAccessibility()
                        }
                    )
                }
            }
        }
    }

    private fun buttonText(stringRes: Int): String =
        resources.getString(stringRes).uppercase()
}
