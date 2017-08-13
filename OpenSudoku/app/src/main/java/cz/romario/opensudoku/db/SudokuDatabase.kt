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

package cz.romario.opensudoku.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteStatement
import android.provider.BaseColumns
import cz.romario.opensudoku.game.CellCollection
import cz.romario.opensudoku.game.FolderInfo
import cz.romario.opensudoku.game.SudokuGame
import cz.romario.opensudoku.gui.SudokuListFilter

/**
 * Wrapper around opensudoku's database.
 *
 *
 * You have to pass application context when creating instance:
 * `SudokuDatabase db = new SudokuDatabase(getApplicationContext());`
 *
 *
 * You have to explicitly close connection when you're done with database (see [.close]).
 *
 *
 * This class supports database transactions using [.beginTransaction], \
 * [.setTransactionSuccessful] and [.endTransaction].
 * See [SQLiteDatabase] for details on how to use them.
 *
 * @author romario
 */
class SudokuDatabase(context: Context) {
    private val mOpenHelper: DatabaseHelper
    private var mInsertSudokuStatement: SQLiteStatement? = null

    /**
     * Returns list of puzzle folders.
     *
     * @return
     */
    val folderList: Cursor
        get() {
            val qb = SQLiteQueryBuilder()

            qb.tables = FOLDER_TABLE_NAME

            val db = mOpenHelper.readableDatabase
            return qb.query(db, null, null, null, null, null, "created ASC")
        }

    /**
     * Returns folder which acts as a holder for puzzles imported without folder.
     * If this folder does not exists, it is created.
     *
     * @return
     */
    val inboxFolder: FolderInfo?
        get() {
            var inbox = findFolder(INBOX_FOLDER_NAME)
            if (inbox != null) {
                inbox = insertFolder(INBOX_FOLDER_NAME, System.currentTimeMillis())
            }
            return inbox
        }

    init {
        mOpenHelper = DatabaseHelper(context)
    }

    /**
     * Returns the folder info.
     *
     * @param folderID Primary key of folder.
     * @return
     */
    fun getFolderInfo(folderID: Long): FolderInfo? {
        val qb = SQLiteQueryBuilder()

        qb.tables = FOLDER_TABLE_NAME
        qb.appendWhere(BaseColumns._ID + "=" + folderID)

        var c: Cursor? = null

        try {
            val db = mOpenHelper.readableDatabase
            c = qb.query(db, null, null, null, null, null, null)

            if (c!!.moveToFirst()) {
                val id = c.getLong(c.getColumnIndex(BaseColumns._ID))
                val name = c.getString(c.getColumnIndex(FolderColumns.NAME))
                return FolderInfo(id, name)
            } else {
                return null
            }
        } finally {
            if (c != null) c.close()
        }
    }

