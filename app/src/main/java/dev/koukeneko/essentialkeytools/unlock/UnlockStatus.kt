package dev.koukeneko.essentialkeytools.unlock

/**
 * Whether the Essential Key's single press has been freed from Nothing OS. Derived purely from the
 * per-package states so the mapping can be unit-tested without Android.
 */
enum class UnlockStatus {
    /** No consumer packages were found — nothing to unlock (unusual, treated as informational). */
    NO_CONSUMERS,

    /** At least one consumer is still enabled and intercepting the key. */
    LOCKED,

    /** Some consumers freed, others still enabled — an OS update may have re-enabled one. */
    PARTIALLY_FREED,

    /** Every installed consumer is disabled — the single press is fully freed. */
    FREED;

    companion object {
        /**
         * Reduces the states of the installed consumer packages to one overall status. Packages
         * that are not installed are ignored: they were never consumers on this model.
         */
        fun fromPackageStates(states: List<PackageState>): UnlockStatus {
            val installed = states.filter { state -> state.installed }
            if (installed.isEmpty()) {
                return NO_CONSUMERS
            }
            val consuming = installed.count { state -> state.isConsuming }
            return when (consuming) {
                0 -> FREED
                installed.size -> LOCKED
                else -> PARTIALLY_FREED
            }
        }
    }
}
