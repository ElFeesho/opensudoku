/* 
 * Copyright (C) 2009 Roman Masek
 * 
 * This file is part of OpenSudoku.
 * 
 * OpenSudoku is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OpenSudoku is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package cz.romario.opensudoku.game

import java.util.*
import kotlin.collections.HashSet

/**
 * Note attached to cell. This object is immutable by design.
 *
 * @author romario
 */
class CellNote(val notedNumbers: Set<Int> = emptySet()) {

    val isEmpty: Boolean
        get() = notedNumbers.isEmpty()

    /**
     * Appends string representation of this object to the given `StringBuilder`.
     * You can later recreate object from this string by calling [.deserialize].
     *
     * @param data data to be serialized
     */
    fun serialize(data: StringBuilder) {
        if (notedNumbers.isEmpty()) {
            data.append("-")
        } else {
            notedNumbers.joinTo(data, ",")
        }
    }

    fun serialize() = StringBuilder().apply { serialize(this) }.toString()

    /**
     * Toggles noted number: if number is already noted, it will be removed otherwise it will be added.
     *
     * @param number Number to toggle.
     * @return New CellNote instance with changes.
     */
    fun toggleNumber(number: Int): CellNote {
        val notedNumbers = HashSet(notedNumbers)
        if (notedNumbers.contains(number)) {
            notedNumbers.remove(number)
        } else {
            notedNumbers.add(number)
        }

        return CellNote(notedNumbers)
    }

    /**
     * Adds number to the cell's note (if not present already).
     *
     * @param number
     * @return
     */
    fun addNumber(number: Int) = CellNote(HashSet(notedNumbers).apply { add(number) })

    fun clear() = CellNote()

    companion object {

        val EMPTY = CellNote()

        /**
         * Creates instance from given string (string which has been
         * created by [.serialize] or [.serialize] method).
         * earlier.
         *
         * @param note notes to store, comma separated list of integers, or hyphen if missing
         */
        fun deserialize(note: String?) = CellNote(HashSet<Int>().apply {
            note?.let {
                addAll(note.split(",").filter { it != "-" && it != "" }.map { it.toInt() })
            }
        })

        /**
         * Creates note instance from given `Integer` array.
         *
         * @param notedNums Array of integers, which should be part of note.
         * @return New note instance.
         */
        fun fromIntArray(notedNums: Array<Int>) = CellNote(HashSet<Int>(Arrays.asList(*notedNums)))
    }
}
