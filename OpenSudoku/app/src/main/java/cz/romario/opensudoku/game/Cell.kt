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

/**
 * Sudoku cell. Every cell has value, some notes attached to it and some basic
 * state (whether it is editable and valid).
 *
 * @author romario
 */
class Cell private constructor(private var mValue: Int, private var mNote: CellNote?, editable: Boolean, valid: Boolean) {
    // if cell is included in collection, here are some additional information
    // about collection and cell's position in it
    private var mCellCollection: CellCollection? = null
    private val mCellCollectionLock = Any()
    /**
     * Gets cell's row index within [CellCollection].
     *
     * @return Cell's row index within CellCollection.
     */
    var rowIndex = -1
        private set
    /**
     * Gets cell's column index within [CellCollection].
     *
     * @return Cell's column index within CellColection.
     */
    var columnIndex = -1
        private set
    /**
     * Returns sector containing this cell. Sector is 3x3 group of cells.
     *
     * @return Sector containing this cell.
     */
    var sector: CellGroup? = null
        private set // sector containing this cell
    /**
     * Returns row containing this cell.
     *
     * @return Row containing this cell.
     */
    var row: CellGroup? = null
        private set // row containing this cell
    /**
     * Returns column containing this cell.
     *
     * @return Column containing this cell.
     */
    var column: CellGroup? = null
        private set // column containing this cell
    /**
     * Returns whether cell can be edited.
     *
     * @return True if cell can be edited.
     */
    var isEditable: Boolean = false
        private set
    /**
     * Returns true, if cell contains valid value according to sudoku rules.
     *
     * @return True, if cell contains valid value according to sudoku rules.
     */
    var isValid: Boolean = false
        private set

    /**
     * Creates empty editable cell.
     */
    constructor() : this(0, CellNote(), true, true) {}

    /**
     * Creates empty editable cell containing given value.
     *
     * @param value Value of the cell.
     */
    constructor(value: Int) : this(value, CellNote(), true, true)

    init {
        if (mValue < 0 || mValue > 9) {
            throw IllegalArgumentException("Value must be between 0-9.")
        }
        isEditable = editable
        isValid = valid
    }

    /**
     * Called when `Cell` is added to [CellCollection].
     *
     * @param rowIndex Cell's row index within collection.
     * @param colIndex Cell's column index within collection.
     * @param sector   Reference to sector group in which cell is included.
     * @param row      Reference to row group in which cell is included.
     * @param column   Reference to column group in which cell is included.
     */
    fun initCollection(cellCollection: CellCollection, rowIndex: Int, colIndex: Int,
                       sector: CellGroup, row: CellGroup, column: CellGroup) {

        synchronized(mCellCollectionLock) {
            mCellCollection = cellCollection
        }

        this.rowIndex = rowIndex
        columnIndex = colIndex
        this.sector = sector
        this.row = row
        this.column = column

        sector.addCell(this)
        row.addCell(this)
        column.addCell(this)
    }

    /**
     * Gets cell's value. Value can be 1-9 or 0 if cell is empty.
     *
     * @return Cell's value. Value can be 1-9 or 0 if cell is empty.
     */
    /**
     * Sets cell's value. Value can be 1-9 or 0 if cell should be empty.
     *
     * @param value 1-9 or 0 if cell should be empty.
     */
    var value: Int
        get() = mValue
        set(value) {
            if (value < 0 || value > 9) {
                throw IllegalArgumentException("Value must be between 0-9.")
            }
            mValue = value
            onChange()
        }


    /**
     * Gets note attached to the cell.
     *
     * @return Note attached to the cell.
     */
    /**
     * Sets note attached to the cell
     *
     * @param note Note attached to the cell
     */
    var note: CellNote?
        get() = mNote
        set(note) {
            mNote = note
            onChange()
        }

    /**
     * Sets whether cell can be edited.
     *
     * @param editable True, if cell should allow editing.
     */
    fun setEditable(editable: Boolean?) {
        isEditable = editable!!
        onChange()
    }

    /**
     * Sets whether cell contains valid value according to sudoku rules.
     *
     * @param valid
     */
    fun setValid(valid: Boolean?) {
        isValid = valid!!
        onChange()
    }


    /**
     * Appends string representation of this object to the given `StringBuilder`.
     * You can later recreate object from this string by calling [.deserialize].
     *
     * @param data
     */
    fun serialize(data: StringBuilder) {
        data.append(mValue).append("|")
        if (mNote == null || mNote!!.isEmpty) {
            data.append("-").append("|")
        } else {
            mNote!!.serialize(data)
            data.append("|")
        }
        data.append(if (isEditable) "1" else "0").append("|")
    }

    /**
     * Notify CellCollection that something has changed.
     */
    private fun onChange() {
        synchronized(mCellCollectionLock) {
            if (mCellCollection != null) {
                mCellCollection!!.onChange()
            }
        }
    }

    companion object {


        /**
         * Creates instance from given `StringTokenizer`.
         *
         * @param data
         * @return
         */
        fun deserialize(data: StringTokenizer): Cell {
            val cell = Cell()
            cell.value = Integer.parseInt(data.nextToken())
            cell.note = CellNote.deserialize(data.nextToken())
            cell.setEditable(data.nextToken() == "1")

            return cell
        }
    }
}
