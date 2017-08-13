package cz.romario.opensudoku.gui

import android.view.ContextMenu
import android.view.Menu
import cz.romario.opensudoku.R

internal class SudokuListContextMenuCreator(private val menu: ContextMenu) {

    operator fun invoke() {
        menu.setHeaderTitle("Puzzle")
        menu.add(0, MENU_ITEM_PLAY, 0, R.string.play_puzzle)
        menu.add(0, MENU_ITEM_EDIT_NOTE, 1, R.string.edit_note)
        menu.add(0, MENU_ITEM_RESET, 2, R.string.reset_puzzle)
        menu.add(0, MENU_ITEM_EDIT, 3, R.string.edit_puzzle)
        menu.add(0, MENU_ITEM_DELETE, 4, R.string.delete_puzzle)
    }

    companion object {
        private val MENU_ITEM_EDIT = Menu.FIRST + 1
        private val MENU_ITEM_DELETE = Menu.FIRST + 2
        private val MENU_ITEM_PLAY = Menu.FIRST + 3
        private val MENU_ITEM_RESET = Menu.FIRST + 4
        private val MENU_ITEM_EDIT_NOTE = Menu.FIRST + 5
    }
}
