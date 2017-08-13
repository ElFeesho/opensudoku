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

package cz.romario.opensudoku.gui;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.romario.opensudoku.db.SudokuDatabase;
import cz.romario.opensudoku.game.FolderInfo;

/**
 * Loads details of given folders on one single background thread.
 * Results are published on GUI thread via {@link FolderDetailCallback} interface.
 * <p/>
 * Please note that instance of this class has to be created on GUI thread!
 * <p/>
 * You should explicitly call {@link #destroy()} when this object is no longer needed.
 *
 * @author romario
 */
public class FolderDetailLoader {

    public interface FolderDetailCallback {
        void onLoaded(FolderInfo folderInfo);
    }

    private static final String TAG = "FolderDetailLoader";
    private SudokuDatabase mDatabase;
    private Handler mGuiHandler;
	private ExecutorService mLoaderService = Executors.newSingleThreadExecutor();

	public FolderDetailLoader(Context context) {
        mDatabase = new SudokuDatabase(context);
        mGuiHandler = new Handler();
	}

    public void loadDetailAsync(final long folderID, final FolderDetailCallback loadedCallback) {
        mLoaderService.execute(new Runnable() {
            @Override
			public void run() {
				try {
                    final FolderInfo folderInfo = mDatabase.getFolderInfoFull(folderID);

                    if (folderInfo != null) {
                        mGuiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                loadedCallback.onLoaded(folderInfo);
                            }
                        });
                    }
                } catch (Exception e) {
					Log.e(TAG, "Error occured while loading full folder info.", e);
				}
			}
		});
	}

    void destroy() {
        mLoaderService.shutdownNow();
		mDatabase.close();
	}
}
