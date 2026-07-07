package dev.koukeneko.essentialkeytools.unlock

/**
 * Builds the `settings` command lines that enable or disable an accessibility service through the
 * secure settings, and the pure list-merge logic those commands need. Pure string assembly with no
 * Android dependency, so the exact arguments and the colon-list merge are verifiable in a unit test.
 *
 * Android stores enabled accessibility services as a single colon-separated string in the secure
 * setting `enabled_accessibility_services`. Enabling ours means appending our component id to that
 * list without disturbing any service the user already enabled; disabling means removing it.
 */
object AccessibilityServiceCommands {

    private const val SETTINGS = "settings"
    private const val GET = "get"
    private const val PUT = "put"
    private const val SECURE = "secure"

    /** Secure setting holding the colon-separated list of enabled accessibility service components. */
    const val ENABLED_SERVICES_KEY = "enabled_accessibility_services"

    /** Secure flag Android checks to decide whether accessibility is on at all. */
    const val ACCESSIBILITY_ENABLED_KEY = "accessibility_enabled"

    private const val ENABLED_FLAG_ON = "1"
    private const val SERVICE_SEPARATOR = ":"

    /** `settings get` prints this literal token when the setting has never been written. */
    private const val NULL_LITERAL = "null"

    /** `settings get secure enabled_accessibility_services`. */
    fun getEnabledServices(): List<String> =
        listOf(SETTINGS, GET, SECURE, ENABLED_SERVICES_KEY)

    /** `settings put secure enabled_accessibility_services <value>`. */
    fun putEnabledServices(value: String): List<String> =
        listOf(SETTINGS, PUT, SECURE, ENABLED_SERVICES_KEY, value)

    /** `settings put secure accessibility_enabled 1` — flips the master accessibility flag on. */
    fun putAccessibilityEnabled(): List<String> =
        listOf(SETTINGS, PUT, SECURE, ACCESSIBILITY_ENABLED_KEY, ENABLED_FLAG_ON)

    /**
     * Parses the raw `settings get` output into the current component list. The command prints the
     * literal "null" for an unset value and may carry surrounding whitespace; both collapse to an
     * empty list. Blank entries between separators are dropped so a merge never reintroduces them.
     */
    fun parseEnabledServices(rawOutput: String): List<String> {
        val trimmed = rawOutput.trim()
        if (trimmed.isEmpty() || trimmed == NULL_LITERAL) {
            return emptyList()
        }
        return trimmed.split(SERVICE_SEPARATOR)
            .map { component -> component.trim() }
            .filter { component -> component.isNotEmpty() }
    }

    /**
     * Returns the merged list with [component] appended if it is not already present, preserving the
     * existing entries and their order. An already-present component yields the input unchanged.
     */
    fun appendService(current: List<String>, component: String): List<String> =
        if (current.contains(component)) current else current + component

    /** Returns the list with every occurrence of [component] removed, preserving the rest in order. */
    fun removeService(current: List<String>, component: String): List<String> =
        current.filter { entry -> entry != component }

    /** Joins components back into the colon-separated form `settings put` expects. */
    fun joinServices(components: List<String>): String =
        components.joinToString(separator = SERVICE_SEPARATOR)

    /**
     * Convenience for the enable path: parse the raw get output, append [component] if absent, and
     * return the value to write back. Returns null when the component is already present so the
     * caller can skip the redundant write.
     */
    fun mergedEnableValue(rawOutput: String, component: String): String? {
        val current = parseEnabledServices(rawOutput)
        if (current.contains(component)) {
            return null
        }
        return joinServices(appendService(current, component))
    }

    /**
     * Convenience for the disable path: parse the raw get output and return the value to write back
     * with [component] removed. Returns null when the component was not present so the caller can
     * skip the redundant write.
     */
    fun mergedDisableValue(rawOutput: String, component: String): String? {
        val current = parseEnabledServices(rawOutput)
        if (!current.contains(component)) {
            return null
        }
        return joinServices(removeService(current, component))
    }
}
