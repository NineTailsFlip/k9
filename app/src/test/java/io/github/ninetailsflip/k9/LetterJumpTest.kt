package io.github.ninetailsflip.k9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LetterJumpTest {
    private val names = listOf("Arabic", "Armenian", "Bulgarian", "Català", "Čeština", "Dansk", "Deutsch")

    @Test
    fun `right goes to the next letter's first name`() {
        assertEquals(2, letterJump(names, 0, 1))
        assertEquals(2, letterJump(names, 1, 1))
        assertEquals(5, letterJump(names, 4, 1))
        assertNull(letterJump(names, 6, 1))
    }

    @Test
    fun `left goes to the previous letter's first name`() {
        assertEquals(0, letterJump(names, 2, -1))
        assertEquals(4, letterJump(names, 5, -1))
        assertNull(letterJump(names, 1, -1))
    }
}
