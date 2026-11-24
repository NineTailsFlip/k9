package io.github.ninetailsflip.k9

import io.github.sspanak.tt9.preferences.settings.SettingsStore

/** One editable list of characters on the key characters screen, with Traditional T9's rules for
 *  it (from its PreferenceChars0/1/Extra/2to9). */
enum class CharList(val title: String, val extraKey: String?) {
    /** 0's characters before the group. Space, new line and tab are always on 0 and aren't typed here. */
    ZERO("0", null),
    ZERO_GROUP("0 · group", SettingsStore.CHARS_GROUP_0),
    ZERO_AFTER("0 · after the group", SettingsStore.CHARS_AFTER_GROUP_0),
    ONE("1", null),
    ONE_GROUP("1 · group", SettingsStore.CHARS_GROUP_1),
    ONE_AFTER("1 · after the group", SettingsStore.CHARS_AFTER_GROUP_1),
    ;

    companion object {
        /** Extra letters for keys 2-9, after the language's own (Traditional T9's
         *  PreferenceChars2to9: key "punctuation_order_key_<n>"). */
        fun letterKey(n: Int) = "punctuation_order_key_$n"
    }
}

/** Why a list can't be saved; the screen words each with Traditional T9's own strings. */
sealed interface CharsProblem {
    data class Forbidden(val chars: List<Char>) : CharsProblem
    data class Missing(val chars: List<Char>) : CharsProblem
    data object Empty : CharsProblem
    data object NotLetters : CharsProblem
    data object Repeating : CharsProblem
}

/** Space, new line and tab: always on 0, never typed into a list. */
val ALWAYS_ON_ZERO = SettingsStore.FORBIDDEN_CHARS_0.toList()

/** The checks Traditional T9 runs before saving [text] as [list]. [mandatory] is what must stay on
 *  1 for the language (its sentence punctuation, SettingsStore.getMandatoryChars0). Returns the
 *  text to save (0's list drops space, new line and tab, which 0 always has) or the problem. */
fun checkChars(list: CharList, text: String, mandatory: List<Char>): Result<String> {
    fun problem(p: CharsProblem) = Result.failure<String>(CharsProblemException(p))
    return when (list) {
        CharList.ZERO -> {
            val cleaned = text.filterNot { it in ALWAYS_ON_ZERO }
            val forbidden = mandatory.filter { it in cleaned }
            if (forbidden.isNotEmpty()) problem(CharsProblem.Forbidden(forbidden)) else Result.success(cleaned)
        }
        CharList.ONE -> {
            val forbidden = ALWAYS_ON_ZERO.filter { it in text }
            val missing = mandatory.filter { it !in text }
            when {
                text.isEmpty() -> problem(CharsProblem.Empty)
                forbidden.isNotEmpty() -> problem(CharsProblem.Forbidden(forbidden))
                missing.isNotEmpty() -> problem(CharsProblem.Missing(missing))
                else -> Result.success(text)
            }
        }
        else -> {
            val forbidden = (mandatory + ALWAYS_ON_ZERO).filter { it in text }
            if (forbidden.isNotEmpty()) problem(CharsProblem.Forbidden(forbidden)) else Result.success(text)
        }
    }
}

/** Keys 2-9: letters only, each once. */
fun checkLetters(text: String): Result<String> = when {
    text.any { !it.isLetter() } -> Result.failure(CharsProblemException(CharsProblem.NotLetters))
    text.toSet().size != text.length -> Result.failure(CharsProblemException(CharsProblem.Repeating))
    else -> Result.success(text)
}

class CharsProblemException(val problem: CharsProblem) : Exception(problem.toString())

/** Characters as a row shows them: space, new line and tab visible. */
fun showChars(chars: String): String = chars
    .replace(" ", "␣")
    .replace("\n", "⏎")
    .replace("\t", "⇥")
    .ifEmpty { "—" }
