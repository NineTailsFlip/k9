package io.github.ninetailsflip.k9

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TextPageActivityTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun all(view: View): List<View> =
        listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()

    private fun open(page: TextPageActivity.Page): List<String> {
        val activity = Robolectric.buildActivity(TextPageActivity::class.java, TextPageActivity.intentFor(context, page)).setup().get()
        val root = activity.findViewById<View>(android.R.id.content)
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline && all(root).any { it is TextView && it.text.toString() == "loading…" }) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
        return all(root).filterIsInstance<TextView>().map { it.text.toString() }
    }

    @Test
    fun `the manual is Traditional T9's, read from the bundled pages`() {
        val texts = open(TextPageActivity.Page.MANUAL)
        assertEquals("Manual", texts.first())
        val body = texts[1]
        assertTrue("a long manual, not a placeholder: ${body.take(80)}", body.length > 5_000)
        assertTrue(body.contains("T9"))
    }

    @Test
    fun `the privacy policy starts with what K9 adds`() {
        val body = open(TextPageActivity.Page.PRIVACY)[1]
        assertTrue(body.startsWith(TextPageActivity.K9_PRIVACY))
        assertTrue(body.length > TextPageActivity.K9_PRIVACY.length + 500)
    }

    @Test
    fun `try typing offers every kind of field, tagged for K9`() {
        val activity = Robolectric.buildActivity(TryTypingActivity::class.java).setup().get()
        val fields = all(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>()
        assertEquals(9, fields.size)
        assertTrue(fields.all { it.privateImeOptions == io.github.sspanak.tt9.hacks.InputType.OWN_TEST_FIELD_TAG })
    }
}
