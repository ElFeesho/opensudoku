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

package cz.romario.opensudoku.gui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.util.Pair
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView
import android.widget.GridView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import cz.romario.opensudoku.R
import cz.romario.opensudoku.db.SudokuColumns
import cz.romario.opensudoku.db.SudokuDatabase

/**
 * List of puzzles in folder.
 *
 * @author romario
 */
class SudokuListActivity : AppCompatActivity() {
    private var mFolderID: Long = 0
    // input parameters for dialogs
    private var mDeletePuzzleID: Long = 0
    private var mResetPuzzleID: Long = 0
    private var mEditNotePuzzleID: Long = 0
    private var mEditNoteInput: TextView? = null
    private var mListFilter: SudokuListFilter? = null
    private var mFilterStatus: TextView? = null
    private var mAdapter: SimpleCursorAdapter? = null
    private var mCursor: Cursor? = null
    private var mDatabase: SudokuDatabase? = null
    private var mFolderDetailLoader: FolderDetailLoader? = null

    private inner class SudokuListContextMenuHandler internal constructor(private val info: AdapterView.AdapterContextMenuInfo, private val itemId: Int) {

        operator fun invoke(): Boolean {
            when (itemId) {
                MENU_ITEM_PLAY -> {
                    startActivity(playSudoku(info.id))
                    return true
                }
                MENU_ITEM_EDIT -> {
                    val i = Intent(this@SudokuListActivity, SudokuEditActivity::class.java)
                    i.action = Intent.ACTION_EDIT
                    i.putExtra(SudokuEditActivity.EXTRA_SUDOKU_ID, info.id)
                    startActivity(i)
                    return true
                }
                MENU_ITEM_DELETE -> {
                    mDeletePuzzleID = info.id
                    showDialog(DIALOG_DELETE_PUZZLE)
                    return true
                }
                MENU_ITEM_EDIT_NOTE -> {
                    mEditNotePuzzleID = info.id
                    showDialog(DIALOG_EDIT_NOTE)
                    return true
                }
                MENU_ITEM_RESET -> {
                    mResetPuzzleID = info.id
                    showDialog(DIALOG_RESET_PUZZLE)
                    return true
                }
            }
            return false
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isTaskRoot && keyCode == KeyEvent.KEYCODE_BACK) {
            startActivity(Intent(this, FolderListActivity::class.java))
            finish()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        SudokuListMenuCreator(menu)()
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View,
                                     menuInfo: ContextMenuInfo) {
        val info: AdapterView.AdapterContextMenuInfo
        try {
            info = menuInfo as AdapterView.AdapterContextMenuInfo
        } catch (e: ClassCastException) {
            Log.e(TAG, "bad menuInfo", e)
            return
        }

        if (mAdapter!!.getItem(info.position) == null) {
            return
        }

        SudokuListContextMenuCreator(menu).invoke()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info: AdapterView.AdapterContextMenuInfo
        try {
            info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        } catch (e: ClassCastException) {
            Log.e(TAG, "bad menuInfo", e)
            return false
        }

        return SudokuListContextMenuHandler(info, item.itemId).invoke()
    }

    override fun onOptionsItemSelected(item: MenuItem) = SudokuListMenuHandler(mFolderID, this).handleMenuSelection(item.itemId, this::showDialog, this::startActivity) || super.onOptionsItemSelected(item)

    /**
     * Updates whole list.
     */
    private fun updateList() {
        updateTitle()
        updateFilterStatus()

        if (mCursor != null) {
            stopManagingCursor(mCursor)
        }
        mCursor = mDatabase!!.getSudokuList(mFolderID, mListFilter)
        startManagingCursor(mCursor)
        mAdapter!!.changeCursor(mCursor)
    }

    private fun updateFilterStatus() {

        if (mListFilter!!.showStateCompleted && mListFilter!!.showStateNotStarted && mListFilter!!.showStatePlaying) {
            mFilterStatus!!.visibility = View.GONE
        } else {
            mFilterStatus!!.text = getString(R.string.filter_active, mListFilter)
            mFilterStatus!!.visibility = View.VISIBLE
        }
    }

    private fun updateTitle() {
        val folder = mDatabase!!.getFolderInfo(mFolderID)
        title = folder!!.name

        mFolderDetailLoader!!.loadDetailAsync(mFolderID) { folderInfo -> title = folderInfo.name + " - " + folderInfo.getDetail(applicationContext) }
    }

    private fun playSudoku(sudokuID: Long): Intent {
        val i = Intent(this@SudokuListActivity, SudokuPlayActivity::class.java)
        i.putExtra(SudokuPlayActivity.EXTRA_SUDOKU_ID, sudokuID)
        return i
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sudoku_list)
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)

