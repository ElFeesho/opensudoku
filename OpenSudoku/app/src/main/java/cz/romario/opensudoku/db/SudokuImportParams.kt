package cz.romario.opensudoku.db

import cz.romario.opensudoku.game.SudokuGame

class SudokuImportParams {
    var created: Long = 0
    var state: Long = 0
    var time: Long = 0
    var lastPlayed: Long = 0
    var data: String? = null
    var note: String? = null

    fun clear() {
        created = 0
        state = SudokuGame.GAME_STATE_NOT_STARTED.toLong()
        time = 0
        lastPlayed = 0
        data = null
        note = null
    }
}
