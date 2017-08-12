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

import android.util.SparseArray


/**
 * Represents group of cells which must each contain unique number.
 *
 *
 * Typical examples of instances are sudoku row, column or sector (3x3 group of cells).
 *
 * @author romario
 */
class CellGroup {
    private val cells = mutableListOf<Cell>()

    fun addCell(cell: Cell) {
        cells += cell
    }

    fun validate(): Boolean {

        val cellsByValue = SparseArray<Cell>()
        for (cell in cells) {
            val value = cell.value
            if (cellsByValue.get(value) != null) {
                cell.setValid(false)
                cellsByValue.get(value).setValid(false)
            } else {
                cellsByValue.put(value, cell)
            }
        }

        return cells.none { !it.isValid }
    }

    operator fun contains(value: Int): Boolean = cells.any { it.value == value }
}
