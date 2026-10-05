package dev.koukeneko.essentialkeytools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.actions.KeyHaptics
import dev.koukeneko.essentialkeytools.settings.HapticPattern
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val SLIDER_GAP = 8.dp

/**
 * Designs the vibration played by the Custom haptic level. Each slider saves when released and
 * plays the result, so the pattern can be tuned by feel; Test replays it without changing anything.
 */
@Composable
fun HapticPatternScreen(
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val keyHaptics = remember(context) { KeyHaptics(context) }
    val coroutineScope = rememberCoroutineScope()

    var pattern by remember { mutableStateOf<HapticPattern?>(null) }
    LaunchedEffect(repository) { pattern = repository.customHapticPattern.first() }
    val current = pattern ?: return

    fun saveAndPlay() {
        // Read the state rather than `current`: a quick tap can finish before the last change recomposes.
        val latest = pattern ?: return
        coroutineScope.launch { repository.setCustomHapticPattern(latest) }
        keyHaptics.play(latest)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.haptic_pattern_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))

        NothingCard(modifier = Modifier.fillMaxWidth()) {
            NothingSectionLabel(text = stringResource(R.string.haptic_pattern_label))
            Spacer(modifier = Modifier.height(LABEL_GAP))
            Column(verticalArrangement = Arrangement.spacedBy(SLIDER_GAP)) {
                PatternSlider(
                    label = stringResource(R.string.haptic_pattern_count),
                    valueText = current.pulseCount.toString(),
                    value = current.pulseCount,
                    range = HapticPattern.PULSE_COUNT_RANGE,
                    onValueChange = { pattern = current.copy(pulseCount = it) },
                    onValueChangeFinished = ::saveAndPlay
                )
                PatternSlider(
                    label = stringResource(R.string.haptic_pattern_length),
                    valueText = "${current.pulseMillis} ms",
                    value = current.pulseMillis,
                    range = HapticPattern.PULSE_MILLIS_RANGE,
                    // Coercing keeps the frequency inside what the new pulse length allows.
                    onValueChange = { pattern = current.copy(pulseMillis = it).coerced() },
                    onValueChangeFinished = ::saveAndPlay
                )
                PatternSlider(
                    label = stringResource(R.string.haptic_pattern_frequency),
                    valueText = "${current.frequencyHz} Hz",
                    value = current.frequencyHz,
                    range = HapticPattern.FREQUENCY_HZ_RANGE.first..
                        HapticPattern.maxFrequencyHz(current.pulseMillis),
                    enabled = current.pulseCount > 1,
                    onValueChange = { pattern = current.copy(frequencyHz = it) },
                    onValueChangeFinished = ::saveAndPlay
                )
                PatternSlider(
                    label = stringResource(R.string.haptic_pattern_amplitude),
                    valueText = "${current.amplitudePercent}%",
                    value = current.amplitudePercent,
                    range = HapticPattern.AMPLITUDE_PERCENT_RANGE,
                    onValueChange = { pattern = current.copy(amplitudePercent = it) },
                    onValueChangeFinished = ::saveAndPlay
                )
            }
            if (!keyHaptics.hasAmplitudeControl) {
                Spacer(modifier = Modifier.height(LABEL_GAP))
                Text(
                    text = stringResource(R.string.haptic_pattern_no_amplitude_control),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(CARD_GAP))
        NothingButton(
            text = stringResource(R.string.haptic_pattern_test),
            onClick = { keyHaptics.play(current) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PatternSlider(
    label: String,
    valueText: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}
