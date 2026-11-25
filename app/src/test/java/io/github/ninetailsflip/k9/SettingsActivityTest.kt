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
            listOf(
                "KEYBOARD", "KEYS", "PREDICTIVE MODE (T9)", "ABC MODE", "TYPING", "MIND READING", "COMPATIBILITY",
                "ON-SCREEN KEYPAD", "DIAGNOSTICS", "HELP", "UPDATES", "THEME", "BACKUP", "ABOUT",
            ),
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

    private fun select(activity: SettingsActivity, label: String) {
        val selectable = rows(activity).filterNot { isSectionHeader(it.text) }
        val index = selectable.indexOfFirst { it.text.startsWith(label) }
        assertTrue("$label row is there", index >= 0)
        repeat(index) { press(activity, KeyEvent.KEYCODE_DPAD_DOWN) }
    }

    private fun rowText(activity: SettingsActivity, label: String) =
        rows(activity).first { it.text.startsWith(label) }.text.toString()

    @Test
    fun `a drop-down offers Traditional T9's own options and writes its key`() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        val activity = buildActivity()
        select(activity, "0 pressed twice")
        val before = SettingsStore(context).doubleZeroChar
        press(activity, KeyEvent.KEYCODE_DPAD_RIGHT)
        val after = SettingsStore(context).doubleZeroChar
        assertTrue("the engine reads a new value ($before -> $after)", before != after)
        assertFalse(rowText(activity, "0 pressed twice").endsWith(": "))
    }

    @Test
    fun `forgetting word pairs takes a second OK, and moving away takes the first back`() {
        val activity = buildActivity()
        select(activity, "Forget learned word pairs")
        press(activity, KeyEvent.KEYCODE_DPAD_CENTER)
        assertTrue(rowText(activity, "Forget learned word pairs").endsWith("OK again to confirm"))
        press(activity, KeyEvent.KEYCODE_DPAD_DOWN)
        press(activity, KeyEvent.KEYCODE_DPAD_UP)
        assertFalse(rowText(activity, "Forget learned word pairs").endsWith("OK again to confirm"))
    }

    @Test
    fun `the old Traditional T9 settings are not linked any more`() {
        assertFalse(rows(buildActivity()).any { it.text.contains("and the rest") })
    }
}
