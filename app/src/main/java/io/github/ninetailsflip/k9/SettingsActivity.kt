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
            action("Languages", value = ::languagesLabel) {
                startActivity(Intent(this@SettingsActivity, LanguagesActivity::class.java))
            }
        }
        screen.section("Keys") {
            action("Hotkeys") { startActivity(Intent(this@SettingsActivity, HotkeysActivity::class.java)) }
            flag("Hold a number to type it", "pref_hold_to_type", true)
            choice(
                "Ignore accidental repeats",
                "pref_key_pad_debounce_time",
                listOf("0" to "off") + listOf(20, 30, 50, 60, 75, 100, 150, 200, 250, 300, 350).map { "$it" to "$it ms" },
                default = { Engine.settings(this@SettingsActivity).keyPadDebounceTime.toString() },
            )
            flag("Show key presses", "pref_hardware_key_visual_feedback", false)
        }
        screen.section("Predictive mode (T9)") {
            flag("Predictive mode", "pref_predictive_mode", true)
            flag("Automatic space", "auto_space_predictive", true)
            flag("Automatic capitals", "auto_text_case_predictive", true)
            flag("Capitals on every line", "auto_capitals_after_newline_predictive", false)
            flag("Learn popular words", "pref_predict_top_words", true)
            flag("Learn word pairs", "pref_predict_word_pairs", true)
            flag("Re-edit a word with Back", "backspace_recomposing", true)
            choice(
                "0 pressed twice",
                "pref_double_zero_char",
                // Traditional T9 stores a new line as the two characters "\\n".
                listOf("." to ".", "," to ",", "\\n" to "new line", "" to "space"),
                default = { "." },
            )
        }
        screen.section("ABC mode") {
            flag("Automatic space", "auto_space_abc_v2", true)
            flag("Automatic capitals", "auto_text_case_abc_v2", true)
            flag("Capitals on every line", "auto_capitals_after_newline_abc", false)
            flag("Letter suggestions", "show_suggestions_abc", false)
            choice(
                "Pick the letter after",
                "pref_abc_auto_accept_time",
                listOf("-1" to "never", "350" to "0.35 s", "500" to "0.5 s", "800" to "0.8 s", "1200" to "1.2 s"),
                default = { "800" },
            )
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
        screen.section("Compatibility") {
            flag("Send with OK in chat apps", "hack_send_with_enter_in_chat_apps", false)
            flag("Show the word being typed", "pref_allow_composing_text", true)
            flag("Turn that off where it misbehaves", "hack_auto_disable_composing", true)
            flag("Status bar icon", "pref_status_icon", Engine.settings(this@SettingsActivity).isStatusIconEnabled)
        }
        screen.section("More") {
            // Until they're rebuilt on nine-core: Traditional T9's own screens for key characters,
            // emoji, added words, the on-screen keypad and diagnostics.
            action("Key characters, added words and the rest") {
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

    /** A Traditional T9 drop-down: [options] are (stored value, shown text); the value is a string
     *  in SharedPreferences, as its drop-downs keep it. */
    private fun SectionScope.choice(label: String, key: String, options: List<Pair<String, String>>, default: () -> String) = choice(
        label = label,
        options = { options.map { it.first } },
        get = { prefs.getString(key, null) ?: default() },
        set = { value -> prefs.edit().putString(key, value).apply() },
        render = { value -> options.firstOrNull { it.first == value }?.second ?: value },
    )

    private fun languagesLabel(): String {
        val ids = Engine.settings(this).enabledLanguageIds
        return if (ids.size == 1) "1 on" else "${ids.size} on"
    }

    private fun isEnabled(): Boolean =
        getSystemService(InputMethodManager::class.java).enabledInputMethodList.any { it.packageName == packageName }

    private fun defaultKeyboardLabel(): String {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        if (id.startsWith("$packageName/")) return "K9"
        val info = getSystemService(InputMethodManager::class.java).enabledInputMethodList.firstOrNull { it.id == id }
        return info?.loadLabel(packageManager)?.toString()?.let { "$it: switch to K9" } ?: "not K9: switch"
    }
}
