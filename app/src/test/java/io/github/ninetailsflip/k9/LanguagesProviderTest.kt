package io.github.ninetailsflip.k9

import android.content.ContentValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LanguagesProviderTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun rows(provider: LanguagesProvider): List<Triple<Int, String, Boolean>> =
        provider.query(LanguagesProvider.URI, null, null, null, null)!!.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(Triple(c.getInt(0), c.getString(2), c.getInt(3) == 1))
                }
            }
        }

    @Test
    fun `lists every language with its locale and which are on`() {
        val provider = Robolectric.setupContentProvider(LanguagesProvider::class.java, LanguagesProvider.AUTHORITY)
        val rows = rows(provider)
        assertTrue("languages: ${rows.size}", rows.size > 20)
        assertTrue(rows.any { it.second == "ru" || it.second.startsWith("ru-") })
        assertTrue(rows.any { it.third })
    }

    @Test
    fun `update turns exactly the given layouts on`() {
        val provider = Robolectric.setupContentProvider(LanguagesProvider::class.java, LanguagesProvider.AUTHORITY)
        val two = rows(provider).take(2).map { it.first }
        val changed = provider.update(LanguagesProvider.URI, ContentValues().apply { put(LanguagesProvider.COLUMN_ENABLED, two.joinToString(",")) }, null, null)
        assertEquals(2, changed)
        assertEquals(two.toSet(), Engine.settings(context).enabledLanguageIds.toSet())
        assertEquals(two.toSet(), rows(provider).filter { it.third }.map { it.first }.toSet())
    }

    @Test
    fun `an empty or unknown list changes nothing`() {
        val provider = Robolectric.setupContentProvider(LanguagesProvider::class.java, LanguagesProvider.AUTHORITY)
        val before = Engine.settings(context).enabledLanguageIds.toSet()
        assertEquals(0, provider.update(LanguagesProvider.URI, ContentValues().apply { put(LanguagesProvider.COLUMN_ENABLED, "") }, null, null))
        assertEquals(0, provider.update(LanguagesProvider.URI, ContentValues().apply { put(LanguagesProvider.COLUMN_ENABLED, "999999999") }, null, null))
        assertEquals(before, Engine.settings(context).enabledLanguageIds.toSet())
    }

    @Test
    fun `ids parse leniently`() {
        assertEquals(listOf(3, 7), LanguagesProvider.parseIds(" 3, 7,x,7,"))
        assertEquals(emptyList<Int>(), LanguagesProvider.parseIds(null))
    }
}
