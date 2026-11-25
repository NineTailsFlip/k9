package io.github.ninetailsflip.k9

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyCharsTest {
    private val mandatory = listOf('.', ',', '?', '!')

    private fun problem(result: Result<String>) = (result.exceptionOrNull() as CharsProblemException).problem

    @Test
    fun `0 drops space, new line and tab, and keeps 1's punctuation off`() {
        assertEquals("@_", checkChars(CharList.ZERO, " \n@\t_", mandatory).getOrThrow())
        assertEquals(CharsProblem.Forbidden(listOf('.')), problem(checkChars(CharList.ZERO, "@.", mandatory)))
    }

    @Test
    fun `1 must keep its punctuation and can't be empty`() {
        assertEquals(".,?!-", checkChars(CharList.ONE, ".,?!-", mandatory).getOrThrow())
        assertEquals(CharsProblem.Empty, problem(checkChars(CharList.ONE, "", mandatory)))
        assertEquals(CharsProblem.Missing(listOf('?', '!')), problem(checkChars(CharList.ONE, ".,", mandatory)))
        assertEquals(CharsProblem.Forbidden(listOf(' ')), problem(checkChars(CharList.ONE, ".,?! ", mandatory)))
    }

    @Test
    fun `groups take neither 1's punctuation nor space`() {
        assertEquals(CharsProblem.Forbidden(listOf(',', ' ')), problem(checkChars(CharList.ONE_GROUP, "a, b", mandatory)))
    }

    @Test
    fun `extra letters are letters, each once`() {
        assertEquals("äö", checkLetters("äö").getOrThrow())
        assertEquals(CharsProblem.NotLetters, problem(checkLetters("a1")))
        assertEquals(CharsProblem.Repeating, problem(checkLetters("aa")))
    }

    @Test
    fun `rows show space, new line and tab`() {
        assertEquals("␣⏎⇥x", showChars(" \n\tx"))
        assertEquals("—", showChars(""))
    }
}
