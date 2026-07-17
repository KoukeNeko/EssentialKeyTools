package dev.koukeneko.essentialkeytools.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun onboardingStepPersistsUntilOnboardingCompletes() = runBlocking {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStoreFile = File(temporaryFolder.root, "settings.preferences_pb")
        val repository = SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = dataStoreScope,
                produceFile = { dataStoreFile }
            )
        )

        try {
            assertEquals(
                OnboardingState(completed = false, step = OnboardingStep.LANGUAGE),
                repository.onboardingState.first()
            )

            repository.setOnboardingStep(OnboardingStep.ACCESSIBILITY)
            assertEquals(
                OnboardingState(completed = false, step = OnboardingStep.ACCESSIBILITY),
                repository.onboardingState.first()
            )

            repository.setOnboardingCompleted()
            assertEquals(
                OnboardingState(completed = true, step = OnboardingStep.LANGUAGE),
                repository.onboardingState.first()
            )
        } finally {
            dataStoreScope.cancel()
        }
    }
}
