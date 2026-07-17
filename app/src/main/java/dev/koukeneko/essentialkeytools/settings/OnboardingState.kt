package dev.koukeneko.essentialkeytools.settings

/** The first-run page that should be restored after the app or process is restarted. */
enum class OnboardingStep(val storageValue: Int) {
    LANGUAGE(0),
    INTRODUCTION(1),
    ACCESSIBILITY(2);

    companion object {
        internal fun fromStorageValue(value: Int?): OnboardingStep =
            entries.firstOrNull { it.storageValue == value } ?: LANGUAGE
    }
}

/** Completion and resumable page are read from one DataStore snapshot. */
data class OnboardingState(
    val completed: Boolean,
    val step: OnboardingStep
)
