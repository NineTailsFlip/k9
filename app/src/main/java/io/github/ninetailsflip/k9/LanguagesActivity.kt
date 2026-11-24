package io.github.ninetailsflip.k9

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.sspanak.tt9.BuildConfig
import io.github.sspanak.tt9.R
import io.github.sspanak.tt9.db.DataStore
import io.github.sspanak.tt9.db.customWords.DictionaryExporter
import io.github.sspanak.tt9.db.entities.WordFile
import io.github.sspanak.tt9.db.words.DictionaryDeleter
import io.github.sspanak.tt9.db.words.DictionaryLoader
import io.github.sspanak.tt9.languages.Language
import io.github.sspanak.tt9.languages.LanguageCollection
import io.github.sspanak.tt9.languages.NaturalLanguage
import io.github.sspanak.tt9.ui.notifications.DictionaryLoadingBar
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

/** The languages K9 types in, and their dictionaries: every language in one list, the ones that
 *  are on ticked. OK turns the selected one on or off; Left/Right jump to the previous/next first
 *  letter. `*` loads the dictionaries of the languages that are on (or cancels loading), exports
 *  them, deletes dictionaries, and leads to the words you added. Loading shows its progress in the
 *  line under the title. */
class LanguagesActivity : NineActivity() {

    private var languages: List<NaturalLanguage> = emptyList()
    private var shown: List<NaturalLanguage> = emptyList()
    private var enabled: MutableSet<Int> = mutableSetOf()
    private var loaded: Set<Int> = emptySet()
    private var details: Map<Int, String> = emptyMap()
    private var onlyEnabled = false
    private var busyText: String? = null

    private lateinit var subtitle: TextView
    private lateinit var status: TextView
    private lateinit var rowListView: RowListView
    private lateinit var hintBar: HintBar
    private lateinit var menuSheet: MenuSheet
    private val loadingBar by lazy { DictionaryLoadingBar.getInstance(this) }

    private val starKey = LongPressKey(::openMenu) { openNineSettings() }

