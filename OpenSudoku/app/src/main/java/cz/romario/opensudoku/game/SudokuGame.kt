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

import android.os.Bundle
import android.os.SystemClock
import cz.romario.opensudoku.game.command.*

class SudokuGame {

    var id: Long = 0
    var created: Long = 0
    var state: Int = 0
    private var mTime: Long = 0
    var lastPlayed: Long = 0
    var note: String? = null
    private var mCells: CellCollection? = null

    private var mOnPuzzleSolvedListener: OnPuzzleSolvedListener? = null
    private var mCommandStack: CommandStack? = null
    // Time when current activity has become active.
    private var mActiveFromTime: Long = -1

    /**
     * Gets time of game-play in milliseconds.
     *
     * @return
     */
    /**
     * Sets time of play in milliseconds.
     *
     * @param time
     */
    var time: Long
        get() = if (mActiveFromTime != -1L) {
            mTime + SystemClock.uptimeMillis() - mActiveFromTime
        } else {
            mTime
        }
        set(time) {
            mTime = time
        }

    var cells: CellCollection?
        get() = mCells
        set(cells) {
            mCells = cells
            validate()
            mCommandStack = CommandStack(mCells)
        }

    /**
     * Returns true, if puzzle is solved. In order to know the current state, you have to
     * call validate first.
     *
     * @return
     */
    private val isCompleted: Boolean
        get() = mCells!!.isCompleted

    init {
        mTime = 0
        lastPlayed = 0
        created = 0

        state = GAME_STATE_NOT_STARTED
    }

    fun saveState(outState: Bundle) {
        outState.putLong("id", id)
        outState.putString("note", note)
        outState.putLong("created", created)
        outState.putInt("state", state)
        outState.putLong("time", mTime)
        outState.putLong("lastPlayed", lastPlayed)
        outState.putString("cells", mCells!!.serialize())

        mCommandStack!!.saveState(outState)
    }

    fun restoreState(inState: Bundle) {
        id = inState.getLong("id")
        note = inState.getString("note")
        created = inState.getLong("created")
        state = inState.getInt("state")
        mTime = inState.getLong("time")
        lastPlayed = inState.getLong("lastPlayed")
        mCells = CellCollection.deserialize(inState.getString("cells", ""))

        mCommandStack = CommandStack(mCells)
        mCommandStack!!.restoreState(inState)

        validate()
    }


    fun setOnPuzzleSolvedListener(l: OnPuzzleSolvedListener) {
        mOnPuzzleSolvedListener = l
    }

    /**
     * Sets value for the given cell. 0 means empty cell.
     *
     * @param cell
     * @param value
     */
    fun setCellValue(cell: Cell?, value: Int) {
        if (cell == null) {
            throw IllegalArgumentException("Cell cannot be null.")
        }
        if (value < 0 || value > 9) {
            throw IllegalArgumentException("Value must be between 0-9.")
        }

        if (cell.isEditable) {
            executeCommand(SetCellValueCommand(cell, value))

            validate()
            if (isCompleted) {
                finish()
                if (mOnPuzzleSolvedListener != null) {
                    mOnPuzzleSolvedListener!!.onPuzzleSolved()
                }
            }
        }
    }

    /**
     * Sets note attached to the given cell.
     *
     * @param cell
     * @param note
     */
    fun setCellNote(cell: Cell?, note: CellNote?) {
        if (cell == null) {
            throw IllegalArgumentException("Cell cannot be null.")
        }
        if (note == null) {
            throw IllegalArgumentException("Note cannot be null.")
        }

        if (cell.isEditable) {
            executeCommand(EditCellNoteCommand(cell, note))
        }
    }

    private fun executeCommand(c: AbstractCommand) {
        mCommandStack!!.execute(c)
    }

    /**
     * Undo last command.
     */
    fun undo() {
        mCommandStack!!.undo()
    }

    fun hasSomethingToUndo(): Boolean {
        return mCommandStack!!.hasSomethingToUndo()
    }

    fun setUndoCheckpoint() {
        mCommandStack!!.setCheckpoint()
    }

    fun undoToCheckpoint() {
        mCommandStack!!.undoToCheckpoint()
    }

    fun hasUndoCheckpoint(): Boolean {
        return mCommandStack!!.hasCheckpoint()
    }


    /**
     * Start game-play.
     */
    fun start() {
        state = GAME_STATE_PLAYING
        resume()
    }

    fun resume() {
        // reset time we have spent playing so far, so time when activity was not active
        // will not be part of the game play time
        mActiveFromTime = SystemClock.uptimeMillis()
    }

    /**
     * Pauses game-play (for example if activity pauses).
     */
    fun pause() {
        // save time we have spent playing so far - it will be reseted after resuming
        mTime += SystemClock.uptimeMillis() - mActiveFromTime
        mActiveFromTime = -1

        lastPlayed = System.currentTimeMillis()
    }

    /**
     * Finishes game-play. Called when puzzle is solved.
     */
    private fun finish() {
        pause()
        state = GAME_STATE_COMPLETED
    }

    /**
     * Resets game.
     */
    fun reset() {
        for (r in 0..CellCollection.SUDOKU_SIZE - 1) {
            for (c in 0..CellCollection.SUDOKU_SIZE - 1) {
                val cell = mCells!!.getCell(r, c)
                if (cell.isEditable) {
                    cell.value = 0
                    cell.note = CellNote()
                }
            }
        }
        validate()
        time = 0
        lastPlayed = 0
        state = GAME_STATE_NOT_STARTED
    }

    fun clearAllNotes() {
        executeCommand(ClearAllNotesCommand())
    }

    /**
     * Fills in possible values which can be entered in each cell.
     */
    fun fillInNotes() {
        executeCommand(FillInNotesCommand())
    }

    private fun validate() {
        mCells!!.validate()
    }

    interface OnPuzzleSolvedListener {
        fun onPuzzleSolved()
    }

    companion object {

        @JvmField
        val GAME_STATE_PLAYING = 0
        @JvmField
        val GAME_STATE_NOT_STARTED = 1
        @JvmField
        val GAME_STATE_COMPLETED = 2

        fun createEmptyGame(): SudokuGame {
            val game = SudokuGame()
            game.cells = CellCollection.createEmpty()
            // set creation time
            game.created = System.currentTimeMillis()
            return game
        }
    }
}
