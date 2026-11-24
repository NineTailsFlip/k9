package io.github.ninetailsflip.k9

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.preference.PreferenceManager
import io.github.sspanak.tt9.preferences.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsActivityTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun buildActivity(): SettingsActivity =
        Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

    private fun rows(activity: SettingsActivity): List<TextView> {
        val scrollView = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ScrollView
        val container = scrollView.getChildAt(0) as LinearLayout
        return (0 until container.childCount).map { container.getChildAt(it) as TextView }
    }

    private fun isSectionHeader(text: CharSequence) = text.isNotEmpty() && text.toString() == text.toString().uppercase()

    private fun press(activity: SettingsActivity, keyCode: Int) {
        activity.onKeyDown(keyCode, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0))
    }

    @Test
    fun `K9's sections come first, then the Nine suite's own`() {
        val headers = rows(buildActivity()).map { it.text.toString() }.filter(::isSectionHeader)
        assertEquals(
            listOf("KEYBOARD", "KEYS", "PREDICTIVE MODE (T9)", "ABC MODE", "TYPING", "MIND READING", "COMPATIBILITY", "MORE", "UPDATES", "THEME", "BACKUP", "ABOUT"),
            headers,
        )
    }

    @Test
    fun `a toggle writes the key Traditional T9's engine reads`() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().clear().commit()
        assertTrue(SettingsStore(context).predictiveMode)

        val activity = buildActivity()
        val selectable = rows(activity).filterNot { isSectionHeader(it.text) }
        val index = selectable.indexOfFirst { it.text.startsWith("Predictive mode") }
        assertTrue("Predictive mode row is there", index >= 0)
        repeat(index) { press(activity, KeyEvent.KEYCODE_DPAD_DOWN) }
        press(activity, KeyEvent.KEYCODE_DPAD_CENTER)

        assertFalse(SettingsStore(context).predictiveMode)
        assertTrue(rows(activity).any { it.text.startsWith("Predictive mode") && it.text.endsWith("off") })
    }
}
