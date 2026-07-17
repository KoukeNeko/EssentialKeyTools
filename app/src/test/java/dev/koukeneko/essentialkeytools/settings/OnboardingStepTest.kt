package dev.koukeneko.essentialkeytools.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepTest {

    @Test
    fun storedValuesRestoreEveryStep() {
        for (step in OnboardingStep.entries) {
            assertEquals(step, OnboardingStep.fromStorageValue(step.storageValue))
        }
    }

    @Test
    fun missingOrUnknownValueStartsAtLanguage() {
        assertEquals(OnboardingStep.LANGUAGE, OnboardingStep.fromStorageValue(null))
        assertEquals(OnboardingStep.LANGUAGE, OnboardingStep.fromStorageValue(Int.MAX_VALUE))
    }
}
