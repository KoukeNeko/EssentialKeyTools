package dev.koukeneko.essentialkeytools.contributors

import org.json.JSONArray

/**
 * Translates the GitHub "list repository contributors" JSON response into [Contributor]s. Kept free
 * of Android and networking types so the mapping is unit-testable on the JVM, mirroring how
 * settings serialization is isolated for the same reason.
 *
 * Entries missing a login or profile URL (anonymous/bot rows) are skipped rather than rendered as
 * blank credits. The API already returns contributors ordered by contribution count, and that order
 * is preserved.
 */
object GitHubContributorsParser {

    private const val LOGIN_FIELD = "login"
    private const val PROFILE_URL_FIELD = "html_url"

    fun parse(json: String): List<Contributor> {
        val entries = JSONArray(json)
        val contributors = mutableListOf<Contributor>()
        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            val handle = entry.optString(LOGIN_FIELD)
            val profileUrl = entry.optString(PROFILE_URL_FIELD)
            if (handle.isNotEmpty() && profileUrl.isNotEmpty()) {
                contributors.add(Contributor(handle = handle, profileUrl = profileUrl))
            }
        }
        return contributors
    }
}
