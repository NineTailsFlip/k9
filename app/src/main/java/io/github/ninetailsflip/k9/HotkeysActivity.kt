package io.github.ninetailsflip.k9

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.sspanak.tt9.R
import io.github.sspanak.tt9.commands.CmdBackspace
import io.github.sspanak.tt9.commands.CmdShift
import io.github.sspanak.tt9.commands.CmdSpaceKorean
import io.github.sspanak.tt9.commands.Command
import io.github.sspanak.tt9.commands.CommandCollection
import io.github.sspanak.tt9.commands.NullCommand
import io.github.sspanak.tt9.ime.helpers.Key
import life.furrer.nine.core.ui.Dimens
import life.furrer.nine.core.ui.HintBar
import life.furrer.nine.core.ui.MenuItem
import life.furrer.nine.core.ui.MenuSheet
import life.furrer.nine.core.ui.NineActivity
import life.furrer.nine.core.ui.Palette
import life.furrer.nine.core.ui.RowBinder
import life.furrer.nine.core.ui.RowListView
import life.furrer.nine.core.ui.dp

/** K9's functions and the keys that run them while typing, one per row. OK on a row waits for a
 *  key: press it, or hold it to assign "hold", as Traditional T9 does. 0 cancels the wait, 2
 *  clears the function's key. A key already used by another function asks first, in the numbered
 *  menu. `*` restores the default keys. */
class HotkeysActivity : NineActivity() {

    private val settings by lazy { Engine.settings(this) }
    private val commands: List<Command> by lazy { CommandCollection.getHotkeyCommands() }
    private var waitingFor: Command? = null
    /** The key whose DOWN started a hold (already assigned), so its UP is swallowed. */
    private var heldKey = KeyEvent.KEYCODE_UNKNOWN

    private lateinit var rowListView: RowListView
    private lateinit var hintBar: HintBar
    private lateinit var menuSheet: MenuSheet

    private val binder = object : RowBinder {
        override fun rowCount() = commands.size
        override fun createRowView(context: Context): View = TwoLineRowView(context)
        override fun bind(view: View, index: Int, selected: Boolean) {
            val command = commands[index]
            val waiting = waitingFor?.id == command.id
            (view as TwoLineRowView).bind(command.getName(this@HotkeysActivity).orEmpty(), if (waiting) "press or hold a key…" else keyName(command), selected, false)
        }
        override fun rowHeight(index: Int) = TWO_LINE_ROW_DP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            textSize = 18f
            setTextColor(Palette.WHITE)
            setPadding(dp(Dimens.SIDE_PADDING_DP), dp(10), dp(Dimens.SIDE_PADDING_DP), dp(6))
            text = "Hotkeys"
        }
        rowListView = RowListView(this).apply { setBinder(binder) }
        hintBar = HintBar(this)
        menuSheet = MenuSheet(this)
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Palette.BLACK)
                addView(
                    LinearLayout(this@HotkeysActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(title)
                        addView(rowListView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                        addView(hintBar)
                    },
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                addView(menuSheet, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            },
        )
        render()
    }

    private fun keyName(command: Command): String {
        val code = settings.getFunctionKey(command.id)
        if (code == KeyEvent.KEYCODE_UNKNOWN) return "none"
        val name = Key.codeToName(this, kotlin.math.abs(code))
        return if (code < 0) "$name ${getString(R.string.key_hold_key)}" else name
    }

    private fun render() {
        val waiting = waitingFor
        hintBar.setText(
            if (waiting != null) "press or hold a key · 0 cancel · 2 none" else "ok set key · * menu · ‹ back",
        )
        rowListView.notifyDataChanged()
    }

    // ── Capturing a key ─────────────────────────────────────────────────────────────────────

    private fun onCaptured(command: Command, keyCode: Int, held: Boolean) {
        when (val result = capture(keyCode, Key.codeToNumber(settings, keyCode), held, command.id == CmdBackspace.ID)) {
            Capture.Ignore -> return
            Capture.Cancel -> waitingFor = null
            Capture.Clear -> {
                settings.setFunctionKey(command.id, KeyEvent.KEYCODE_UNKNOWN)
                waitingFor = null
            }
            Capture.NotAllowed -> {
                hintBar.setText("digits type letters: choose another key · 0 cancel")
                return
            }
            is Capture.Assign -> {
                waitingFor = null
                assign(command, result.keyCode)
            }
        }
        render()
    }

    /** "Shift" and "Korean space" may share a key: Traditional T9 handles that itself. */
    private fun assign(command: Command, code: Int) {
        val other = CommandCollection.getByHotkey(settings, code)
        val sharable = setOf(command.id, other.id) == setOf(CmdShift.ID, CmdSpaceKorean.ID)
        if (other is NullCommand || other.id == command.id || sharable) {
            settings.setFunctionKey(command.id, code)
            return
        }
        val keyText = Key.codeToName(this, kotlin.math.abs(code))
        menuSheet.show(
            listOf(
                MenuItem("Move $keyText from \"${other.getName(this)}\" here") {
                    settings.setFunctionKey(other.id, KeyEvent.KEYCODE_UNKNOWN)
                    settings.setFunctionKey(command.id, code)
                    render()
                },
                MenuItem("Keep it there") {},
            ),
        )
    }

    // ── Keys ────────────────────────────────────────────────────────────────────────────────

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val command = waitingFor
        if (command == null || menuSheet.isShowing) return super.dispatchKeyEvent(event)
        // While waiting, every key is a candidate - Back, *, # and Menu included.
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 1) {
                heldKey = event.keyCode
                onCaptured(command, event.keyCode, held = true)
            }
            KeyEvent.ACTION_UP -> if (event.keyCode == heldKey) {
                heldKey = KeyEvent.KEYCODE_UNKNOWN
            } else {
                onCaptured(command, event.keyCode, held = false)
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (menuSheet.handleKeyDown(keyCode, event)) return true
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> rowListView.moveSelection(-1)
            KeyEvent.KEYCODE_DPAD_DOWN -> rowListView.moveSelection(1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                waitingFor = commands.getOrNull(rowListView.selectedIndex)
                // The OK that started the wait still has its UP to come: let capture() ignore it.
                render()
            }
            KeyEvent.KEYCODE_STAR -> menuSheet.show(
                listOf(
                    MenuItem("Restore the default keys") {
                        settings.setDefaultKeys()
                        render()
                        hintBar.setText("default keys restored")
                    },
                    MenuItem("Back to settings") { finish() },
                ),
            )
            KeyEvent.KEYCODE_BACK -> finish()
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }
}
