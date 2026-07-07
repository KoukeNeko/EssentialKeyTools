package dev.koukeneko.essentialkeytools.unlock

/**
 * The Nothing OS system packages that consume the Essential Key's single press before any app can
 * react. Disabling them is what "frees" the single press (see docs/RESEARCH.md section 2b).
 *
 * The exact set differs by model and firmware, so this is only the candidate list to probe: the
 * unlocker queries which of these are actually installed rather than assuming all are present.
 */
object NothingConsumerPackages {

    /** Essential Space — owns single press (screenshot capture) and double press. */
    const val ESSENTIAL_SPACE = "com.nothing.ntessentialspace"

    /** Essential Recorder — owns long press (voice memo). */
    const val ESSENTIAL_RECORDER = "com.nothing.ntessentialrecorder"

    /** Essential intelligence — reported as an additional consumer on Phone (3a) Lite. */
    const val ESSENTIAL_INTELLIGENCE = "com.essentialintelligence"

    /** Package-name prefix used to discover Nothing consumers dynamically via `pm list packages`. */
    const val NOTHING_PACKAGE_PREFIX = "com.nothing"

    /**
     * Every candidate consumer to probe. [ESSENTIAL_INTELLIGENCE] is included even though it lacks
     * the [NOTHING_PACKAGE_PREFIX] because on some models it is a separate consumer package.
     */
    val CANDIDATES: List<String> = listOf(
        ESSENTIAL_SPACE,
        ESSENTIAL_RECORDER,
        ESSENTIAL_INTELLIGENCE
    )
}
