package dev.koukeneko.essentialkeytools.ui

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import java.util.Locale

/**
 * The languages the app can be switched to from within the app. [SYSTEM] defers to the device's
 * per-app language setting; the others pin a specific locale by its BCP-47 tag.
 */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(languageTag = null),
    ENGLISH(languageTag = "en"),
    TRADITIONAL_CHINESE(languageTag = "zh-TW")
}

/**
 * Reads and applies the app language through the platform per-app language API (Android 13+, always
 * present given the app's minSdk). Assigning the locale list makes the system recreate the activity
 * with the new configuration, so the UI re-renders in the chosen language without any manual Context
 * wrapping — this is the modern replacement for overriding the base Context's Configuration.
 */
object AppLocale {

    fun current(context: Context): AppLanguage {
        val selected = localeManager(context).applicationLocales
        if (selected.isEmpty) {
            return AppLanguage.SYSTEM
        }
        val selectedLanguage = selected[0].language
        return AppLanguage.entries.firstOrNull { language ->
            language.matchesLanguage(selectedLanguage)
        } ?: AppLanguage.SYSTEM
    }

    fun apply(context: Context, language: AppLanguage) {
        localeManager(context).applicationLocales = language.toLocaleList()
    }

    // Match on the language subtag alone so a stored "zh-Hant-TW" still resolves to zh-TW.
    private fun AppLanguage.matchesLanguage(languageCode: String): Boolean =
        languageTag != null && Locale.forLanguageTag(languageTag).language == languageCode

    private fun AppLanguage.toLocaleList(): LocaleList =
        languageTag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()

    private fun localeManager(context: Context): LocaleManager =
        context.getSystemService(LocaleManager::class.java)
}
