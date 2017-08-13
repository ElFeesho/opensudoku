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

import java.util.ArrayList;
import java.util.List;

import cz.romario.opensudoku.R;
import cz.romario.opensudoku.utils.StringUtils;

public class SudokuListFilter {

	public boolean showStateNotStarted = true;
	public boolean showStatePlaying = true;
	public boolean showStateCompleted = true;
    private String notStartedLabel;
    private String playingLabel;
    private String solvedLabel;

	public SudokuListFilter(Context context) {
        notStartedLabel = context.getString(R.string.not_started);
        playingLabel = context.getString(R.string.playing);
        solvedLabel = context.getString(R.string.solved);
    }

	@Override
	public String toString() {
        List<String> visibleStates = new ArrayList<>();
        if (showStateNotStarted) {
            visibleStates.add(notStartedLabel);
        }
		if (showStatePlaying) {
            visibleStates.add(playingLabel);
        }
		if (showStateCompleted) {
            visibleStates.add(solvedLabel);
        }
		return StringUtils.join(visibleStates, ",");
	}
}
