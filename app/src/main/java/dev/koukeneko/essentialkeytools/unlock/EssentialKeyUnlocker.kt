package dev.koukeneko.essentialkeytools.unlock

/**
 * Orchestrates freeing and restoring the Essential Key's single press. Reads consumer-package state
 * through a [PackageStateReader]. The class owns the candidate-package list so status reporting is
 * consistent between the home screen and unlock wizard while remaining unit-testable with a fake.
 */
class EssentialKeyUnlocker(
    private val stateReader: PackageStateReader,
    private val candidatePackages: List<String> = NothingConsumerPackages.CANDIDATES
) {
    /** State of every candidate consumer package, in candidate order. */
    fun readPackageStates(): List<PackageState> =
        candidatePackages.map { packageName -> stateReader.stateOf(packageName) }

    /** The overall unlock status derived from the current package states. */
    fun readStatus(): UnlockStatus =
        UnlockStatus.fromPackageStates(readPackageStates())
}
