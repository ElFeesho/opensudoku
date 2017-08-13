package cz.romario.opensudoku.gui

import android.app.Activity
import android.content.Intent
import android.view.Menu

class SudokuListMenuHandler(private val mFolderID: Long, private val activity: Activity) {

    fun handleMenuSelection(menuItemId: Int, showDialog: (Int) -> Unit, launchIntent: (Intent) -> Unit): Boolean {
        when (menuItemId) {
            MENU_ITEM_INSERT -> {
                // Launch activity to insert a new item
                val intent = Intent(activity, SudokuEditActivity::class.java)
                intent.action = Intent.ACTION_INSERT
                intent.putExtra(SudokuEditActivity.EXTRA_FOLDER_ID, mFolderID)
                launchIntent(intent)
                return true
            }
            MENU_ITEM_FILTER -> {
                showDialog(DIALOG_FILTER)
                return true
            }
            MENU_ITEM_FOLDERS -> {
                launchIntent(Intent(activity, FolderListActivity::class.java))
                activity.finish()
                return true
            }
        }
        return false
    }

    companion object {
        private val MENU_ITEM_INSERT = Menu.FIRST
        private val MENU_ITEM_FILTER = Menu.FIRST + 6
        private val MENU_ITEM_FOLDERS = Menu.FIRST + 7
        private val DIALOG_FILTER = 3
    }
}