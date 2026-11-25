package io.github.ninetailsflip.k9

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import life.furrer.nine.core.ui.Dimens
import life.furrer.nine.core.ui.HintBar
import life.furrer.nine.core.ui.NineActivity
import life.furrer.nine.core.ui.Palette
import io.github.sspanak.tt9.hacks.InputType as Tt9InputType

/** Fields of every kind to try K9 in: numbers, phone, text, e-mail, password, web address.
 *  Up/Down move between them (K9 hands them on when the field is empty or at its edge); Back
 *  closes. Traditional T9's test screen in the Nine look: the fields carry its tag, so K9 shows
 *  what it detected about each field in its logs. */
class TryTypingActivity : NineActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val side = (Dimens.SIDE_PADDING_DP * resources.displayMetrics.density).toInt()
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, side)
            addView(
                TextView(this@TryTypingActivity).apply {
                    text = "Try typing"
                    textSize = Dimens.TITLE_SP
                    setTextColor(Color.WHITE)
                    setPadding(side, side, side, side / 2)
                },
            )
        }
        FIELDS.forEach { (label, type) ->
            list.addView(
                TextView(this).apply {
                    text = label
                    textSize = 12f
                    setTextColor(Palette.GRAY_MUTED)
                    setPadding(side, side / 2, side, 0)
                },
            )
            list.addView(
                EditText(this).apply {
                    inputType = type
                    isSingleLine = type and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0
                    privateImeOptions = Tt9InputType.OWN_TEST_FIELD_TAG
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setHintTextColor(Palette.GRAY_FAINT)
                    hint = "type here"
                    setPadding(side, side / 3, side, side / 3)
                    onFocusChangeListener = View.OnFocusChangeListener { v, focused ->
                        setBackgroundColor(if (focused) Palette.DIVIDER else Color.BLACK)
                        if (focused) v.requestRectangleOnScreen(android.graphics.Rect(0, 0, v.width, v.height))
                    }
                    setBackgroundColor(Color.BLACK)
                },
            )
        }
        val hintBar = HintBar(this)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.BLACK)
                addView(ScrollView(this@TryTypingActivity).apply { addView(list) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(hintBar)
            },
        )
        hintBar.setText("type · ↑↓ next field · ‹ back")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private companion object {
        val FIELDS = listOf(
            "Text" to InputType.TYPE_CLASS_TEXT,
            "Text, several lines" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            "E-mail" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
            "Password" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD),
            "Web address" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI),
            "Number" to InputType.TYPE_CLASS_NUMBER,
            "Number with sign and decimals" to (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL),
            "PIN" to (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD),
            "Phone number" to InputType.TYPE_CLASS_PHONE,
        )
    }
}
