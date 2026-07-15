package dev.koukeneko.essentialkeytools.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
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
        showDialog(confirmButtonTextRes = R.string.a11y_disclosure_agree_enable)

        composeRule.onNodeWithText(resources.getString(R.string.a11y_disclosure_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(resources.getString(R.string.a11y_disclosure_body))
            .assertIsDisplayed()
        composeRule.onNodeWithText(buttonText(R.string.a11y_disclosure_decline))
            .assertIsDisplayed()
        composeRule.onNodeWithText(buttonText(R.string.a11y_disclosure_agree_enable))
            .assertIsDisplayed()
    }

    @Test
    fun decline_onlyInvokesDeclineOnce() {
        val declined = AtomicInteger()
        val consented = AtomicInteger()
        showDialog(
            confirmButtonTextRes = R.string.a11y_disclosure_agree_enable,
            onDecline = { declined.incrementAndGet() },
            onConsent = { consented.incrementAndGet() },
            hideOnDecision = false
        )

        val declineButton = composeRule.onNodeWithText(
            buttonText(R.string.a11y_disclosure_decline)
        )
        declineButton.performClick()
        declineButton.performClick()

        composeRule.runOnIdle {
            assertEquals(1, declined.get())
            assertEquals(0, consented.get())
        }
    }

    @Test
    fun consent_onlyInvokesConsentOnce() {
        val declined = AtomicInteger()
        val consented = AtomicInteger()
        showDialog(
            confirmButtonTextRes = R.string.a11y_disclosure_agree_open_settings,
            onDecline = { declined.incrementAndGet() },
            onConsent = { consented.incrementAndGet() },
            hideOnDecision = false
        )

        val consentButton = composeRule.onNodeWithText(
            buttonText(R.string.a11y_disclosure_agree_open_settings)
        )
        consentButton.assertIsDisplayed()
        consentButton.performClick()
        consentButton.performClick()

        composeRule.runOnIdle {
            assertEquals(0, declined.get())
            assertEquals(1, consented.get())
        }
    }

    @Test
    fun backPress_isDeclineAndNeverConsent() {
        val declined = AtomicInteger()
        val consented = AtomicInteger()
        showDialog(
            confirmButtonTextRes = R.string.a11y_disclosure_agree_enable,
            onDecline = { declined.incrementAndGet() },
            onConsent = { consented.incrementAndGet() }
        )

        Espresso.pressBack()

        composeRule.runOnIdle {
            assertEquals(1, declined.get())
            assertEquals(0, consented.get())
        }
    }

    private fun showDialog(
        confirmButtonTextRes: Int,
        onDecline: () -> Unit = {},
        onConsent: () -> Unit = {},
        hideOnDecision: Boolean = true
    ) {
        composeRule.setContent {
            EssentialKeyToolsTheme {
                var visible by remember { mutableStateOf(true) }
                if (visible) {
                    AccessibilityDisclosureDialog(
                        confirmButtonTextRes = confirmButtonTextRes,
                        onDecline = {
                            if (hideOnDecision) visible = false
                            onDecline()
                        },
                        onConsent = {
                            if (hideOnDecision) visible = false
                            onConsent()
                        }
                    )
                }
            }
        }
    }

    private fun buttonText(stringRes: Int): String =
        resources.getString(stringRes).uppercase()
}
