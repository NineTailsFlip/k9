package io.github.ninetailsflip.k9

import android.app.Application
import io.github.ninetailsflip.update.Updates
import life.furrer.nine.core.KeyHelp
import life.furrer.nine.core.Nine
import life.furrer.nine.core.NineApp
import life.furrer.nine.core.NineSurface

/** K9 joins the Nine suite the same way L9, C9 and the rest do: one [NineApp] descriptor for
 *  Settings, the manual and backups, and updates from its GitHub releases. The typing engine
 *  underneath is Traditional T9's, unchanged. */
class K9Application : Application() {
    override fun onCreate() {
        super.onCreate()
        Nine.init(k9App)
        Updates.init(this, "NineTailsFlip/k9")
    }
}

private val k9App = NineApp(
    id = "k9",
    name = "K9",
    tagline = "Keyboard on 9 keys",
    settingsActivity = SettingsActivity::class.java,
    websiteUrl = "https://github.com/NineTailsFlip/k9",
    sourceUrl = "https://github.com/NineTailsFlip/k9",
    trackerUrl = "https://github.com/NineTailsFlip/k9/issues",
    surfaces = listOf(
        NineSurface(
            id = "",
            name = "K9",
            introText = """
                K9 ("Keyboard on 9 keys") types with the phone's own keypad, in every app. It is
                NineTailsFlip's fork of Traditional T9, in the look and with the keys of the Nine
                apps.

                In predictive mode (T9) you press each key once per letter and K9 finds the word:
                4-6-6-3 is "good". Left and Right pick another word from the suggestions; OK types
                it. In ABC mode you press a key repeatedly to cycle through its letters. 123 types
                digits. "#" switches between the modes, holding "#" switches the language.

                K9 learns the words you type. Hold "*" while typing for K9's numbered menu: 1 adds a
                word it doesn't know, 2 edits one, 4 and 6 undo and redo, 5 opens the clipboard, 8
                switches to another keyboard and 9 opens these settings.

                Turn K9 on and make it the default keyboard in the "Keyboard" section of Settings.
            """.trimIndent(),
            keyMap = listOf(
                KeyHelp("— While typing —", ""),
                KeyHelp("2-9", "Letters: once per letter in T9, repeatedly in ABC."),
                KeyHelp("0", "Space; press again for a new line or special characters. Hold for 0."),
                KeyHelp("1", "Punctuation; press again for the next mark, then emoji. Hold for 1."),
                KeyHelp("Left / Right", "Previous / next suggestion."),
                KeyHelp("Up", "Show only words that start like the selected one."),
                KeyHelp("Down", "Clear that filter."),
                KeyHelp("OK", "Type the selected word; with no suggestions, the app's own OK."),
                KeyHelp("*", "Shift: Abc, ABC, abc."),
                KeyHelp("Hold *", "K9's menu: 1 add word, 2 edit word, 3 voice, 4 undo, 5 clipboard, 6 redo, 8 keyboard, 9 settings."),
                KeyHelp("#", "Next mode: T9, ABC, 123."),
                KeyHelp("Hold #", "Next language."),
                KeyHelp("Back / Clear", "Delete a character."),
                KeyHelp("— Settings —", ""),
                KeyHelp("Up / Down", "Move."),
                KeyHelp("Left / Right / OK", "Change the selected setting."),
                KeyHelp("Back", "Close."),
            ),
        ),
    ),
)
