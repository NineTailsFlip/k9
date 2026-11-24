package io.github.ninetailsflip.k9

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.sspanak.tt9.R
import io.github.sspanak.tt9.languages.Language
import io.github.sspanak.tt9.languages.LanguageCollection
import life.furrer.nine.core.settings.openNineSettings
import life.furrer.nine.core.ui.Dimens
import life.furrer.nine.core.ui.HintBar
import life.furrer.nine.core.ui.LongPressKey
import life.furrer.nine.core.ui.MenuItem
import life.furrer.nine.core.ui.MenuSheet
import life.furrer.nine.core.ui.NineActivity
import life.furrer.nine.core.ui.Palette
import life.furrer.nine.core.ui.RowBinder
import life.furrer.nine.core.ui.RowListView
import life.furrer.nine.core.ui.dp
import life.furrer.nine.core.ui.relaunch

/** Which characters 0, 1 and (for alphabets) 2-9 type, per language - Traditional T9's "Key
 *  characters" as a Nine list. The first row picks the language with Left/Right; the next two
 *  turn new line and tab on 0 on or off; then one row per list of characters. OK on a list opens
 *  a field under the list: OK saves it (checked by Traditional T9's rules), Back on an empty
 *  field cancels. `*` restores the language's defaults. */
class KeyCharsActivity : NineActivity() {

    private sealed interface Row {
        data object LanguageRow : Row
        data object NewLine : Row
        data object Tab : Row
        data class Chars(val list: CharList) : Row
        data class Letters(val key: Int) : Row
    }

    private val settings by lazy { Engine.settings(this) }
    private var languages: List<Language> = emptyList()
    private var language: Language? = null
    private var rows: List<Row> = emptyList()
    private var editing: Row? = null

    private lateinit var rowListView: RowListView
    private lateinit var field: EditText
    private lateinit var hintBar: HintBar
    private lateinit var menuSheet: MenuSheet
    private val starKey = LongPressKey(::openMenu) { openNineSettings() }

