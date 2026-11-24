package io.github.ninetailsflip.k9

import android.view.KeyEvent
import io.github.sspanak.tt9.commands.CommandCollection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HotkeysActivityTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun key(activity: HotkeysActivity, action: Int, code: Int, repeat: Int = 0) =
        activity.dispatchKeyEvent(KeyEvent(0, 0, action, code, repeat))

    @Test
    fun `OK on a function, then holding a key, assigns "hold" that key`() {
        val settings = Engine.settings(context)
        settings.setDefaultKeys()
        val activity = Robolectric.buildActivity(HotkeysActivity::class.java).setup().get()
        val first = CommandCollection.getHotkeyCommands().first()

        key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)
        key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER)
        key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL)
        key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL, repeat = 1)
        key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL)

        assertEquals(-KeyEvent.KEYCODE_CALL, settings.getFunctionKey(first.id))
    }
}
