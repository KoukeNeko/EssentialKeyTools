package dev.koukeneko.essentialkeytools.ui.screens

import androidx.activity.ComponentActivity
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
class AccessibilityDisclosureScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun disclosurePage_showsFullCopyAndBothChoices() {
        showScreen()

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
        ).assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun continueWithoutAccessibilityOnlyInvokesThatChoiceOnce() {
        val continuedWithoutAccessibility = AtomicInteger()
        val usedAccessibility = AtomicInteger()
        showScreen(
            onContinueWithoutAccessibility = { continuedWithoutAccessibility.incrementAndGet() },
            onUseAccessibility = { usedAccessibility.incrementAndGet() }
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
        showScreen(
            onContinueWithoutAccessibility = { continuedWithoutAccessibility.incrementAndGet() },
            onUseAccessibility = { usedAccessibility.incrementAndGet() }
        )

        val useAccessibilityButton = composeRule.onNodeWithText(
            buttonText(R.string.a11y_disclosure_use_accessibility)
        )
        useAccessibilityButton.performClick()
        useAccessibilityButton.performClick()

        composeRule.runOnIdle {
            assertEquals(0, continuedWithoutAccessibility.get())
            assertEquals(1, usedAccessibility.get())
        }
    }

    @Test
    fun backPressUsesTheDistinctDeclineCallback() {
        val backDeclines = AtomicInteger()
        val continuedWithoutAccessibility = AtomicInteger()
        val usedAccessibility = AtomicInteger()
        showScreen(
            onBackWithoutAccessibility = { backDeclines.incrementAndGet() },
            onContinueWithoutAccessibility = { continuedWithoutAccessibility.incrementAndGet() },
            onUseAccessibility = { usedAccessibility.incrementAndGet() }
        )

        Espresso.pressBack()

        composeRule.runOnIdle {
            assertEquals(1, backDeclines.get())
            assertEquals(0, continuedWithoutAccessibility.get())
            assertEquals(0, usedAccessibility.get())
        }
    }

    private fun showScreen(
        onBackWithoutAccessibility: () -> Unit = {},
        onContinueWithoutAccessibility: () -> Unit = {},
        onUseAccessibility: () -> Unit = {}
    ) {
        composeRule.setContent {
            EssentialKeyToolsTheme {
                AccessibilityDisclosureScreen(
                    onBackWithoutAccessibility = onBackWithoutAccessibility,
                    onContinueWithoutAccessibility = onContinueWithoutAccessibility,
                    onUseAccessibility = onUseAccessibility
                )
            }
        }
    }

    private fun buttonText(stringRes: Int): String =
        resources.getString(stringRes).uppercase()
}
