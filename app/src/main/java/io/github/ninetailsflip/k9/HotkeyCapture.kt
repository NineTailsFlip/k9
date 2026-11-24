package io.github.ninetailsflip.k9

import android.view.KeyEvent

/** What a key press means while the hotkeys screen waits for a key, by Traditional T9's rules:
 *  0 cancels, 2 clears the function's key, any other digit can't be a hotkey (digits type), OK is
 *  ignored (it's what started the wait), and anything else is assigned - held (a negative code,
 *  Traditional T9's convention for "hold") or pressed. Backspace works the same pressed or held,
 *  so it never gets a "hold" variant. */
sealed interface Capture {
    data object Cancel : Capture
    data object Clear : Capture
    data object NotAllowed : Capture
    data object Ignore : Capture
    data class Assign(val keyCode: Int) : Capture
}

/** [digit] is the digit the key types (Traditional T9's Key.codeToNumber), or -1 for none. */
fun capture(keyCode: Int, digit: Int, held: Boolean, isBackspaceFunction: Boolean): Capture = when {
    keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER -> Capture.Ignore
    digit == 0 -> Capture.Cancel
    digit == 2 -> Capture.Clear
    digit in 1..9 -> Capture.NotAllowed
    held && !isBackspaceFunction -> Capture.Assign(-keyCode)
    else -> Capture.Assign(keyCode)
}
