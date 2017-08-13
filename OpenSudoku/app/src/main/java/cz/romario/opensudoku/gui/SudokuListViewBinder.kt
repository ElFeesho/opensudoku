package cz.romario.opensudoku.gui

import android.database.Cursor
import android.graphics.Color
import android.view.View
import android.widget.SimpleCursorAdapter
import android.widget.TextView

import cz.romario.opensudoku.R
import cz.romario.opensudoku.db.SudokuColumns
import cz.romario.opensudoku.game.CellCollection
import cz.romario.opensudoku.game.SudokuGame

internal class SudokuListViewBinder : SimpleCursorAdapter.ViewBinder {

    private val mGameTimeFormatter = GameTimeFormat()

    override fun setViewValue(view: View, c: Cursor, columnIndex: Int): Boolean {
        val state = c.getInt(c.getColumnIndex(SudokuColumns.STATE))
        val label: TextView
        when (view.id) {
            R.id.sudoku_board -> {
                val data = c.getString(columnIndex)

                val board = view as SudokuBoardView
                board.isReadOnly = true
                board.isFocusable = false

                try {
                    view.cells = CellCollection.deserialize(data)
                } catch (ignored: Exception) {
                }

            }
            R.id.state -> {
                label = view as TextView
                val stateString: String
                when (state) {
                    SudokuGame.GAME_STATE_COMPLETED -> stateString = view.getContext().getString(R.string.solved)
                    SudokuGame.GAME_STATE_PLAYING -> stateString = view.getContext().getString(R.string.playing)
                    else -> stateString = "Unstarted"
                }
                label.text = stateString
                if (state == SudokuGame.GAME_STATE_COMPLETED) {
                    label.setTextColor(Color.rgb(187, 187, 187))
                } else {
                    label.setTextColor(Color.rgb(255, 255, 255))
                }
            }
            R.id.time -> {
                val time = c.getLong(columnIndex)
                label = view as TextView
                var timeString: String? = null
                if (time != 0L) {
                    timeString = mGameTimeFormatter.format(time)
                }
                label.visibility = if (timeString == null)
                    View.GONE
                else
                    View.VISIBLE
                label.text = timeString
                if (state == SudokuGame.GAME_STATE_COMPLETED) {
                    label.setTextColor(Color.rgb(187, 187, 187))
                } else {
                    label.setTextColor(Color.rgb(255, 255, 255))
                }
            }
        }

        return true
    }
}