    /**
     * Returns the full folder info - this includes count of games in particular states.
     *
     * @param folderID Primary key of folder.
     * @return
     */
    fun getFolderInfoFull(folderID: Long): FolderInfo? {
        var folder: FolderInfo? = null

        val db = mOpenHelper.readableDatabase

        // selectionArgs: You may include ?s in where clause in the query, which will be replaced by the values from selectionArgs. The values will be bound as Strings.
        val query = "select folder._id as _id, folder.name as name, sudoku.state as state, count(sudoku.state) as count from folder left join sudoku on folder._id = sudoku.folder_id where folder._id = $folderID group by sudoku.state"

        db.rawQuery(query, null).use { c ->

            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndex(BaseColumns._ID))
                val name = c.getString(c.getColumnIndex(FolderColumns.NAME))
                val state = c.getInt(c.getColumnIndex(SudokuColumns.STATE))
                val count = c.getInt(c.getColumnIndex("count"))

                if (folder == null) {
                    folder = FolderInfo(id, name)
                }

                folder?.let { folder ->
                    folder.puzzleCount = folder.puzzleCount + count
                    if (state == SudokuGame.GAME_STATE_COMPLETED) {
                        folder.solvedCount = folder.solvedCount + count
                    }
                    if (state == SudokuGame.GAME_STATE_PLAYING) {
                        folder.playingCount = folder.playingCount + count
                    }
                }
            }
        }

        return folder
    }

    /**
     * Find folder by name. If no folder is found, null is returned.
     *
     * @param folderName
     * @return
     */
    fun findFolder(folderName: String): FolderInfo? {
        val qb = SQLiteQueryBuilder()

        qb.tables = FOLDER_TABLE_NAME
        qb.appendWhere(FolderColumns.NAME + " = ?")

        var c: Cursor? = null

        try {
            val db = mOpenHelper.readableDatabase
            c = qb.query(db, null, null, arrayOf(folderName), null, null, null)

            if (c!!.moveToFirst()) {
                val id = c.getLong(c.getColumnIndex(BaseColumns._ID))
                val name = c.getString(c.getColumnIndex(FolderColumns.NAME))
                return FolderInfo(id, name)
            } else {
                return null
            }
        } finally {
            if (c != null) c.close()
        }
    }

    /**
     * Inserts new puzzle folder into the database.
     *
     * @param name    Name of the folder.
     * @param created Time of folder creation.
     * @return
     */
    fun insertFolder(name: String, created: Long?): FolderInfo {
        val values = ContentValues()
        values.put(FolderColumns.CREATED, created)
        values.put(FolderColumns.NAME, name)

        val rowId: Long
        val db = mOpenHelper.writableDatabase
        rowId = db.insert(FOLDER_TABLE_NAME, BaseColumns._ID, values)

        if (rowId > 0) {
            return FolderInfo(rowId, name)
        }

        throw SQLException(String.format("Failed to insert folder '%s'.", name))
    }

    /**
     * Updates folder's information.
     *
     * @param folderID Primary key of folder.
     * @param name     New name for the folder.
     */
    fun updateFolder(folderID: Long, name: String) {
        val values = ContentValues()
        values.put(FolderColumns.NAME, name)

        val db = mOpenHelper.writableDatabase
        db.update(FOLDER_TABLE_NAME, values, BaseColumns._ID + "=" + folderID, null)
    }

    /**
     * Deletes given folder.
     *
     * @param folderID Primary key of folder.
     */
    fun deleteFolder(folderID: Long) {
        mOpenHelper.writableDatabase.apply {
            delete(SUDOKU_TABLE_NAME, SudokuColumns.FOLDER_ID + "=" + folderID, null)
            delete(FOLDER_TABLE_NAME, BaseColumns._ID + "=" + folderID, null)
        }
    }

    /**
     * Returns list of puzzles in the given folder.
     *
     * @param folderID Primary key of folder.
     * @return
     */
    fun getSudokuList(folderID: Long, filter: SudokuListFilter?): Cursor {
        val qb = SQLiteQueryBuilder()

        qb.tables = SUDOKU_TABLE_NAME
        qb.appendWhere(SudokuColumns.FOLDER_ID + "=" + folderID)

        if (filter != null) {
            if (!filter.showStateCompleted) {
                qb.appendWhere(" and " + SudokuColumns.STATE + "!=" + SudokuGame.GAME_STATE_COMPLETED)
            }
            if (!filter.showStateNotStarted) {
                qb.appendWhere(" and " + SudokuColumns.STATE + "!=" + SudokuGame.GAME_STATE_NOT_STARTED)
            }
            if (!filter.showStatePlaying) {
                qb.appendWhere(" and " + SudokuColumns.STATE + "!=" + SudokuGame.GAME_STATE_PLAYING)
            }
        }

        val db = mOpenHelper.readableDatabase
        return qb.query(db, null, null, null, null, null, "created DESC")
    }

    /**
     * Returns sudoku game object.
     *
     * @param sudokuID Primary key of folder.
     * @return
     */
    fun getSudoku(sudokuID: Long): SudokuGame? {
        val qb = SQLiteQueryBuilder()

        qb.tables = SUDOKU_TABLE_NAME
        qb.appendWhere(BaseColumns._ID + "=" + sudokuID)

        // Get the database and run the query

        var s: SudokuGame? = null
        val db = mOpenHelper.readableDatabase
        qb.query(db, null, null, null, null, null, null).use { c ->

            if (c!!.moveToFirst()) {
                val id = c.getLong(c.getColumnIndex(BaseColumns._ID))
                val created = c.getLong(c.getColumnIndex(SudokuColumns.CREATED))
                val data = c.getString(c.getColumnIndex(SudokuColumns.DATA))
                val lastPlayed = c.getLong(c.getColumnIndex(SudokuColumns.LAST_PLAYED))
                val state = c.getInt(c.getColumnIndex(SudokuColumns.STATE))
                val time = c.getLong(c.getColumnIndex(SudokuColumns.TIME))
                val note = c.getString(c.getColumnIndex(SudokuColumns.PUZZLE_NOTE))

                s = SudokuGame().apply {
                    this.id = id
                    this.created = created
                    this.cells = CellCollection.deserialize(data)
                    this.lastPlayed = lastPlayed
                    this.state = state
                    this.time = time
                    this.note = note
                }
            }
        }

        return s

    }

    /**
     * Inserts new puzzle into the database.
     *
     * @param folderID Primary key of the folder in which puzzle should be saved.
     * @param sudoku
     * @return
     */
    fun insertSudoku(folderID: Long, sudoku: SudokuGame): Long {
        val db = mOpenHelper.writableDatabase
        val values = ContentValues()
        values.put(SudokuColumns.DATA, sudoku.cells!!.serialize())
        values.put(SudokuColumns.CREATED, sudoku.created)
        values.put(SudokuColumns.LAST_PLAYED, sudoku.lastPlayed)
        values.put(SudokuColumns.STATE, sudoku.state)
        values.put(SudokuColumns.TIME, sudoku.time)
        values.put(SudokuColumns.PUZZLE_NOTE, sudoku.note)
        values.put(SudokuColumns.FOLDER_ID, folderID)

        val rowId = db.insert(SUDOKU_TABLE_NAME, FolderColumns.NAME, values)
        if (rowId > 0) {
            return rowId
        }

        throw SQLException("Failed to insert sudoku.")
    }

    @Throws(SudokuInvalidFormatException::class)
    fun importSudoku(folderID: Long, pars: SudokuImportParams): Long {
        if (pars.data == null) {
            throw SudokuInvalidFormatException(pars.data!!)
        }

        if (!CellCollection.isValid(pars.data!!, CellCollection.DATA_VERSION_PLAIN)) {
            if (!CellCollection.isValid(pars.data!!, CellCollection.DATA_VERSION_1)) {
                throw SudokuInvalidFormatException(pars.data!!)
            }
        }

        if (mInsertSudokuStatement == null) {
            val db = mOpenHelper.writableDatabase
            mInsertSudokuStatement = db.compileStatement(
                    "insert into sudoku (folder_id, created, state, time, last_played, data, puzzle_note) values (?, ?, ?, ?, ?, ?, ?)"
            )
        }

        mInsertSudokuStatement!!.bindLong(1, folderID)
        mInsertSudokuStatement!!.bindLong(2, pars.created)
        mInsertSudokuStatement!!.bindLong(3, pars.state)
        mInsertSudokuStatement!!.bindLong(4, pars.time)
        mInsertSudokuStatement!!.bindLong(5, pars.lastPlayed)
        mInsertSudokuStatement!!.bindString(6, pars.data)
        if (pars.note == null) {
            mInsertSudokuStatement!!.bindNull(7)
        } else {
            mInsertSudokuStatement!!.bindString(7, pars.note)
        }

        val rowId = mInsertSudokuStatement!!.executeInsert()
        if (rowId > 0) {
            return rowId
        }

        throw SQLException("Failed to insert sudoku.")
    }

    /**
     * Returns List of sudokus to export.
     *
     * @param folderID Id of folder to export, -1 if all folders will be exported.
     * @return
     */
    fun exportFolder(folderID: Long): Cursor {
        var query = "select f._id as folder_id, f.name as folder_name, f.created as folder_created, s.created, s.state, s.time, s.last_played, s.data, s.puzzle_note from folder f left outer join sudoku s on f._id = s.folder_id"
        val db = mOpenHelper.readableDatabase
        if (folderID != -1L) {
            query += " where f._id = ?"
        }
        return db.rawQuery(query, if (folderID != -1L) arrayOf(folderID.toString()) else null)
    }

    /**
     * Returns one concrete sudoku to export. Folder context is not exported in this case.
     *
     * @param sudokuID
     * @return
     */
    fun exportSudoku(sudokuID: Long): Cursor {
        val query = "select f._id as folder_id, f.name as folder_name, f.created as folder_created, s.created, s.state, s.time, s.last_played, s.data, s.puzzle_note from sudoku s inner join folder f on s.folder_id = f._id where s._id = ?"
        val db = mOpenHelper.readableDatabase
        return db.rawQuery(query, arrayOf(sudokuID.toString()))
    }

    /**
     * Updates sudoku game in the database.
     *
     * @param sudoku
     */
    fun updateSudoku(sudoku: SudokuGame) {
        val values = ContentValues()
        values.put(SudokuColumns.DATA, sudoku.cells!!.serialize())
        values.put(SudokuColumns.LAST_PLAYED, sudoku.lastPlayed)
        values.put(SudokuColumns.STATE, sudoku.state)
        values.put(SudokuColumns.TIME, sudoku.time)
        values.put(SudokuColumns.PUZZLE_NOTE, sudoku.note)

        val db = mOpenHelper.writableDatabase
        db.update(SUDOKU_TABLE_NAME, values, BaseColumns._ID + "=" + sudoku.id, null)
    }


    /**
     * Deletes given sudoku from the database.
     *
     * @param sudokuID
     */
    fun deleteSudoku(sudokuID: Long) {
        val db = mOpenHelper.writableDatabase
        db.delete(SUDOKU_TABLE_NAME, BaseColumns._ID + "=" + sudokuID, null)
    }

    fun close() {
        if (mInsertSudokuStatement != null) {
            mInsertSudokuStatement!!.close()
        }

        mOpenHelper.close()
    }

    fun beginTransaction() {
        mOpenHelper.writableDatabase.beginTransaction()
    }

    fun setTransactionSuccessful() {
        mOpenHelper.writableDatabase.setTransactionSuccessful()
    }

    fun endTransaction() {
        mOpenHelper.writableDatabase.endTransaction()
    }

    companion object {
        val DATABASE_NAME = "opensudoku"


        val SUDOKU_TABLE_NAME = "sudoku"
        val FOLDER_TABLE_NAME = "folder"

        //private static final String TAG = "SudokuDatabase";
        private val INBOX_FOLDER_NAME = "Inbox"
    }
}
