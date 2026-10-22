package app.stringer.domain

import app.stringer.data.Shot

/**
 * Investigations: long assignments that take weeks. Each has slots to fill with a frame, in any
 * order, on any walk. When every slot is filled the paper runs a special edition.
 */
enum class Case(val key: String) {
    /** Every letter of the alphabet on the district's signs. */
    ALPHABET("alphabet"),

    /** The digits 0 to 9 on house numbers, prices, anything. */
    DIGITS("digits"),

    /** Twelve different doors of the district. */
    DOORS("doors"),

    /** Ten cats of the district. */
    CATS("cats");

    /** The slots, in the paper's language. */
    fun slots(ru: Boolean): List<String> = when (this) {
        ALPHABET -> (if (ru) "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ").map { it.toString() }
        DIGITS -> (0..9).map { it.toString() }
        DOORS -> (1..12).map { it.toString() }
        CATS -> (1..10).map { it.toString() }
    }

    fun filed(shots: List<Shot>): Map<String, Shot> = shots.filter { it.case == key }.associateBy { it.slot }

    fun done(shots: List<Shot>, ru: Boolean): Boolean {
        val f = filed(shots)
        return slots(ru).all { it in f }
    }

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}
