package com.newjourney.core.datastore

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app's own language, chosen on the Profile page. Null means "follow the phone".
 *
 * Android 13 and newer keep the choice themselves (it also appears in the phone's settings under
 * App languages). Older phones keep it here and every screen is opened in that language.
 */
object AppLanguage {

    /** The languages the app is translated into, as language tags. */
    val supported: List<String> = listOf("en", "nl", "de", "fr", "es")

    private const val PREFS = "app_language"
    private const val KEY_TAG = "language_tag"

    /** The chosen language tag, or null when the app follows the phone. */
    fun current(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) null else locales[0].language.takeIf { it in supported }
        } else {
            stored(context)
        }

    /** Switches the language and reopens the screen in it. [tag] null follows the phone again. */
    fun set(activity: Activity, tag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android reopens the screens in the new language by itself.
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            val editor = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            if (tag == null) editor.remove(KEY_TAG) else editor.putString(KEY_TAG, tag)
            // Saved straight away, because the screen reopens right after this.
            editor.commit()
            activity.recreate()
        }
    }

    /** For attachBaseContext on Android 12 and older: [base] in the chosen language. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = stored(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    private fun stored(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)?.takeIf { it in supported }
}
