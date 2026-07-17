package dev.koukeneko.essentialkeytools.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun languageIntroductionAndDisclosureAppearInOrder() {
        val selectedLanguage = AtomicReference<String>()
        showOnboarding(onLanguageSelected = selectedLanguage::set)

        composeRule.onNodeWithText(resources.getString(R.string.onboarding_language_headline))
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            resources.getString(R.string.onboarding_language_traditional_chinese)
        ).performClick()
        composeRule.runOnIdle {
            assertEquals("zh-TW", selectedLanguage.get())
        }

        composeRule.onNodeWithText(buttonText(R.string.onboarding_continue)).performClick()

        composeRule.onNodeWithText(resources.getString(R.string.onboarding_intro_headline))
            .assertIsDisplayed()
        composeRule.onNodeWithText(resources.getString(R.string.onboarding_intro_setup_body))
            .assertIsDisplayed()

        composeRule.onNodeWithText(buttonText(R.string.onboarding_continue)).performClick()

        composeRule.onNodeWithText(resources.getString(R.string.onboarding_permission_headline))
            .assertIsDisplayed()
        composeRule.onNodeWithText(resources.getString(R.string.onboarding_permission_data_body))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(resources.getString(R.string.onboarding_permission_next_body))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(buttonText(R.string.onboarding_use_accessibility))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            buttonText(R.string.onboarding_continue_without_accessibility)
        )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun privacyPolicyCanOpenWithoutAcceptingOrLeaving() {
        val exits = AtomicInteger()
        val settingsOpens = AtomicInteger()
        val privacyOpens = AtomicInteger()
        showOnboarding(
            onExit = { exits.incrementAndGet() },
            onUseAccessibility = { settingsOpens.incrementAndGet() },
            onOpenPrivacyPolicy = { privacyOpens.incrementAndGet() }
        )

        openPermissionStep()
        composeRule.onNodeWithText(buttonText(R.string.action_open_privacy_policy))
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(0, exits.get())
            assertEquals(0, settingsOpens.get())
            assertEquals(1, privacyOpens.get())
        }
    }

    @Test
    fun continueWithoutAccessibilityExitsWithoutOpeningSettings() {
        val exits = AtomicInteger()
        val settingsOpens = AtomicInteger()
        showOnboarding(
            onExit = { exits.incrementAndGet() },
            onUseAccessibility = { settingsOpens.incrementAndGet() }
        )

        openPermissionStep()
        composeRule.onNodeWithText(
            buttonText(R.string.onboarding_continue_without_accessibility)
        )
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, exits.get())
            assertEquals(0, settingsOpens.get())
        }
    }

    @Test
    fun useAccessibilityOnlyOpensSettingsOnce() {
        val exits = AtomicInteger()
        val settingsOpens = AtomicInteger()
        showOnboarding(
            onExit = { exits.incrementAndGet() },
            onUseAccessibility = { settingsOpens.incrementAndGet() }
        )

        openPermissionStep()
        val useAccessibilityButton = composeRule.onNodeWithText(
            buttonText(R.string.onboarding_use_accessibility)
        )
        useAccessibilityButton.performScrollTo()
        useAccessibilityButton.performClick()
        useAccessibilityButton.performClick()

        composeRule.runOnIdle {
            assertEquals(0, exits.get())
            assertEquals(1, settingsOpens.get())
        }
    }

    private fun openPermissionStep() {
        composeRule.onNodeWithText(buttonText(R.string.onboarding_continue)).performClick()
        composeRule.onNodeWithText(buttonText(R.string.onboarding_continue)).performClick()
    }

    private fun showOnboarding(
        onLanguageSelected: (String) -> Unit = {},
        onExit: () -> Unit = {},
        onUseAccessibility: () -> Unit = {},
        onOpenPrivacyPolicy: () -> Unit = {}
    ) {
        composeRule.setContent {
            EssentialKeyToolsTheme {
                OnboardingScreen(
                    initialLanguageTag = "",
                    onLanguageSelected = onLanguageSelected,
                    onExit = onExit,
                    onUseAccessibility = onUseAccessibility,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy
                )
            }
        }
    }

    private fun buttonText(stringRes: Int): String =
        resources.getString(stringRes).uppercase()
}
