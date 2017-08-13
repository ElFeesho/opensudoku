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

import android.provider.BaseColumns

abstract class SudokuColumns : BaseColumns {
    companion object {
        @JvmField
        val FOLDER_ID = "folder_id"
        @JvmField
        val CREATED = "created"
        @JvmField
        val STATE = "state"
        @JvmField
        val TIME = "time"
        @JvmField
        val LAST_PLAYED = "last_played"
        @JvmField
        val DATA = "data"
        @JvmField
        val PUZZLE_NOTE = "puzzle_note"
    }
}
