package dev.koukeneko.essentialkeytools.unlock

/**
 * The per-package outcome of a free/restore run, surfaced in the wizard so the user sees exactly
 * which packages changed and which failed.
 */
data class PackageActionResult(
    val packageName: String,
    val succeeded: Boolean,
    val detail: String
)

/** The overall result of an unlock or restore run across every targeted consumer package. */
data class UnlockRunResult(
    val perPackage: List<PackageActionResult>
) {
    val allSucceeded: Boolean get() = perPackage.isNotEmpty() && perPackage.all { it.succeeded }
    val anyFailed: Boolean get() = perPackage.any { !it.succeeded }
}
