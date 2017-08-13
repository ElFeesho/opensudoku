package cz.romario.opensudoku.gui

import android.view.Menu

import cz.romario.opensudoku.R

internal class SudokuListMenuCreator(private val menu: Menu) {

    operator fun invoke() {
        menu.add(0, MENU_ITEM_FOLDERS, 0, R.string.folders).setShortcut('1', 'f').setIcon(android.R.drawable.ic_menu_sort_by_size)
        menu.add(0, MENU_ITEM_FILTER, 1, R.string.filter).setShortcut('1', 'f').setIcon(android.R.drawable.ic_menu_view)
        menu.add(0, MENU_ITEM_INSERT, 2, R.string.add_sudoku).setShortcut('3', 'a').setIcon(android.R.drawable.ic_menu_add)
    }

    companion object {
        private val MENU_ITEM_INSERT = Menu.FIRST
        private val MENU_ITEM_FILTER = Menu.FIRST + 6
        private val MENU_ITEM_FOLDERS = Menu.FIRST + 7
    }
}
