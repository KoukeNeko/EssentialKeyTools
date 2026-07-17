package dev.koukeneko.essentialkeytools

import android.app.LocaleManager
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.koukeneko.essentialkeytools.core.KeyGesture
import dev.koukeneko.essentialkeytools.settings.OnboardingState
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.PRIVACY_POLICY_URL
import dev.koukeneko.essentialkeytools.ui.openExternalUrl
import dev.koukeneko.essentialkeytools.ui.screens.ActionPickerScreen
import dev.koukeneko.essentialkeytools.ui.screens.HomeScreen
import dev.koukeneko.essentialkeytools.ui.screens.KeySetupScreen
import dev.koukeneko.essentialkeytools.ui.screens.KeyTestScreen
import dev.koukeneko.essentialkeytools.ui.screens.OnboardingScreen
import dev.koukeneko.essentialkeytools.ui.screens.UnlockWizardScreen
import dev.koukeneko.essentialkeytools.ui.screens.openAccessibilitySettings
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import kotlinx.coroutines.launch

/** The screens reachable from the home control panel via the explicit back stack. */
private enum class Screen {
    HOME,
    KEY_SETUP,
    KEY_TEST,
    UNLOCK_WIZARD,
    ACTION_PICKER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EssentialKeyToolsTheme {
                // The Scaffold keeps the black container edge-to-edge and hands each screen the
                // system-bar insets as PaddingValues; screens decide whether to apply them as
                // padding (static screens) or contentPadding (scrolling lists) so content can
                // scroll under the transparent bars without being clipped by the nav bar.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(systemBarsPadding = innerPadding)
                }
            }
        }
    }
}

/**
 * State-based navigation with an explicit back stack, avoiding a navigation dependency. The
 * action picker needs to know which gesture it is editing, so that is tracked alongside the stack.
 */
@Composable
private fun AppNavigation(systemBarsPadding: PaddingValues) {
    val context = LocalContext.current
    val localeManager = remember(context) { context.getSystemService(LocaleManager::class.java) }
    val repository = remember { SettingsRepository.getInstance(context) }
    val persistedOnboardingState: OnboardingState? by repository.onboardingState.collectAsState(
        initial = null
    )
    val coroutineScope = rememberCoroutineScope()

    // Wait for DataStore before choosing the first screen so returning users never see a flash of
    // onboarding while their saved completion and page are loading.
    val onboardingState = persistedOnboardingState ?: return

    var showOnboarding by rememberSaveable { mutableStateOf(!onboardingState.completed) }
    val backStack = remember { mutableStateListOf(Screen.HOME) }
    var gestureBeingEdited by remember { mutableStateOf(KeyGesture.SINGLE_PRESS) }

    fun finishOnboarding() {
        showOnboarding = false
        coroutineScope.launch { repository.setOnboardingCompleted() }
    }

    fun leaveOnboardingForNow() {
        showOnboarding = false
    }

    if (showOnboarding) {
        OnboardingScreen(
            initialStep = onboardingState.step,
            onStepChanged = { step ->
                if (!onboardingState.completed) {
                    coroutineScope.launch { repository.setOnboardingStep(step) }
                }
            },
            initialLanguageTag = localeManager.applicationLocales.toLanguageTags(),
            onLanguageSelected = { languageTag ->
                localeManager.applicationLocales = if (languageTag.isEmpty()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(languageTag)
                }
            },
            onLeaveOnboarding = ::leaveOnboardingForNow,
            onContinueWithoutAccessibility = ::finishOnboarding,
            onUseAccessibility = {
                finishOnboarding()
                openAccessibilitySettings(context)
            },
            onOpenPrivacyPolicy = { openExternalUrl(context, PRIVACY_POLICY_URL) },
            systemBarsPadding = systemBarsPadding
        )
        return
    }

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    // Route the system back gesture through the same stack so it pops screens instead of exiting.
    BackHandler(enabled = backStack.size > 1) { navigateBack() }

    fun editGesture(gesture: KeyGesture) {
        gestureBeingEdited = gesture
        navigateTo(Screen.ACTION_PICKER)
    }

    when (backStack.last()) {
        Screen.HOME -> HomeScreen(
            onEditGesture = ::editGesture,
            onUnlockWizard = { navigateTo(Screen.UNLOCK_WIZARD) },
            onKeySetup = { navigateTo(Screen.KEY_SETUP) },
            onKeyTest = { navigateTo(Screen.KEY_TEST) },
            onReviewOnboarding = { showOnboarding = true },
            systemBarsPadding = systemBarsPadding
        )

        Screen.KEY_SETUP -> KeySetupScreen(systemBarsPadding = systemBarsPadding)

        Screen.KEY_TEST -> KeyTestScreen(
            onEditGesture = ::editGesture,
            systemBarsPadding = systemBarsPadding
        )

        Screen.UNLOCK_WIZARD -> UnlockWizardScreen(systemBarsPadding = systemBarsPadding)

        Screen.ACTION_PICKER -> ActionPickerScreen(
            gesture = gestureBeingEdited,
            onActionSaved = { navigateBack() },
            systemBarsPadding = systemBarsPadding
        )
    }
}
