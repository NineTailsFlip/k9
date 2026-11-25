package io.github.ninetailsflip.k9

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.sspanak.tt9.db.customWords.LogcatExporter
import io.github.sspanak.tt9.preferences.HelpFile
import io.github.sspanak.tt9.preferences.PrivacyPolicyFile
import io.github.sspanak.tt9.util.AssetFile
import life.furrer.nine.core.ui.Dimens
import life.furrer.nine.core.ui.HintBar
import life.furrer.nine.core.ui.NineActivity
import life.furrer.nine.core.ui.Palette
import life.furrer.nine.core.util.Async
import java.util.Locale

/** A page to read, the way nine-core's Manual pages look: a title over text that Up/Down scroll
 *  and Left/Right page through; Back closes it. It shows Traditional T9's manual and privacy
 *  policy (bundled with K9, in the system language where there's a translation) and K9's recent
 *  logs, in place of Traditional T9's web-view pages. */
class TextPageActivity : NineActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var body: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = Page.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_PAGE) } ?: Page.MANUAL
        val side = (Dimens.SIDE_PADDING_DP * resources.displayMetrics.density).toInt()
        body = TextView(this).apply {
            textSize = if (page == Page.LOGS) 11f else Dimens.ROW_SP
            setTextColor(Palette.GRAY_DIM)
            setLineSpacing(6f, 1f)
            setPadding(side, 0, side, side)
            if (page == Page.LOGS) typeface = Typeface.MONOSPACE
            text = "loading…"
        }
        scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = false
            addView(
                LinearLayout(this@TextPageActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        TextView(this@TextPageActivity).apply {
                            text = page.title
                            textSize = Dimens.TITLE_SP
                            setTextColor(Color.WHITE)
                            setPadding(side, side, side, side / 2)
                        },
                    )
                    addView(body)
                },
            )
        }
        val hintBar = HintBar(this)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.BLACK)
                addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(hintBar)
            },
        )
        hintBar.setText("↑↓ scroll · ←→ page · ‹ back")
        Async.run(
            background = { load(page) },
            onResult = { text -> if (!isDestroyed) body.text = text },
        )
    }

    private fun load(page: Page): CharSequence = when (page) {
        Page.MANUAL -> {
            val language = Locale.getDefault().language
            html(HelpFile(this, language).takeIf { it.exists() } ?: HelpFile(this))
        }
        Page.PRIVACY -> "$K9_PRIVACY\n\n".let { intro -> android.text.SpannableStringBuilder(intro).append(html(PrivacyPolicyFile(this))) }
        Page.LOGS -> LogcatExporter.getLogs(Engine.settings(this).systemLogs).ifBlank { "No logs yet." }
    }

    private fun html(file: AssetFile): CharSequence = try {
        Html.fromHtml(file.reader.use { it.readText() }, Html.FROM_HTML_MODE_COMPACT).trim()
    } catch (e: Exception) {
        "This page isn't included in this build of K9."
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val line = body.lineHeight
        val page = (scrollView.height - line * 2).coerceAtLeast(line)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> scrollView.smoothScrollBy(0, -line * 3)
            KeyEvent.KEYCODE_DPAD_DOWN -> scrollView.smoothScrollBy(0, line * 3)
            KeyEvent.KEYCODE_DPAD_LEFT -> scrollView.smoothScrollBy(0, -page)
            KeyEvent.KEYCODE_DPAD_RIGHT -> scrollView.smoothScrollBy(0, page)
            KeyEvent.KEYCODE_BACK -> finish()
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    enum class Page(val title: String) {
        MANUAL("Manual"),
        PRIVACY("Privacy policy"),
        LOGS("Logs"),
    }

    companion object {
        private const val EXTRA_PAGE = "page"

        /** K9's own addition to Traditional T9's policy below it. */
        const val K9_PRIVACY =
            "K9 is NineTailsFlip's edition of Traditional T9. On top of what follows, it checks " +
                "github.com/NineTailsFlip/k9 once a day for a newer release while update checks " +
                "are on (Settings → Updates), and downloads one only when you choose to install it. " +
                "Nothing about you or what you type is sent."

        fun intentFor(context: Context, page: Page): Intent =
            Intent(context, TextPageActivity::class.java).putExtra(EXTRA_PAGE, page.name)
    }
}
