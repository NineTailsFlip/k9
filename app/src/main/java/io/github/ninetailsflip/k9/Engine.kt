package io.github.ninetailsflip.k9

import android.content.Context
import io.github.sspanak.tt9.db.DataStore
import io.github.sspanak.tt9.languages.LanguageCollection
import io.github.sspanak.tt9.preferences.settings.SettingsStore
import life.furrer.nine.core.util.Async

/** Traditional T9's engine, as K9's screens use it. Before languages or dictionaries can be listed,
 *  the language definitions and the word database must be opened; Traditional T9's own settings
 *  did that on a background thread every time it opened, and so does this, once per process. */
object Engine {
    @Volatile private var ready = false

    /** Many of Traditional T9's settings fall back to the language list (the default language,
     *  "more than two languages on"), so it's loaded first - on the calling thread, as Traditional
     *  T9's own settings screen did: it's a list of definitions, not the word database. */
    fun settings(context: Context): SettingsStore {
        LanguageCollection.init(context.applicationContext)
        return SettingsStore(context)
    }

    /** [onReady] runs on the main thread, right away when the engine is already up. */
    fun whenReady(context: Context, onReady: () -> Unit) {
        if (ready) return onReady()
        val app = context.applicationContext
        Async.run(
            background = {
                LanguageCollection.init(app)
                DataStore.init(app)
                val settings = SettingsStore(app)
                if (settings.areHotkeysInitialized()) settings.setDefaultKeys()
                ready = true
            },
            onResult = { onReady() },
        )
    }
}
