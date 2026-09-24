package dev.koukeneko.essentialkeytools.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.core.KeyGesture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The default Linux scanCode of the Essential Key, firmware-verified on Phone (3a)/(3a) Lite.
 * Other models may differ, so the value is learnable at runtime and only seeded from here.
 */
const val DEFAULT_ESSENTIAL_KEY_SCAN_CODE = 250

private const val DATASTORE_NAME = "essential_key_settings"

// One DataStore instance per process, owned by the application Context.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME
)

/**
 * Persists the app's configuration: onboarding completion and progress, the learned Essential Key
 * scanCode, the gesture -> action mapping and the haptic feedback settings. Exposes reactive [Flow]s for observers (service, UI)
 * and suspend writers.
 *
 * No DI framework: a manual process-wide singleton via [getInstance] keeps it simple while still
 * sharing one DataStore across the service and the activity.
 */
class SettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val scanCodeKey = intPreferencesKey("essential_key_scan_code")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val onboardingStepKey = intPreferencesKey("onboarding_step")
    private val hapticStrengthKey = intPreferencesKey("haptic_strength")
    private val hapticsOnActionOnlyKey = booleanPreferencesKey("haptics_on_action_only")

    val onboardingState: Flow<OnboardingState> = dataStore.data.map { preferences ->
        OnboardingState(
            completed = preferences[onboardingCompletedKey] ?: false,
            step = OnboardingStep.fromStorageValue(preferences[onboardingStepKey])
        )
    }

    val essentialKeyScanCode: Flow<Int> = dataStore.data.map { preferences ->
        preferences[scanCodeKey] ?: DEFAULT_ESSENTIAL_KEY_SCAN_CODE
    }

    val gestureActionMap: Flow<GestureActionMap> = dataStore.data.map { preferences ->
        SettingsSerialization.decodeMap { keyName ->
            preferences[stringPreferencesKey(keyName)]
        }
    }

    val hapticStrength: Flow<HapticStrength> = dataStore.data.map { preferences ->
        HapticStrength.fromStorageValue(preferences[hapticStrengthKey])
    }

    /** Whether to vibrate once when a gesture runs its action instead of on every press. */
    val hapticsOnActionOnly: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[hapticsOnActionOnlyKey] ?: false
    }

    suspend fun setEssentialKeyScanCode(scanCode: Int) {
        dataStore.edit { preferences ->
            preferences[scanCodeKey] = scanCode
        }
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = true
            preferences.remove(onboardingStepKey)
        }
    }

    suspend fun setOnboardingStep(step: OnboardingStep) {
        dataStore.edit { preferences ->
            preferences[onboardingStepKey] = step.storageValue
        }
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        dataStore.edit { preferences ->
            preferences[hapticStrengthKey] = strength.storageValue
        }
    }

    suspend fun setHapticsOnActionOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[hapticsOnActionOnlyKey] = enabled
        }
    }

    suspend fun setActionFor(gesture: KeyGesture, action: KeyAction) {
        val persisted = SettingsSerialization.encodeAction(action)
        val idKey = stringPreferencesKey(SettingsSerialization.actionIdKeyName(gesture))
        val payloadKey = stringPreferencesKey(SettingsSerialization.actionPayloadKeyName(gesture))
        dataStore.edit { preferences ->
            preferences[idKey] = persisted.id
            val payload = persisted.payload
            if (payload == null) {
                preferences.remove(payloadKey)
            } else {
                preferences[payloadKey] = payload
            }
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(
                    context.applicationContext.settingsDataStore
                ).also { instance = it }
            }
    }
}