    private val binder = object : RowBinder {
        override fun rowCount() = shown.size
        override fun createRowView(context: Context): View = TwoLineRowView(context)
        override fun bind(view: View, index: Int, selected: Boolean) {
            val language = shown[index]
            (view as TwoLineRowView).bind(language.name, details[language.id].orEmpty() + loadedSuffix(language), selected, language.id in enabled)
        }
        override fun rowHeight(index: Int) = TWO_LINE_ROW_DP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            textSize = 18f
            setTextColor(Palette.WHITE)
            setPadding(dp(Dimens.SIDE_PADDING_DP), dp(10), dp(Dimens.SIDE_PADDING_DP), 0)
            text = "Languages"
        }
        subtitle = TextView(this).apply {
            textSize = 12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Palette.GRAY_MUTED)
            setPadding(dp(Dimens.SIDE_PADDING_DP), 0, dp(Dimens.SIDE_PADDING_DP), dp(6))
        }
        rowListView = RowListView(this).apply { setBinder(binder) }
        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Palette.GRAY_DIM)
            text = "Loading…"
        }
        hintBar = HintBar(this)
        menuSheet = MenuSheet(this)
        val list = FrameLayout(this).apply {
            addView(rowListView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(status, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Palette.BLACK)
                addView(
                    LinearLayout(this@LanguagesActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(title)
                        addView(subtitle)
                        addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                        addView(hintBar)
                    },
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                addView(menuSheet, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            },
        )
        render()
        Engine.whenReady(this) { if (!isDestroyed) load() }
    }

    override fun onResume() {
        super.onResume()
        loadingBar.setOnStatusChange { runOnUiThread(::onLoadingStatus) }
        if (DictionaryLoader.isRunning()) onLoadingStatus()
    }

    override fun onPause() {
        super.onPause()
        loadingBar.setOnStatusChange(null)
        starKey.reset()
    }

    // ── Data ────────────────────────────────────────────────────────────────────────────────

    private fun load() {
        languages = LanguageCollection.getAll(true).filterIsInstance<NaturalLanguage>()
        enabled = Engine.settings(this).enabledLanguageIds.toMutableSet()
        details = languages.associate { it.id to describe(it) }
        applyFilter()
        status.visibility = View.GONE
        refreshLoaded()
    }

    /** "Русский · 400 000 words · 9 MB", as Traditional T9 described its languages. */
    private fun describe(language: NaturalLanguage): String {
        val file = WordFile(this, language, assets)
        return listOfNotNull(
            language.locale.displayLanguage.takeIf { it.isNotBlank() && it != language.name },
            file.getFormattedWords(getString(R.string.language_selection_words)),
            if (BuildConfig.LITE) file.formattedSize else null,
        ).joinToString(" · ")
    }

    /** Which languages have their dictionary in the database. */
    private fun refreshLoaded() {
        if (DictionaryLoader.isRunning()) return
        DataStore.exists({ ids -> runOnUiThread { loaded = ids.toSet(); rowListView.notifyDataChanged() } }, ArrayList<Language>(languages))
    }

    private fun loadedSuffix(language: NaturalLanguage) = if (language.id in loaded) " · loaded" else ""

    private fun applyFilter() {
        val keep = shown.getOrNull(rowListView.selectedIndex)?.id
        shown = if (onlyEnabled) languages.filter { it.id in enabled } else languages
        rowListView.notifyDataChanged()
        shown.indexOfFirst { it.id == keep }.takeIf { it >= 0 }?.let(rowListView::setSelectedIndex)
        render()
    }

    private fun enabledLanguages(): ArrayList<Language> = ArrayList(languages.filter { it.id in enabled })

    // ── Actions ─────────────────────────────────────────────────────────────────────────────

    private fun toggle(language: NaturalLanguage) {
        if (language.id in enabled) {
            if (enabled.size == 1) return say("at least one language stays on")
            enabled.remove(language.id)
        } else {
            enabled.add(language.id)
        }
        Engine.settings(this).saveEnabledLanguageIds(ArrayList(enabled))
        rowListView.notifyDataChanged()
        render()
    }

    private fun loadOrCancel() {
        if (DictionaryLoader.isRunning()) {
            DictionaryLoader.abort()
            busyText = null
            render()
            return
        }
        if (!DictionaryLoader.load(this, Engine.settings(this), enabledLanguages())) {
            DictionaryLoader.abort()
            say("couldn't start loading")
        }
        onLoadingStatus()
    }

    private fun onLoadingStatus() {
        busyText = when {
            DictionaryLoader.isRunning() || loadingBar.inProgress() -> "${loadingBar.title} ${loadingBar.message}".trim()
            else -> null
        }
        if (busyText == null) {
            when {
                loadingBar.isFailed -> say(loadingBar.message)
                loadingBar.isCancelled -> say("loading cancelled")
                else -> say("dictionaries loaded")
            }
            refreshLoaded()
        }
        render()
    }

    private fun export() {
        val exporter = DictionaryExporter.getInstance().setLanguages(enabledLanguages())
        exporter.setStartHandler { runOnUiThread { say("exporting…") } }
        exporter.setSuccessHandler { where -> runOnUiThread { say("exported to $where") } }
        exporter.setFailureHandler { runOnUiThread { say("export failed") } }
        if (!exporter.run(this)) say("an export is already running")
    }

    private fun delete(which: List<Language>, done: String) {
        if (which.isEmpty()) return say("nothing to delete")
        say("deleting…")
        DictionaryDeleter.getInstance(this).deleteLanguages(ArrayList(which)) {
            DictionaryLoader.clearAutoLoadNotificationCooldown(ArrayList(which))
            runOnUiThread { say(done); refreshLoaded() }
        }
    }

    private fun confirm(question: String, action: () -> Unit) {
        menuSheet.show(listOf(MenuItem(question) { action() }, MenuItem("Cancel") {}))
    }

    // ── Rendering and keys ──────────────────────────────────────────────────────────────────

    private fun say(text: String) = hintBar.setText(text)

    private fun render() {
        subtitle.text = busyText ?: "${enabled.size} on · ${loaded.count { it in enabled }} of them loaded"
        hintBar.setText(if (busyText != null) "* cancel loading · ‹ back" else "ok on/off · ←→ letter · * menu")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (menuSheet.handleKeyDown(keyCode, event)) return true
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> rowListView.moveSelection(-1)
            KeyEvent.KEYCODE_DPAD_DOWN -> rowListView.moveSelection(1)
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> jumpLetter(if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> shown.getOrNull(rowListView.selectedIndex)?.let(::toggle)
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STAR) {
            starKey.onUp()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /** To the first language of the previous/next initial letter. */
    private fun jumpLetter(direction: Int) {
        letterJump(shown.map { it.name }, rowListView.selectedIndex, direction)?.let(rowListView::setSelectedIndex)
    }

    private fun openMenu() {
        val items = mutableListOf<MenuItem>()
        if (DictionaryLoader.isRunning()) {
            items += MenuItem("Cancel loading") { loadOrCancel() }
        } else {
            items += MenuItem("Load dictionaries (${enabled.size} on)") { loadOrCancel() }
            items += MenuItem("Export dictionaries (${enabled.size} on)") { export() }
            items += MenuItem("Delete dictionaries of languages that are off…") {
                confirm("Delete dictionaries of languages that are off") {
                    delete(languages.filter { it.id !in enabled }, "deleted the dictionaries of languages that are off")
                }
            }
            items += MenuItem("Delete all dictionaries…") {
                confirm("Delete all dictionaries") { delete(languages, "deleted all dictionaries") }
            }
        }
        items += MenuItem(if (onlyEnabled) "Show all languages" else "Show only languages that are on") {
            onlyEnabled = !onlyEnabled
            applyFilter()
        }
        items += MenuItem("Settings") { openNineSettings() }
        menuSheet.show(items)
    }
}
