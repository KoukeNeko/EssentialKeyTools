package dev.koukeneko.essentialkeytools

import android.app.LocaleManager
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import dev.koukeneko.essentialkeytools.core.KeyGesture
import dev.koukeneko.essentialkeytools.settings.OnboardingState
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.components.FloatingNavBar
import dev.koukeneko.essentialkeytools.ui.components.NavBarItem
import dev.koukeneko.essentialkeytools.ui.components.navBarBackdropSource
import dev.koukeneko.essentialkeytools.ui.components.rememberNavBarBackdrop
import dev.koukeneko.essentialkeytools.ui.components.rememberNavBarScroll
import dev.koukeneko.essentialkeytools.ui.screens.ActionPickerScreen
import dev.koukeneko.essentialkeytools.ui.screens.DiagnosticsScreen
import dev.koukeneko.essentialkeytools.ui.screens.HapticPatternScreen
import dev.koukeneko.essentialkeytools.ui.screens.HomeScreen
import dev.koukeneko.essentialkeytools.ui.screens.KeySetupScreen
import dev.koukeneko.essentialkeytools.ui.screens.KeyTestScreen
import dev.koukeneko.essentialkeytools.ui.screens.OnboardingScreen
import dev.koukeneko.essentialkeytools.ui.screens.SettingsScreen
import dev.koukeneko.essentialkeytools.ui.screens.UnlockWizardScreen
import dev.koukeneko.essentialkeytools.ui.screens.openAccessibilitySettings
import dev.koukeneko.essentialkeytools.ui.theme.EssentialKeyToolsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Every screen of the explicit back stack. Screens with a [tab] are the top-level destinations of the
 * floating navigation bar; the others are pages opened from one of them.
 */
private enum class Screen(val tab: NavBarItem? = null) {
    HOME(NavBarItem(R.string.nav_home, R.drawable.ic_nav_home)),
    KEY_SETUP(NavBarItem(R.string.action_key_setup, R.drawable.ic_nav_setup)),
    KEY_TEST(NavBarItem(R.string.action_key_test, R.drawable.ic_nav_test)),
    SETTINGS(NavBarItem(R.string.action_open_settings, R.drawable.ic_nav_settings)),
    DIAGNOSTICS,
    UNLOCK_WIZARD,
    HAPTIC_PATTERN,
    ACTION_PICKER
}

private val TAB_SCREENS = Screen.entries.filter { it.tab != null }
private val TAB_ITEMS = TAB_SCREENS.mapNotNull { it.tab }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SettingsRepository.getInstance(this)
        // Read before the first frame so a Material You user never sees the Nothing palette flash by.
        // The preferences file is tiny, so blocking here costs milliseconds.
        val initialThemeStyle = runBlocking { repository.themeStyle.first() }
        setContent {
            val themeStyle by repository.themeStyle.collectAsState(initial = initialThemeStyle)
            EssentialKeyToolsTheme(themeStyle = themeStyle) {
                AppNavigation()
            }
        }
    }
}

/**
 * State-based navigation with an explicit back stack, avoiding a navigation dependency. The
 * action picker needs to know which gesture it is editing, so that is tracked alongside the stack.
 *
 * The Scaffold keeps the container edge-to-edge and hands each screen the system-bar insets as
 * PaddingValues; screens decide whether to apply them as padding (static screens) or contentPadding
 * (scrolling lists) so content can scroll under the transparent bars without being clipped by the nav
 * bar. On a tab the floating bar is the Scaffold's bottom bar, so those insets grow by its height and
 * content scrolls clear of it.
 */
