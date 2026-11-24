package io.github.ninetailsflip.k9

import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import io.github.sspanak.tt9.db.DataStore
import io.github.sspanak.tt9.languages.LanguageCollection
import life.furrer.nine.core.ui.RowListView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AddedWordsActivityTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun list(activity: AddedWordsActivity): RowListView {
        fun find(v: View): RowListView? = when (v) {
            is RowListView -> v
            is ViewGroup -> (0 until v.childCount).firstNotNullOfOrNull { find(v.getChildAt(it)) }
            else -> null
        }
        return find(activity.window.decorView)!!
    }

    /** The engine and the word database answer on background threads. */
    private fun waitUntil(condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(50)
        }
    }

    private fun press(activity: AddedWordsActivity, code: Int) {
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun count(): Long {
        var n = -1L
        DataStore.countCustomWords { n = it }
        waitUntil { n >= 0 }
        return n
    }

    @Test
    fun `an added word is listed, and OK then 1 deletes it`() {
        var ready = false
        Engine.whenReady(context) { ready = true }
        waitUntil { ready }
        var added = false
        DataStore.put({ added = true }, LanguageCollection.getDefault(), "Knine", false)
        waitUntil { added }
        assertEquals(1L, count())

        val activity = Robolectric.buildActivity(AddedWordsActivity::class.java).setup().get()
        waitUntil { list(activity).rowCount() == 1 }
        assertEquals(1, list(activity).rowCount())

        press(activity, KeyEvent.KEYCODE_DPAD_CENTER)
        press(activity, KeyEvent.KEYCODE_1)
        waitUntil { count() == 0L }
        assertEquals(0L, count())
    }
}
