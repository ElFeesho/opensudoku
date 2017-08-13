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

import android.util.SparseIntArray
import java.util.*
import java.util.regex.Pattern

/**
 * Collection of sudoku cells. This class in fact represents one sudoku board (9x9).
 *
 * @author romario
 */
class CellCollection private constructor(val cells: Array<Array<Cell>>) {
    private val mChangeListeners = ArrayList<OnChangeListener>()

    // Helper arrays, contains references to the groups of cells, which should contain unique
    // numbers.
    private val sectors = Array(SUDOKU_SIZE) { CellGroup() }
    private val rows = Array(SUDOKU_SIZE) { CellGroup() }
    private val columns = Array(SUDOKU_SIZE) { CellGroup() }
    private var onChangeEnabled = true

    val isEmpty: Boolean
        get() {
            for (r in 0 until SUDOKU_SIZE) {
                for (c in 0 until SUDOKU_SIZE) {
                    val cell = cells[r][c]
                    if (cell.value != 0)
                        return false
                }
            }
            return true
        }

    val isCompleted: Boolean
        get() {
            for (r in 0..SUDOKU_SIZE - 1) {
                for (c in 0..SUDOKU_SIZE - 1) {
                    val cell = cells[r][c]
                    if (cell.value == 0 || !cell.isValid) {
                        return false
                    }
                }
            }
            return true
        }

    val valuesUseCount: SparseIntArray
        get() {
            val valuesUseCount = SparseIntArray()
            for (value in 1..CellCollection.SUDOKU_SIZE) {
                valuesUseCount.put(value, 0)
            }

            for (r in 0 until CellCollection.SUDOKU_SIZE) {
                for (c in 0 until CellCollection.SUDOKU_SIZE) {
                    val value = getCell(r, c).value
                    if (value != 0) {
                        valuesUseCount.put(value, valuesUseCount[value] + 1)
                    }
                }
            }

            return valuesUseCount
        }

    interface OnChangeListener {
        fun onChange()
    }

    init {
        for (r in 0 until SUDOKU_SIZE) {
            for (c in 0 until SUDOKU_SIZE) {
                val cell = this.cells[r][c]
                cell.initCollection(this, r, c,
                        sectors[c / 3 * 3 + r / 3],
                        rows[c],
                        columns[r]
                )
            }
        }
    }

    fun getCell(rowIndex: Int, colIndex: Int) = cells[rowIndex][colIndex]

    private fun markAllCellsAsValid() {
        onChangeEnabled = false
        for (r in 0..SUDOKU_SIZE - 1) {
            for (c in 0..SUDOKU_SIZE - 1) {
                cells[r][c].isValid = true
            }
        }
        onChangeEnabled = true
        onChange()
    }

    fun validate() {
        // first set all cells as valid
        markAllCellsAsValid()

        onChangeEnabled = false
        // run validation in groups
        for (row in rows) {
            row.validate()
        }
        for (column in columns) {
            column.validate()
        }
        for (sector in sectors) {
            sector.validate()
        }

        onChangeEnabled = true
        onChange()

    }

    fun markAllCellsAsEditable() {
        for (r in 0..SUDOKU_SIZE - 1) {
            for (c in 0..SUDOKU_SIZE - 1) {
                val cell = cells[r][c]
                cell.isEditable = true
            }
        }
    }

    fun markFilledCellsAsNotEditable() {
        for (r in 0..SUDOKU_SIZE - 1) {
            for (c in 0..SUDOKU_SIZE - 1) {
                val cell = cells[r][c]
                cell.isEditable = cell.value == 0
            }
        }
    }

    fun serialize(): String {
        val sb = StringBuilder()
        sb.append("version: 1\n")

        for (r in 0..SUDOKU_SIZE - 1) {
            for (c in 0..SUDOKU_SIZE - 1) {
                val cell = cells[r][c]
                cell.serialize(sb)
            }
        }
        return sb.toString()
    }

    fun addOnChangeListener(listener: OnChangeListener) {
        synchronized(mChangeListeners) {
            mChangeListeners.add(listener)
        }
    }

    /**
     * Notify all registered listeners that something has changed.
     */
    fun onChange() {
        if (onChangeEnabled) {
            synchronized(mChangeListeners) {
                for (l in mChangeListeners) {
                    l.onChange()
                }
            }
        }
    }

    companion object {

        val SUDOKU_SIZE = 9
        /**
         * String is expected to be in format "00002343243202...", where each number represents
         * cell value, no other information can be set using this method.
         */
        var DATA_VERSION_PLAIN = 0

        /**
         * See [.DATA_PATTERN_VERSION_1] and [.serialize].
         */
        var DATA_VERSION_1 = 1

        private val DATA_PATTERN_VERSION_PLAIN = Pattern.compile("^\\d{81}$")
        private val DATA_PATTERN_VERSION_1 = Pattern.compile("^version: 1\\n((?#value)\\d\\|(?#note)((\\d,)+|-)\\|(?#editable)[01]\\|){0,81}$")

        fun createEmpty(): CellCollection {
            return CellCollection(Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } })
        }

        private fun deserialize(data: StringTokenizer): CellCollection {
            val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }

            var r = 0
            var c = 0

            while (data.hasMoreTokens() && r < 9) {
                cells[r][c] = Cell.deserialize(data)
                c++

                if (c == 9) {
                    r++
                    c = 0
                }
            }

            return CellCollection(cells)
        }

        fun deserialize(data: String): CellCollection {
            // TODO: use DATA_PATTERN_VERSION_1 to validate and extract puzzle data
            val lines = data.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (lines.size == 0) {
                throw IllegalArgumentException("Cannot deserialize Sudoku, data corrupted.")
            }

            if (lines[0] == "version: 1") {
                val st = StringTokenizer(lines[1], "|")
                return deserialize(st)
            } else {
                return fromString(data)
            }
        }

        private fun fromString(data: String): CellCollection {
            // TODO: validate

            val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell(0) } }

            var pos = 0
            for (r in 0 until CellCollection.SUDOKU_SIZE) {
                for (c in 0 until CellCollection.SUDOKU_SIZE) {
                    var value = 0
                    while (pos < data.length) {
                        pos++
                        if (data[pos - 1] in '0'..'9') {
                            value = data[pos - 1] - '0'
                            break
                        }
                    }
                    val cell = Cell()
                    cell.value = value
                    cell.isEditable = value == 0
                    cells[r][c] = cell
                }
            }

            return CellCollection(cells)
        }

        fun isValid(data: String, dataVersion: Int): Boolean {
            return when (dataVersion) {
                DATA_VERSION_PLAIN -> DATA_PATTERN_VERSION_PLAIN.matcher(data).matches()
                DATA_VERSION_1 -> DATA_PATTERN_VERSION_1.matcher(data).matches()
                else -> throw IllegalArgumentException("Unknown version: " + dataVersion)
            }
        }
    }
}
