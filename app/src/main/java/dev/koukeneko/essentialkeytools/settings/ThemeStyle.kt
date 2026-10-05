package dev.koukeneko.essentialkeytools.settings

/** The color style of the app: the fixed Nothing palette, or Material You colors from the wallpaper. */
enum class ThemeStyle(val storageValue: Int) {
    NOTHING(0),
    MATERIAL(1);

    companion object {
        // Nothing by default so upgrading never changes how the app looks.
        internal fun fromStorageValue(value: Int?): ThemeStyle =
            entries.firstOrNull { it.storageValue == value } ?: NOTHING
    }
}
