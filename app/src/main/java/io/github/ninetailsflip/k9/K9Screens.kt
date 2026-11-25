package io.github.ninetailsflip.k9

import android.content.Context
import android.content.Intent

/** Where Traditional T9's engine sends you when it opens its settings ("Settings" in the hold-`*`
 *  menu, a tap on the dictionary-loading notification): to K9's screens, by Traditional T9's
 *  screen names. Its own PreferencesActivity is no longer reachable from the keyboard. */
object K9Screens {
    @JvmStatic
    fun intentFor(context: Context, screenName: String?): Intent {
        val target = when (screenName) {
            "Languages", "LanguageSelection" -> LanguagesActivity::class.java
            "Hotkeys" -> HotkeysActivity::class.java
            "KeyChars" -> KeyCharsActivity::class.java
            "DeleteWords" -> AddedWordsActivity::class.java
            else -> SettingsActivity::class.java
        }
        return Intent(context, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
