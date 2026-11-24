package io.github.ninetailsflip.k9

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import io.github.ninetailsflip.update.updatesSection
import life.furrer.nine.core.NineSurface
import life.furrer.nine.core.settings.NineSettingsActivity
import life.furrer.nine.core.settings.SectionScope
import life.furrer.nine.core.settings.SettingsScreen

/** K9's Settings, built like every Nine app's: its own sections first, then Theme, Backup and
 *  About from nine-core, and Updates.
 *
 *  The typing engine is Traditional T9's and reads its settings from the default
 *  SharedPreferences under its own keys, so these rows read and write exactly those keys - the
 *  engine needs no change to follow them. */
class SettingsActivity : NineSettingsActivity() {

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun buildAppSections(screen: SettingsScreen, surface: NineSurface) {
        screen.section("Keyboard") {
            action("K9", value = { if (isEnabled()) "on" else "off: turn on" }) {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            action("Default keyboard", value = ::defaultKeyboardLabel) {
                getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            }
        }
        screen.section("Predictive mode (T9)") {
            flag("Predictive mode", "pref_predictive_mode", true)
            flag("Automatic space", "auto_space_predictive", true)
            flag("Automatic capitals", "auto_text_case_predictive", true)
            flag("Capitals on every line", "auto_capitals_after_newline_predictive", false)
            flag("Learn popular words", "pref_predict_top_words", true)
            flag("Learn word pairs", "pref_predict_word_pairs", true)
            flag("Re-edit a word with Back", "backspace_recomposing", true)
        }
        screen.section("ABC mode") {
            flag("Automatic space", "auto_space_abc_v2", true)
            flag("Automatic capitals", "auto_text_case_abc_v2", true)
            flag("Capitals on every line", "auto_capitals_after_newline_abc", false)
            flag("Letter suggestions", "show_suggestions_abc", false)
        }
        screen.section("Typing") {
            flag("Fast delete", "backspace_acceleration", false)
            flag("Trim trailing spaces", "auto_trim_trailing_space", true)
        }
        screen.section("Mind reading") {
            flag("Next-word predictions", "auto_mind_reading_guess", false)
            flag("Autocomplete", "auto_mind_reading_complete", false)
            flag("Show completions last", "auto_mind_reading_sort_predictions_last", true)
            flag("In all text fields", "auto_mind_reading_in_all_fields", false)
        }
        screen.section("More") {
            // Until they're rebuilt on nine-core: Traditional T9's own screens for languages and
            // dictionaries, hotkeys, key characters, appearance and compatibility.
            action("Languages, hotkeys and the rest") {
                startActivity(Intent().setClassName(this@SettingsActivity, "io.github.sspanak.tt9.preferences.PreferencesActivity"))
            }
        }
        updatesSection(screen, this, ::refresh)
    }

    private fun SectionScope.flag(label: String, key: String, default: Boolean) = toggle(
        label = label,
        get = { prefs.getBoolean(key, default) },
        set = { prefs.edit().putBoolean(key, it).apply() },
        on = "on",
        off = "off",
    )

    private fun isEnabled(): Boolean =
        getSystemService(InputMethodManager::class.java).enabledInputMethodList.any { it.packageName == packageName }

    private fun defaultKeyboardLabel(): String {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        if (id.startsWith("$packageName/")) return "K9"
        val info = getSystemService(InputMethodManager::class.java).enabledInputMethodList.firstOrNull { it.id == id }
        return info?.loadLabel(packageManager)?.toString()?.let { "$it: switch to K9" } ?: "not K9: switch"
    }
}
