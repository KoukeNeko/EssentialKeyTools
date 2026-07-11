package dev.koukeneko.essentialkeytools.contributors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubContributorsParserTest {

    @Test
    fun parse_readsHandleAndProfileUrl() {
        val json = """
            [
              {"login":"KoukeNeko","html_url":"https://github.com/KoukeNeko","contributions":42},
              {"login":"octocat","html_url":"https://github.com/octocat","contributions":3}
            ]
        """.trimIndent()

        val contributors = GitHubContributorsParser.parse(json)

        assertEquals(
            listOf(
                Contributor("KoukeNeko", "https://github.com/KoukeNeko"),
                Contributor("octocat", "https://github.com/octocat")
            ),
            contributors
        )
    }

    @Test
    fun parse_preservesApiOrder() {
        val json = """
            [
              {"login":"first","html_url":"https://github.com/first"},
              {"login":"second","html_url":"https://github.com/second"}
            ]
        """.trimIndent()

        val handles = GitHubContributorsParser.parse(json).map { it.handle }

        assertEquals(listOf("first", "second"), handles)
    }

    @Test
    fun parse_skipsEntriesMissingHandleOrProfile() {
        val json = """
            [
              {"login":"valid","html_url":"https://github.com/valid"},
              {"html_url":"https://github.com/no-login"},
              {"login":"no-url"}
            ]
        """.trimIndent()

        val contributors = GitHubContributorsParser.parse(json)

        assertEquals(1, contributors.size)
        assertEquals("valid", contributors.first().handle)
    }

    @Test
    fun parse_emptyArray_returnsEmptyList() {
        assertTrue(GitHubContributorsParser.parse("[]").isEmpty())
    }
}
