package io.github.ninetailsflip.k9

import android.os.Looper
import android.view.KeyEvent
import io.github.sspanak.tt9.languages.LanguageCollection
import life.furrer.nine.core.ui.RowListView
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LanguagesActivityTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun list(activity: LanguagesActivity): RowListView {
        fun find(v: android.view.View): RowListView? = when (v) {
            is RowListView -> v
            is android.view.ViewGroup -> (0 until v.childCount).firstNotNullOfOrNull { find(v.getChildAt(it)) }
            else -> null
        }
        return find(activity.window.decorView)!!
    }

    /** The engine opens on a background thread: let it, and the main thread, finish. */
    private fun waitForRows(activity: LanguagesActivity) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (list(activity).rowCount() > 0) return
            Thread.sleep(50)
        }
    }

    @Test
    fun `every language is listed and OK turns the selected one on`() {
        val activity = Robolectric.buildActivity(LanguagesActivity::class.java).setup().get()
        waitForRows(activity)
        val rows = list(activity).rowCount()
        assertTrue("languages listed: $rows", rows > 20)

        val before = Engine.settings(context).enabledLanguageIds.toSet()
        val all = LanguageCollection.getAll(true).filterIsInstance<io.github.sspanak.tt9.languages.NaturalLanguage>()
        val target = all.indexOfFirst { it.id !in before }
        repeat(target) { activity.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)) }
        activity.onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))

        assertTrue(all[target].id in Engine.settings(context).enabledLanguageIds)
    }
}
