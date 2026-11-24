package io.github.ninetailsflip.k9

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class HotkeyCaptureTest {
    @Test
    fun `0 cancels, 2 clears, other digits are not allowed, OK is ignored`() {
        assertEquals(Capture.Cancel, capture(KeyEvent.KEYCODE_0, 0, held = false, isBackspaceFunction = false))
        assertEquals(Capture.Clear, capture(KeyEvent.KEYCODE_2, 2, held = false, isBackspaceFunction = false))
        assertEquals(Capture.NotAllowed, capture(KeyEvent.KEYCODE_5, 5, held = true, isBackspaceFunction = false))
        assertEquals(Capture.Ignore, capture(KeyEvent.KEYCODE_DPAD_CENTER, -1, held = false, isBackspaceFunction = false))
    }

    @Test
    fun `a held key is stored negative, except for backspace`() {
        assertEquals(Capture.Assign(KeyEvent.KEYCODE_POUND), capture(KeyEvent.KEYCODE_POUND, -1, held = false, isBackspaceFunction = false))
        assertEquals(Capture.Assign(-KeyEvent.KEYCODE_POUND), capture(KeyEvent.KEYCODE_POUND, -1, held = true, isBackspaceFunction = false))
        assertEquals(Capture.Assign(KeyEvent.KEYCODE_BACK), capture(KeyEvent.KEYCODE_BACK, -1, held = true, isBackspaceFunction = true))
    }
}