        val mListView = findViewById<GridView>(R.id.list)
        mListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> ActivityCompat.startActivity(this@SudokuListActivity, playSudoku(id), ActivityOptionsCompat.makeSceneTransitionAnimation(this@SudokuListActivity, Pair(view.findViewById(R.id.sudoku_board), "board")).toBundle()) }

        mFilterStatus = findViewById(R.id.filter_status)

        mListView.setOnCreateContextMenuListener(this)
        setDefaultKeyMode(Activity.DEFAULT_KEYS_SHORTCUT)

        mDatabase = SudokuDatabase(applicationContext)
        mFolderDetailLoader = FolderDetailLoader(applicationContext)

        val intent = intent
        if (intent.hasExtra(EXTRA_FOLDER_ID)) {
            mFolderID = intent.getLongExtra(EXTRA_FOLDER_ID, 0)
        } else {
            Log.d(TAG, "No 'folder_id' extra provided, exiting.")
            finish()
            return
        }

        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mListFilter = SharedPreferenceSudokuFilterFactory(settings).create(applicationContext)

        mAdapter = SimpleCursorAdapter(this, R.layout.sudoku_list_item, null, arrayOf(SudokuColumns.DATA, SudokuColumns.STATE, SudokuColumns.TIME, SudokuColumns.LAST_PLAYED, SudokuColumns.CREATED, SudokuColumns.PUZZLE_NOTE),
                intArrayOf(R.id.sudoku_board, R.id.state, R.id.time))
        mAdapter!!.viewBinder = SudokuListViewBinder()
        updateList()
        mListView.adapter = mAdapter
    }

    override fun onDestroy() {
        super.onDestroy()

        mDatabase!!.close()
        mFolderDetailLoader!!.destroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong("mDeletePuzzleID", mDeletePuzzleID)
        outState.putLong("mResetPuzzleID", mResetPuzzleID)
        outState.putLong("mEditNotePuzzleID", mEditNotePuzzleID)
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)

        mDeletePuzzleID = state.getLong("mDeletePuzzleID")
        mResetPuzzleID = state.getLong("mResetPuzzleID")
        mEditNotePuzzleID = state.getLong("mEditNotePuzzleID")
    }

    override fun onResume() {
        super.onResume()
        updateTitle()
    }

    override fun onCreateDialog(id: Int): Dialog? {
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        when (id) {
            DIALOG_DELETE_PUZZLE -> return AlertDialog.Builder(this).setIcon(
                    android.R.drawable.ic_delete).setTitle("Puzzle").setMessage(
                    R.string.delete_puzzle_confirm)
                    .setPositiveButton(android.R.string.yes
                    ) { dialog, whichButton ->
                        mDatabase!!.deleteSudoku(mDeletePuzzleID)
                        updateList()
                    }.setNegativeButton(android.R.string.no, null).create()
            DIALOG_EDIT_NOTE -> {
                val factory = LayoutInflater.from(this)
                val noteView = factory.inflate(R.layout.sudoku_list_item_note, null)
                mEditNoteInput = noteView.findViewById(R.id.note)
                return AlertDialog.Builder(this).setIcon(
                        android.R.drawable.ic_menu_add).setTitle(R.string.edit_note)
                        .setView(noteView).setPositiveButton(R.string.save
                ) { dialog, whichButton ->
                    val game = mDatabase!!.getSudoku(mEditNotePuzzleID)
                    game!!.note = mEditNoteInput!!.text
                            .toString()
                    mDatabase!!.updateSudoku(game)
                    updateList()
                }.setNegativeButton(android.R.string.cancel, null).create()
            }
            DIALOG_RESET_PUZZLE -> return AlertDialog.Builder(this).setIcon(
                    android.R.drawable.ic_menu_rotate).setTitle("Puzzle")
                    .setMessage(R.string.reset_puzzle_confirm)
                    .setPositiveButton(android.R.string.yes
                    ) { dialog, whichButton ->
                        val game = mDatabase!!.getSudoku(mResetPuzzleID)
                        if (game != null) {
                            game.reset()
                            mDatabase!!.updateSudoku(game)
                        }
                        updateList()
                    }.setNegativeButton(android.R.string.no, null).create()
            DIALOG_FILTER -> return AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_menu_view)
                    .setTitle(R.string.filter_by_gamestate)
                    .setMultiChoiceItems(
                            R.array.game_states,
                            booleanArrayOf(mListFilter!!.showStateNotStarted, mListFilter!!.showStatePlaying, mListFilter!!.showStateCompleted)
                    ) { dialog, whichButton, isChecked ->
                        when (whichButton) {
                            0 -> mListFilter!!.showStateNotStarted = isChecked
                            1 -> mListFilter!!.showStatePlaying = isChecked
                            2 -> mListFilter!!.showStateCompleted = isChecked
                        }
                    }
                    .setPositiveButton(android.R.string.ok) { dialog, whichButton ->
                        SharedPreferencesSudokuFilterPersister(settings).invoke(mListFilter)
                        updateList()
                    }
                    .setNegativeButton(android.R.string.cancel, null).create()
        }
        return null
    }

    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        super.onPrepareDialog(id, dialog)

        when (id) {
            DIALOG_EDIT_NOTE -> {
                val db = SudokuDatabase(applicationContext)
                val game = db.getSudoku(mEditNotePuzzleID)
                mEditNoteInput!!.text = game!!.note
            }
        }
    }

    companion object {

        val EXTRA_FOLDER_ID = "folder_id"
        val MENU_ITEM_EDIT = Menu.FIRST + 1
        val MENU_ITEM_DELETE = Menu.FIRST + 2
        val MENU_ITEM_PLAY = Menu.FIRST + 3
        val MENU_ITEM_RESET = Menu.FIRST + 4
        val MENU_ITEM_EDIT_NOTE = Menu.FIRST + 5
        private val DIALOG_DELETE_PUZZLE = 0
        private val DIALOG_RESET_PUZZLE = 1
        private val DIALOG_EDIT_NOTE = 2
        private val DIALOG_FILTER = 3

        private val TAG = "SudokuListActivity"
    }

}
