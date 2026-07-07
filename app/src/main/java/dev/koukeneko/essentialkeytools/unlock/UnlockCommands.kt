package dev.koukeneko.essentialkeytools.unlock

/**
 * Builds the `pm` command lines that free or restore a consumer package. Pure string assembly with
 * no Android dependency, so the exact arguments are verifiable in a unit test.
 *
 * `disable-user --user 0` is used rather than plain `disable` because it is the no-root form that
 * Shizuku can run for a package the shell does not own (see docs/RESEARCH.md section 2b).
 */
object UnlockCommands {

    private const val PM = "pm"
    private const val DISABLE_USER = "disable-user"
    private const val ENABLE = "enable"
    private const val USER_FLAG = "--user"
    private const val CURRENT_USER = "0"
    private const val LIST_PACKAGES = "list"
    private const val PACKAGES = "packages"

    /** Frees a package: `pm disable-user --user 0 <pkg>`. */
    fun disable(packageName: String): List<String> =
        listOf(PM, DISABLE_USER, USER_FLAG, CURRENT_USER, packageName)

    /** Restores a package: `pm enable <pkg>`. */
    fun enable(packageName: String): List<String> =
        listOf(PM, ENABLE, packageName)

    /** Lists installed packages under a prefix: `pm list packages <prefix>`. */
    fun listPackages(prefix: String): List<String> =
        listOf(PM, LIST_PACKAGES, PACKAGES, prefix)

    /**
     * Parses the output of `pm list packages` into bare package names, stripping the `package:`
     * prefix each line carries. Blank lines are ignored.
     */
    fun parsePackageList(output: String): List<String> =
        output.lineSequence()
            .map { line -> line.trim().removePrefix(PACKAGE_LINE_PREFIX) }
            .filter { name -> name.isNotEmpty() }
            .toList()

    private const val PACKAGE_LINE_PREFIX = "package:"
}
