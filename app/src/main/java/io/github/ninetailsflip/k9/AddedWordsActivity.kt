package io.github.ninetailsflip.k9

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.sspanak.tt9.db.DataStore
import io.github.sspanak.tt9.db.entities.CustomWordFile
import io.github.sspanak.tt9.db.customWords.CustomWordsExporter
import io.github.sspanak.tt9.db.customWords.CustomWordsImporter
import io.github.sspanak.tt9.db.entities.CustomWord
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
import life.furrer.nine.core.ui.SearchField
import life.furrer.nine.core.ui.dp

private const val MAX_WORDS = 500
private const val REQUEST_IMPORT = 700
/** Up/Down/OK browse the list and `*` stays the menu while the search field has the keypad. */
private val RESERVED_KEYS = setOf(
    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_STAR,
)

/** The words you added to K9's dictionaries, newest searches first: type to search (the search
 *  field wakes on the first letter key, as in C9's searches), OK on a word offers to delete it,
 *  and `*` exports them all to a file or imports a file of words. */
class AddedWordsActivity : NineActivity() {

    private var words: List<CustomWord> = emptyList()
    private var total = 0L

    private lateinit var subtitle: TextView
    private lateinit var searchField: SearchField
    private lateinit var rowListView: RowListView
    private lateinit var hintBar: HintBar
    private lateinit var menuSheet: MenuSheet
    private val starKey = LongPressKey(::openMenu) { openNineSettings() }

    private val binder = object : RowBinder {
        override fun rowCount() = words.size
        override fun createRowView(context: Context): View = TwoLineRowView(context)
        override fun bind(view: View, index: Int, selected: Boolean) {
            val word = words[index]
            (view as TwoLineRowView).bind(word.word, word.language.name, selected, false)
        }
        override fun rowHeight(index: Int) = TWO_LINE_ROW_DP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            textSize = 18f
            setTextColor(Palette.WHITE)
            setPadding(dp(Dimens.SIDE_PADDING_DP), dp(10), dp(Dimens.SIDE_PADDING_DP), 0)
            text = "Added words"
        }
        subtitle = TextView(this).apply {
            textSize = 12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Palette.GRAY_MUTED)
            setPadding(dp(Dimens.SIDE_PADDING_DP), 0, dp(Dimens.SIDE_PADDING_DP), 0)
        }
        searchField = SearchField(this).apply {
            onQueryChanged = { search() }
            editText.apply {
                hint = "Search"
                textSize = 16f
                setTextColor(Color.WHITE)
                setHintTextColor(Palette.GRAY_FAINT)
                setPadding(dp(Dimens.SIDE_PADDING_DP), dp(6), dp(Dimens.SIDE_PADDING_DP), dp(6))
            }
        }
        rowListView = RowListView(this).apply { setBinder(binder) }
        hintBar = HintBar(this)
        menuSheet = MenuSheet(this)
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Palette.BLACK)
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                addView(
                    LinearLayout(this@AddedWordsActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(title)
                        addView(subtitle)
                        addView(searchField.editText)
                        addView(rowListView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                        addView(hintBar)
                    },
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                addView(menuSheet, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            },
        )
        hintBar.setText("type to search · ok delete · * menu")
        Engine.whenReady(this) { if (!isDestroyed) reload() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) searchField.onWindowFocusGained()
    }

    override fun onPause() {
        super.onPause()
        searchField.onPause()
        starKey.reset()
    }

    // ── Data ────────────────────────────────────────────────────────────────────────────────

    private fun reload() {
        DataStore.countCustomWords { count -> runOnUiThread { total = count; renderCount() } }
        search()
    }

    private fun search() {
        val query = searchField.query.trim()
        DataStore.getCustomWords({ found ->
            runOnUiThread {
                if (isDestroyed || query != searchField.query.trim()) return@runOnUiThread
                words = found
                rowListView.notifyDataChanged()
                renderCount()
            }
        }, query, MAX_WORDS)
    }

    private fun renderCount() {
        subtitle.text = when {
            total == 0L -> "none yet: add words from K9's menu (hold *, then 1)"
            searchField.query.isNotBlank() -> "${words.size} found of $total"
            else -> if (total == 1L) "1 word" else "$total words"
        }
    }

    // ── Actions ─────────────────────────────────────────────────────────────────────────────

    private fun offerDelete(word: CustomWord) {
        menuSheet.show(
            listOf(
                MenuItem("Delete \"${word.word}\"") {
                    DataStore.deleteCustomWord({ runOnUiThread { hintBar.setText("deleted \"${word.word}\""); reload() } }, word.language, word.word)
                },
                MenuItem("Keep it") {},
            ),
        )
    }

    private fun export() {
        val exporter = CustomWordsExporter.getInstance()
        exporter.setStartHandler { runOnUiThread { hintBar.setText("exporting…") } }
        exporter.setSuccessHandler { where -> runOnUiThread { hintBar.setText("exported to $where") } }
        exporter.setFailureHandler { runOnUiThread { hintBar.setText("export failed") } }
        if (!exporter.run(this)) hintBar.setText("an export is already running")
    }

    @Suppress("DEPRECATION") // Activity's own result API: Nine apps use no AndroidX activities.
    private fun pickImportFile() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(CustomWordFile.MIME_TYPE)
        startActivityForResult(pick, REQUEST_IMPORT)
    }

    @Deprecated("Activity's own result API, as in every Nine app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != REQUEST_IMPORT || resultCode != RESULT_OK || uri == null) return
        val importer = CustomWordsImporter.getInstance(this)
        importer.setStartHandler { runOnUiThread { subtitle.text = "importing…" } }
        importer.setProgressHandler { progress -> runOnUiThread { subtitle.text = "importing… ${progress.toInt()}%" } }
        importer.setSuccessHandler { message -> runOnUiThread { hintBar.setText(message.ifBlank { "imported" }); reload() } }
        importer.setFailureHandler { message: String? -> runOnUiThread { hintBar.setText(message ?: "import failed"); reload() } }
        importer.setCancelHandler { runOnUiThread { hintBar.setText("import cancelled"); reload() } }
        importer.run(this, Engine.settings(this), CustomWordFile(uri, contentResolver))
    }

    private fun openMenu() {
        val items = mutableListOf<MenuItem>()
        words.getOrNull(rowListView.selectedIndex)?.let { word -> items += MenuItem("Delete \"${word.word}\"…") { offerDelete(word) } }
        items += MenuItem("Export all added words") { export() }
        items += MenuItem("Import words from a file") { pickImportFile() }
        if (searchField.query.isNotEmpty()) items += MenuItem("Clear the search") { searchField.clear() }
        items += MenuItem("Settings") { openNineSettings() }
        menuSheet.show(items)
    }

    // ── Keys ────────────────────────────────────────────────────────────────────────────────

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (menuSheet.interceptKeyEvent(event)) return true
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (searchField.handleBack(event) { super.dispatchKeyEvent(it) }) return true
            if (searchField.query.isNotEmpty()) searchField.clear() else finish()
            return true
        }
        if (searchField.handleKeyEvent(event, RESERVED_KEYS)) {
            if (event.keyCode == KeyEvent.KEYCODE_STAR) {
                if (event.action == KeyEvent.ACTION_DOWN) starKey.onDown(event.repeatCount) else if (event.action == KeyEvent.ACTION_UP) starKey.onUp()
            } else if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> rowListView.moveSelection(-1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> rowListView.moveSelection(1)
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> words.getOrNull(rowListView.selectedIndex)?.let(::offerDelete)
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