@Composable
private fun AppNavigation() {
    val context = LocalContext.current
    val localeManager = remember(context) { context.getSystemService(LocaleManager::class.java) }
    val repository = remember { SettingsRepository.getInstance(context) }
    val persistedOnboardingState: OnboardingState? by repository.onboardingState.collectAsState(
        initial = null
    )
    val coroutineScope = rememberCoroutineScope()

    // Wait for DataStore before choosing the first screen so returning users never see a flash of
    // onboarding while their saved completion and page are loading. The empty Scaffold paints the
    // canvas meanwhile, and starts tracking the window insets in the first frame: reading them only
    // once the real Scaffold arrives misses the first dispatch and leaves the status bar inset at 0.
    val onboardingState = persistedOnboardingState
    if (onboardingState == null) {
        // Nothing to lay out yet, so the content padding is deliberately ignored.
        @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
        Scaffold(modifier = Modifier.fillMaxSize()) {}
        return
    }

    var showOnboarding by rememberSaveable { mutableStateOf(!onboardingState.completed) }
    val backStack = remember { mutableStateListOf(Screen.HOME) }
    var gestureBeingEdited by remember { mutableStateOf(KeyGesture.SINGLE_PRESS) }
    val current = backStack.last()
    // The tab a page was opened from stays selected while the bar slides away and back.
    val selectedTab = backStack.last { screen -> screen.tab != null }
    val backdrop = rememberNavBarBackdrop()
    val navScroll = rememberNavBarScroll()
    val background = MaterialTheme.colorScheme.background

    // A new screen starts with the full-size bar, whatever the last one left it as.
    LaunchedEffect(current) { navScroll.expand() }

    fun finishOnboarding() {
        showOnboarding = false
        coroutineScope.launch { repository.setOnboardingCompleted() }
    }

    fun leaveOnboardingForNow() {
        showOnboarding = false
    }

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    // A tab replaces the stack instead of piling onto it, so Back from any tab returns to Home
    // before it leaves the app.
    fun selectTab(screen: Screen) {
        backStack.clear()
        backStack.add(Screen.HOME)
        if (screen != Screen.HOME) {
            backStack.add(screen)
        }
    }

    fun editGesture(gesture: KeyGesture) {
        gestureBeingEdited = gesture
        navigateTo(Screen.ACTION_PICKER)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = !showOnboarding && current.tab != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                FloatingNavBar(
                    items = TAB_ITEMS,
                    selectedIndex = TAB_SCREENS.indexOf(selectedTab),
                    onSelect = { index -> selectTab(TAB_SCREENS[index]) },
                    backdrop = backdrop,
                    collapsed = navScroll.collapsed
                )
            }
        }
    ) { systemBarsPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(navScroll.connection)
                .navBarBackdropSource(backdrop, background)
        ) {
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
                    systemBarsPadding = systemBarsPadding
                )
            } else {
                // Route the system back gesture through the same stack so it pops screens instead of exiting.
                BackHandler(enabled = backStack.size > 1) { navigateBack() }

                when (current) {
                    Screen.HOME -> HomeScreen(
                        onEditGesture = ::editGesture,
                        onUnlockWizard = { navigateTo(Screen.UNLOCK_WIZARD) },
                        systemBarsPadding = systemBarsPadding
                    )

                    Screen.KEY_SETUP -> KeySetupScreen(systemBarsPadding = systemBarsPadding)

                    Screen.KEY_TEST -> KeyTestScreen(
                        onEditGesture = ::editGesture,
                        systemBarsPadding = systemBarsPadding
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        onEditHapticPattern = { navigateTo(Screen.HAPTIC_PATTERN) },
                        onDiagnostics = { navigateTo(Screen.DIAGNOSTICS) },
                        onReviewOnboarding = { showOnboarding = true },
                        systemBarsPadding = systemBarsPadding
                    )

                    Screen.DIAGNOSTICS -> DiagnosticsScreen(systemBarsPadding = systemBarsPadding)

                    Screen.UNLOCK_WIZARD -> UnlockWizardScreen(systemBarsPadding = systemBarsPadding)

                    Screen.HAPTIC_PATTERN -> HapticPatternScreen(systemBarsPadding = systemBarsPadding)

                    Screen.ACTION_PICKER -> ActionPickerScreen(
                        gesture = gestureBeingEdited,
                        onActionSaved = { navigateBack() },
                        systemBarsPadding = systemBarsPadding
                    )
                }
            }
        }
    }
}