    private val binder = object : RowBinder {
        override fun rowCount() = rows.size
        override fun createRowView(context: Context): View = TwoLineRowView(context)
        override fun bind(view: View, index: Int, selected: Boolean) {
            val (title, meta) = describe(rows[index])
            (view as TwoLineRowView).bind(title, meta, selected, false)
        }
        override fun rowHeight(index: Int) = TWO_LINE_ROW_DP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            textSize = 18f
            setTextColor(Palette.WHITE)
            setPadding(dp(Dimens.SIDE_PADDING_DP), dp(10), dp(Dimens.SIDE_PADDING_DP), dp(6))
            text = "Key characters"
        }
        rowListView = RowListView(this).apply { setBinder(binder) }
        field = EditText(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Palette.BLACK)
            setPadding(dp(Dimens.SIDE_PADDING_DP), dp(8), dp(Dimens.SIDE_PADDING_DP), dp(8))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
            visibility = View.GONE
            // Not focusable until a list is being edited: a focusable field in a fresh window gets
            // the IME's connection at once, and the key that opened the screen leaks into it.
            isFocusable = false
            setOnEditorActionListener { _, _, _ -> save(); true }
        }
        hintBar = HintBar(this)
        menuSheet = MenuSheet(this)
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Palette.BLACK)
                addView(
                    LinearLayout(this@KeyCharsActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(title)
                        addView(rowListView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                        addView(field)
                        addView(hintBar)
                    },
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                addView(menuSheet, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            },
        )
        render()
        Engine.whenReady(this) {
            if (isDestroyed) return@whenReady
            languages = LanguageCollection.getAll(settings.enabledLanguageIds, true)
            val wanted = intent.getIntExtra(EXTRA_LANGUAGE, -1)
            showLanguage(languages.firstOrNull { it.id == wanted } ?: languages.firstOrNull())
            rowListView.setSelectedIndex(intent.getIntExtra(EXTRA_ROW, 0))
            render()
        }
    }

    override fun onPause() {
        super.onPause()
        starKey.reset()
    }

    // ── Rows ────────────────────────────────────────────────────────────────────────────────

    private fun showLanguage(next: Language?) {
        language = next
        if (next != null) settings.setDefaultChars(next, false)
        rows = buildList {
            add(Row.LanguageRow)
            add(Row.NewLine)
            add(Row.Tab)
            CharList.entries.forEach { add(Row.Chars(it)) }
            if (next != null && !next.isTranscribed) (2..9).forEach { add(Row.Letters(it)) }
        }
        rowListView.notifyDataChanged()
    }

    private fun describe(row: Row): Pair<String, String> {
        val lang = language ?: return "Language" to "none on"
        return when (row) {
            Row.LanguageRow -> "Language" to "‹ ${lang.name} ›"
            Row.NewLine -> "New line on 0" to onOff(settings.getIncludeNewlineInChars0(lang))
            Row.Tab -> "Tab on 0" to onOff(settings.getIncludeTabInChars0(lang))
            is Row.Chars -> row.list.title to if (editing == row) "editing below…" else showChars(current(row.list, lang))
            is Row.Letters -> "${row.key} · extra letters" to if (editing == row) "editing below…" else showChars(settings.getCharsExtra(lang, CharList.letterKey(row.key)))
        }
    }

    private fun onOff(on: Boolean) = if (on) "on" else "off"

    /** What the list's field starts with: for 0, without the space, new line and tab it always has. */
    private fun current(list: CharList, lang: Language): String = when (list) {
        CharList.ZERO -> settings.getChars0(lang).filterNot { it in ALWAYS_ON_ZERO }
        CharList.ONE -> settings.getChars1(lang)
        else -> settings.getCharsExtra(lang, list.extraKey!!)
    }

    // ── Editing ─────────────────────────────────────────────────────────────────────────────

    private fun startEditing(row: Row) {
        val lang = language ?: return
        editing = row
        field.setText(
            when (row) {
                is Row.Chars -> current(row.list, lang)
                is Row.Letters -> settings.getCharsExtra(lang, CharList.letterKey(row.key))
                else -> return
            },
        )
        field.visibility = View.VISIBLE
        field.isFocusable = true
        field.isFocusableInTouchMode = true
        field.requestFocus()
        field.setSelection(field.text.length)
        render()
    }

    private fun save() {
        val lang = language ?: return
        val row = editing ?: return
        val text = field.text.toString()
        val result = when (row) {
            is Row.Chars -> checkChars(row.list, text, settings.getMandatoryChars0(lang).toList())
            is Row.Letters -> checkLetters(text)
            else -> return
        }
        result.onFailure { e ->
            hintBar.setText(problemText((e as CharsProblemException).problem))
            return
        }
        val chars = result.getOrThrow()
        when (row) {
            is Row.Chars -> when (row.list) {
                CharList.ZERO -> settings.saveChars0(lang, zeroWithFixedChars(lang) + chars)
                CharList.ONE -> settings.saveChars1(lang, chars)
                else -> settings.saveCharsExtra(lang, row.list.extraKey!!, chars)
            }
            is Row.Letters -> settings.saveCharsExtra(lang, CharList.letterKey(row.key), chars)
            else -> {}
        }
        stopEditing()
    }

    /** Space always, then new line and tab when they're on: what 0's list starts with. */
    private fun zeroWithFixedChars(lang: Language): String = buildString {
        for (c in ALWAYS_ON_ZERO) {
            when (c) {
                '\n' -> if (settings.getIncludeNewlineInChars0(lang)) append(c)
                '\t' -> if (settings.getIncludeTabInChars0(lang)) append(c)
                else -> append(c)
            }
        }
    }

    /** A fresh window, not just a cleared focus: the IME keeps `*` and `#` while it holds a
     *  connection to the field (core's relaunch, as Nine apps do after typing). */
    private fun stopEditing() {
        editing = null
        relaunch(intentFor(this, language?.id ?: -1, rowListView.selectedIndex))
    }

    private fun problemText(problem: CharsProblem): String = when (problem) {
        is CharsProblem.Forbidden -> getString(
            if (problem.chars.size > 1) R.string.key_chars_error_forbidden_chars else R.string.key_chars_error_forbidden_char,
            problem.chars.joinToString(", ") { charName(it) },
        )
        is CharsProblem.Missing -> getString(
            if (problem.chars.size > 1) R.string.key_chars_error_mandatory_chars_missing else R.string.key_chars_error_mandatory_char_missing,
            problem.chars.joinToString(", ") { charName(it) },
        )
        CharsProblem.Empty -> getString(R.string.key_chars_error_cannot_be_empty)
        CharsProblem.NotLetters -> getString(R.string.key_chars_error_only_letters_allowed)
        CharsProblem.Repeating -> getString(R.string.key_chars_error_repeating_letters_not_allowed)
    }

    private fun charName(c: Char) = when (c) {
        '\n' -> getString(R.string.char_newline)
        ' ' -> getString(R.string.char_space)
        '\t' -> "tab"
        else -> c.toString()
    }

    // ── Keys ────────────────────────────────────────────────────────────────────────────────

    private fun render() {
        hintBar.setText(
            when {
                editing != null -> "type · ok save · ‹ on empty: cancel"
                rows.getOrNull(rowListView.selectedIndex) == Row.LanguageRow -> "←→ language · * menu · ‹ back"
                else -> "ok change · ←→ language on the first row · * menu"
            },
        )
        rowListView.notifyDataChanged()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (editing != null && event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN && field.text.isEmpty()) {
            stopEditing()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (editing != null) return super.onKeyDown(keyCode, event)
        if (menuSheet.handleKeyDown(keyCode, event)) return true
        val row = rows.getOrNull(rowListView.selectedIndex)
        val lang = language
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> rowListView.moveSelection(-1)
            KeyEvent.KEYCODE_DPAD_DOWN -> rowListView.moveSelection(1)
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> when (row) {
                Row.LanguageRow -> if (languages.isNotEmpty()) {
                    val step = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                    val at = languages.indexOfFirst { it.id == lang?.id }
                    showLanguage(languages[(at + step + languages.size) % languages.size])
                }
                Row.NewLine -> lang?.let { settings.setIncludeNewlineInChars0(it, !settings.getIncludeNewlineInChars0(it)); resave0(it) }
                Row.Tab -> lang?.let { settings.setIncludeTabInChars0(it, !settings.getIncludeTabInChars0(it)); resave0(it) }
                is Row.Chars, is Row.Letters -> if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) startEditing(row)
                null -> {}
            }
            KeyEvent.KEYCODE_STAR -> {
                starKey.onDown(event.repeatCount)
                return true
            }
            KeyEvent.KEYCODE_BACK -> finish()
            else -> return super.onKeyDown(keyCode, event)
        }
        render()
        return true
    }

    /** New line and tab live at the start of 0's list: re-save it so the switch takes effect. */
    private fun resave0(lang: Language) {
        settings.saveChars0(lang, zeroWithFixedChars(lang) + current(CharList.ZERO, lang))
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (editing == null && keyCode == KeyEvent.KEYCODE_STAR) {
            starKey.onUp()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun openMenu() {
        val lang = language ?: return
        menuSheet.show(
            listOf(
                MenuItem("Restore the defaults for ${lang.name}") {
                    settings.setDefaultChars(lang, true)
                    hintBar.setText("defaults restored")
                    render()
                },
                MenuItem("Settings") { openNineSettings() },
            ),
        )
    }

    companion object {
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_ROW = "row"

        fun intentFor(context: Context, languageId: Int = -1, row: Int = 0): Intent =
            Intent(context, KeyCharsActivity::class.java).putExtra(EXTRA_LANGUAGE, languageId).putExtra(EXTRA_ROW, row)
    }
}
