package io.github.ninetailsflip.k9

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import io.github.sspanak.tt9.languages.LanguageCollection
import io.github.sspanak.tt9.languages.NaturalLanguage

/**
 * K9's keyboard layouts (its languages), for the phone's own setup and Settings: they list them
 * and choose which are on, the same set K9 → Languages shows ticked.
 *
 * `content://io.github.ninetailsflip.k9.languages/languages`:
 *  - query: one row per language — [COLUMN_ID], [COLUMN_NAME] (in that language), [COLUMN_LOCALE]
 *    (a BCP 47 tag) and [COLUMN_ENABLED] (1 or 0);
 *  - update with [COLUMN_ENABLED] = the ids that should be on, comma-separated: saves them. At
 *    least one valid id must remain, as in K9 → Languages; otherwise nothing changes and 0 comes
 *    back. A language without its dictionary gets it the first time K9 types in it.
 *
 * Reading is open (the list says nothing about the user); changing needs WRITE_SECURE_SETTINGS
 * (the manifest), which only the system's own setup and Settings hold.
 */
class LanguagesProvider : ContentProvider() {

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (matcher.match(uri) != LANGUAGES) return null
        val context = context ?: return null
        val settings = Engine.settings(context)
        val enabled = settings.enabledLanguageIds.toSet()
        val cursor = MatrixCursor(arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_LOCALE, COLUMN_ENABLED))
        LanguageCollection.getAll(true).filterIsInstance<NaturalLanguage>().forEach { language ->
            cursor.addRow(arrayOf<Any>(language.id, language.name, language.locale.toLanguageTag(), if (language.id in enabled) 1 else 0))
        }
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (matcher.match(uri) != LANGUAGES) return 0
        val context = context ?: return 0
        val ids = parseIds(values?.getAsString(COLUMN_ENABLED))
        val settings = Engine.settings(context)
        val known = ids.filter { LanguageCollection.getLanguage(it) != null }
        if (known.isEmpty()) return 0
        settings.saveEnabledLanguageIds(ArrayList(known))
        context.contentResolver.notifyChange(uri, null)
        return known.size
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val AUTHORITY = "io.github.ninetailsflip.k9.languages"
        val URI: Uri = Uri.parse("content://$AUTHORITY/languages")
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_LOCALE = "locale"
        const val COLUMN_ENABLED = "enabled"

        private const val LANGUAGES = 1
        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply { addURI(AUTHORITY, "languages", LANGUAGES) }

        /** "3, 7,x,7" → [3, 7]: blanks and junk dropped, each id once, in order. */
        internal fun parseIds(raw: String?): List<Int> =
            raw.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.distinct()
    }
}
