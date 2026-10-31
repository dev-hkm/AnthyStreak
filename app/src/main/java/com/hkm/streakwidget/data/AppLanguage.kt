package com.hkm.streakwidget.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** User-facing app language. System follows the device without overriding it. */
enum class AppLanguage(val preferenceValue: String, val languageTag: String?) {
    SYSTEM("SYSTEM", null),
    ENGLISH("EN", "en"),
    VIETNAMESE("VI", "vi");

    companion object {
        fun fromPreference(value: String?): AppLanguage =
            entries.firstOrNull { it.preferenceValue.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

fun Context.localizedFor(language: AppLanguage): Context {
    val tag = language.languageTag ?: return this
    val locale = Locale.forLanguageTag(tag)
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLocales(android.os.LocaleList(locale))
    }
    return createConfigurationContext(configuration)
}
