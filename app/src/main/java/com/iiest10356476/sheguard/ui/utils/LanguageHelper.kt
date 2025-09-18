package com.iiest10356476.sheguard.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.*

class LanguageHelper {

    companion object {
        private const val PREF_NAME = "app_language"
        private const val KEY_LANGUAGE = "selected_language"

        // Supported languages
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_AFRIKAANS = "af"
        const val LANGUAGE_ZULU = "zu"
        const val LANGUAGE_XHOSA = "xh"

        /**
         * Get saved language preference
         */
        fun getSavedLanguage(context: Context): String {
            val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return sharedPref.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
        }

        /**
         * Save language preference
         */
        fun saveLanguage(context: Context, languageCode: String) {
            val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(KEY_LANGUAGE, languageCode)
                apply()
            }
        }

        /**
         * Set app language
         */
        fun setAppLanguage(context: Context, languageCode: String): Context {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)

            return context.createConfigurationContext(configuration)
        }

        /**
         * Get language display name
         */
        fun getLanguageDisplayName(languageCode: String): String {
            return when (languageCode) {
                LANGUAGE_ENGLISH -> "English"
                LANGUAGE_AFRIKAANS -> "Afrikaans"
                LANGUAGE_ZULU -> "isiZulu"
                LANGUAGE_XHOSA -> "isiXhosa"
                else -> "English"
            }
        }

        /**
         * Get all supported languages
         */
        fun getSupportedLanguages(): List<Pair<String, String>> {
            return listOf(
                Pair(LANGUAGE_ENGLISH, "English"),
                Pair(LANGUAGE_AFRIKAANS, "Afrikaans"),
                Pair(LANGUAGE_ZULU, "isiZulu"),
                Pair(LANGUAGE_XHOSA, "isiXhosa")
            )
        }
    }
}