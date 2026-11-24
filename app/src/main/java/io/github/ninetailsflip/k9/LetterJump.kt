package io.github.ninetailsflip.k9

/** Left/Right in an alphabetical list: the index of the first name under the previous ([direction]
 *  -1) or next (+1) initial letter, or null at either end. Pure, for unit tests. */
fun letterJump(names: List<String>, from: Int, direction: Int): Int? {
    fun initial(i: Int) = names[i].firstOrNull()?.uppercaseChar()
    if (from !in names.indices) return null
    val letter = initial(from)
    if (direction > 0) {
        return (from + 1 until names.size).firstOrNull { initial(it) != letter }
    }
    val previous = (from - 1 downTo 0).firstOrNull { initial(it) != letter } ?: return null
    val previousLetter = initial(previous)
    return (0..previous).first { initial(it) == previousLetter }
}
