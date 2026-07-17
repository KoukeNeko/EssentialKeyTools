package dev.koukeneko.essentialkeytools.updates

/**
 * The version format published by this project: stable tags such as `v1.0.8` and preview tags such
 * as `preview-v1.0.9-pre.37`. Parsing is intentionally strict so an unexpected GitHub tag can never
 * be presented as an app update.
 */
@ConsistentCopyVisibility
data class AppVersion private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val previewNumber: Int?
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }

        // A stable release sorts after previews with the same numeric core.
        if (previewNumber == null && other.previewNumber != null) return 1
        if (previewNumber != null && other.previewNumber == null) return -1
        return compareValues(previewNumber ?: 0, other.previewNumber ?: 0)
    }

    companion object {
        private val VERSION_PATTERN = Regex(
            pattern = "^(?:preview-)?v?(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-pre\\.(\\d+))?$"
        )

        fun parse(rawValue: String): AppVersion? {
            val match = VERSION_PATTERN.matchEntire(rawValue.trim()) ?: return null
            return AppVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: return null,
                previewNumber = match.groupValues[4].takeIf { it.isNotEmpty() }?.toIntOrNull()
            )
        }
    }
}
