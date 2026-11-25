package io.github.ninetailsflip.k9

import android.content.Intent
import android.view.KeyEvent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import android.content.ClipData
import android.content.ClipboardManager
import io.github.sspanak.tt9.db.DataStore
import io.github.sspanak.tt9.db.customWords.LogcatExporter
import io.github.sspanak.tt9.db.mindreader.MindReaderStore
import io.github.sspanak.tt9.db.words.SlowQueryStats
import io.github.sspanak.tt9.ime.mindreader.MindReader
import io.github.sspanak.tt9.languages.LanguageCollection
import io.github.sspanak.tt9.preferences.custom.EnhancedDropDownPreference
import io.github.sspanak.tt9.preferences.screens.debug.DropDownInputHandlingMode
import io.github.sspanak.tt9.preferences.screens.debug.DropDownLogLevel
import io.github.sspanak.tt9.ui.notifications.DictionaryProgressNotification
import io.github.sspanak.tt9.util.Logger
import io.github.sspanak.tt9.util.sys.DeviceInfo
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownAlignment
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownBottomPaddingPortrait
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownKeyHeight
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownLayoutType
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownNumpadFnKeyScale
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownNumpadKeyFontSize
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownNumpadShape
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownSuggestionFontSize
import io.github.sspanak.tt9.preferences.screens.appearance.DropDownWidth
import io.github.sspanak.tt9.preferences.screens.keypad.DropDownKeyPadDebounceTime
import io.github.sspanak.tt9.preferences.screens.keypad.DropDownOneKeyEmoji
import io.github.sspanak.tt9.preferences.screens.modeAbc.DropDownAbcAutoAcceptTime
import io.github.sspanak.tt9.preferences.screens.modePredictive.DropDownZeroKeyCharacter
import io.github.sspanak.tt9.preferences.settings.SettingsUI
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
    private val settings by lazy { Engine.settings(this) }

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
            action("Added words") { startActivity(Intent(this@SettingsActivity, AddedWordsActivity::class.java)) }
            action("Key characters") { startActivity(KeyCharsActivity.intentFor(this@SettingsActivity)) }
            flag("Add words without asking", "add_word_no_confirmation", settings.addWordsNoConfirmation)
            flag("Language key switches at once", "pref_quick_switch_language", settings.quickSwitchLanguage)
            flag("Import big word lists", "pref_raise_custom_words_import_limits", settings.raiseImportLimits)
        }
        screen.section("Keys") {
            action("Hotkeys") { startActivity(Intent(this@SettingsActivity, HotkeysActivity::class.java)) }
            flag("Hold a number to type it", "pref_hold_to_type", true)
            dropDown("Ignore accidental repeats", DropDownKeyPadDebounceTime(this@SettingsActivity).populate(settings), default = { settings.keyPadDebounceTime.toString() })
            flag("Show key presses", "pref_hardware_key_visual_feedback", false)
            flag("Keys upside down (1 2 3 at the bottom)", "pref_upside_down_keys", settings.upsideDownKeys)
            flag("Vibrate on key presses", "pref_haptic_feedback", settings.hapticFeedback)
            dropDown("Emoji on one key", DropDownOneKeyEmoji(this@SettingsActivity).populate(settings))
        }
        screen.section("Predictive mode (T9)") {
            flag("Predictive mode", "pref_predictive_mode", true)
            flag("Automatic space", "auto_space_predictive", true)
            flag("Automatic capitals", "auto_text_case_predictive", true)
            flag("Capitals on every line", "auto_capitals_after_newline_predictive", false)
            flag("Learn popular words", "pref_predict_top_words", true)
            flag("Learn word pairs", "pref_predict_word_pairs", true)
            flag("Re-edit a word with Back", "backspace_recomposing", true)
            dropDown("0 pressed twice", DropDownZeroKeyCharacter(this@SettingsActivity).populate(settings))
        }
        screen.section("ABC mode") {
            flag("Automatic space", "auto_space_abc_v2", true)
            flag("Automatic capitals", "auto_text_case_abc_v2", true)
            flag("Capitals on every line", "auto_capitals_after_newline_abc", false)
            flag("Letter suggestions", "show_suggestions_abc", false)
            dropDown("Pick the letter after", DropDownAbcAutoAcceptTime(this@SettingsActivity).populate(settings))
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
            flag("Status bar icon", "pref_status_icon", settings.isStatusIconEnabled)
        }
        screen.section("On-screen keypad") {
            dropDown(
                "Layout",
                DropDownLayoutType(this@SettingsActivity).populate(settings),
                onSet = { v -> v.toIntOrNull()?.let { if (it == SettingsUI.LAYOUT_CLASSIC || it == SettingsUI.LAYOUT_NUMPAD) settings.setPreferredLargeLayout(it) } },
            )
            dropDown("Width", DropDownWidth(this@SettingsActivity).populate(settings))
            dropDown("Alignment", DropDownAlignment(this@SettingsActivity).populate(settings))
            dropDown("Key height", DropDownKeyHeight(this@SettingsActivity).populate(settings))
            dropDown("Key text size", DropDownNumpadKeyFontSize(this@SettingsActivity).populate(settings))
            dropDown("Side key width", DropDownNumpadFnKeyScale(this@SettingsActivity).populate(settings))
            dropDown("Key shape", DropDownNumpadShape(this@SettingsActivity).populate(settings))
            dropDown("Suggestion text size", DropDownSuggestionFontSize(this@SettingsActivity).populate(settings))
            dropDown("Space below", DropDownBottomPaddingPortrait(this@SettingsActivity).populate(settings))
            flag("Up and down arrows", "pref_arrows_up_down", settings.arrowsUpDown)
            flag("Key shadows", "pref_key_shadows", settings.keyShadows)
            flag("Smooth suggestion scrolling", "pref_suggestion_smooth_scroll", settings.suggestionSmoothScroll)
        }
        diagnosticsSection(screen)
        screen.section("Help") {
            action("Manual") { startActivity(Intent().setClassName(this@SettingsActivity, "io.github.sspanak.tt9.preferences.HelpActivity")) }
            action("Privacy policy") { startActivity(Intent().setClassName(this@SettingsActivity, "io.github.sspanak.tt9.preferences.PrivacyPolicyActivity")) }
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

    /** Status shown beside a Diagnostics action until the screen is left: "exporting…", "done". */
    private val statuses = mutableMapOf<String, String>()
    /** The destructive action waiting for its second OK. */
    private var armed: String? = null

    private fun setStatus(label: String, status: String) = runOnUiThread {
        statuses[label] = status
        if (!isDestroyed) refresh()
    }

    /** An action that forgets something for good: the first OK only asks, the second does it. */
    private fun SectionScope.forget(label: String, run: () -> Unit) = action(
        label,
        value = { if (armed == label) "OK again to confirm" else statuses[label].orEmpty() },
    ) {
        if (armed == label) {
            armed = null
            run()
        } else {
            armed = label
        }
    }

    /** Moving off a "Forget…" row takes back its first OK. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_ENTER) armed = null
        return super.onKeyDown(keyCode, event)
    }

    private fun diagnosticsSection(screen: SettingsScreen) = screen.section("Diagnostics") {
        dropDown("Log level", DropDownLogLevel(this@SettingsActivity), onSet = { v -> v.toIntOrNull()?.let(Logger::setLevel) })
        flag("Include system logs", "pref_enable_system_logs", settings.systemLogs)
        val exportLabel = "Export logs"
        action(exportLabel, value = { statuses[exportLabel].orEmpty() }) {
            val exporter = LogcatExporter.getInstance().setIncludeSystemLogs(settings.systemLogs)
            exporter.setStartHandler { setStatus(exportLabel, "exporting…") }
            exporter.setSuccessHandler { where ->
                DictionaryProgressNotification.getInstance(this@SettingsActivity).hide()
                setStatus(exportLabel, "saved to $where")
            }
            exporter.setFailureHandler { setStatus(exportLabel, "export failed") }
            if (!exporter.run(this@SettingsActivity)) setStatus(exportLabel, "already exporting")
        }
        action("View logs") { startActivity(Intent().setClassName(this@SettingsActivity, "io.github.sspanak.tt9.preferences.LogsActivity")) }
        action("Try typing") { startActivity(Intent().setClassName(this@SettingsActivity, "io.github.sspanak.tt9.preferences.screens.debug.TestInputActivity")) }
        val deviceLabel = "Copy device info"
        action(deviceLabel, value = { statuses[deviceLabel].orEmpty() }) {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("K9 device info", DeviceInfo().toString()))
            setStatus(deviceLabel, "copied")
        }
        dropDown("Key handling", DropDownInputHandlingMode(this@SettingsActivity))
        val cachesLabel = "Clear the caches"
        action(cachesLabel, value = { statuses[cachesLabel].orEmpty() }) {
            MindReader.clear()
            DataStore.clearWordPairCache()
            SlowQueryStats.clear()
            setStatus(cachesLabel, "cleared")
        }
        val pairsLabel = "Forget learned word pairs"
        forget(pairsLabel) {
            setStatus(pairsLabel, "forgetting…")
            DataStore.deleteWordPairs(LanguageCollection.getAll()) { setStatus(pairsLabel, "forgotten") }
        }
        val mindLabel = "Forget mind reading"
        forget(mindLabel) {
            setStatus(mindLabel, "forgetting…")
            val languages = LanguageCollection.getAll()
            MindReaderStore(applicationContext).truncate(languages) {
                languages.forEach { settings.setMindReaderFactoryNgramsRevision(it, "") }
                setStatus(mindLabel, "forgotten")
            }
        }
    }

    /** One of Traditional T9's own drop-downs, already populated: its options, their names and
     *  its current value (or [default] where it keeps none) come from it, so they always match
     *  what the engine reads. Values are strings in SharedPreferences under the drop-down's key. */
    private fun SectionScope.dropDown(
        label: String,
        dropDown: EnhancedDropDownPreference,
        default: (() -> String)? = null,
        onSet: (String) -> Unit = {},
    ) {
        val values = dropDown.entryValues.orEmpty().map { it.toString() }
        val names = dropDown.entries.orEmpty().map { it.toString() }
        val initial = dropDown.value
        val key = dropDown.key
        choice(
            label = label,
            options = { values },
            get = { prefs.getString(key, null) ?: default?.invoke() ?: initial ?: values.firstOrNull() },
            set = { value -> if (value != null) { prefs.edit().putString(key, value).apply(); onSet(value) } },
            render = { value -> names.getOrNull(values.indexOf(value)) ?: value },
        )
    }

    private fun languagesLabel(): String {
        val ids = settings.enabledLanguageIds
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
