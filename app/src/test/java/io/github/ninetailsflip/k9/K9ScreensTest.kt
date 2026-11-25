package io.github.ninetailsflip.k9

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class K9ScreensTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun target(screen: String?) = K9Screens.intentFor(context, screen).component!!.className

    @Test
    fun `Traditional T9's screen names open K9's screens`() {
        assertEquals(SettingsActivity::class.java.name, target(null))
        assertEquals(LanguagesActivity::class.java.name, target("Languages"))
        assertEquals(HotkeysActivity::class.java.name, target("Hotkeys"))
        assertEquals(KeyCharsActivity::class.java.name, target("KeyChars"))
        assertEquals(AddedWordsActivity::class.java.name, target("DeleteWords"))
        assertEquals(SettingsActivity::class.java.name, target("Appearance"))
    }
}
