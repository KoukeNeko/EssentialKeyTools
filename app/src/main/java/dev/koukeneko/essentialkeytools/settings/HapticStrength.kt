package dev.koukeneko.essentialkeytools.settings

/** How strongly the device vibrates when the Essential Key is pressed. */
enum class HapticStrength(val storageValue: Int) {
    OFF(0),
    LIGHT(1),
    MEDIUM(2),
    STRONG(3);

    companion object {
        // Off by default so upgrading never adds vibration the user did not ask for.
        internal fun fromStorageValue(value: Int?): HapticStrength =
            entries.firstOrNull { it.storageValue == value } ?: OFF
    }
}
