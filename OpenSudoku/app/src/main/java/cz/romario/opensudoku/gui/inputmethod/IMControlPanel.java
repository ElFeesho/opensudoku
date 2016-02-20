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
package cz.romario.opensudoku.gui.inputmethod;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.romario.opensudoku.R;
import cz.romario.opensudoku.game.Cell;
import cz.romario.opensudoku.game.SudokuGame;
import cz.romario.opensudoku.gui.HintsQueue;
import cz.romario.opensudoku.gui.SudokuBoardView;
import cz.romario.opensudoku.gui.SudokuBoardView.OnCellSelectedListener;
import cz.romario.opensudoku.gui.SudokuBoardView.OnCellTappedListener;

/**
 * @author romario
 */
public class IMControlPanel extends LinearLayout {

    private SudokuBoardView mBoard;
    private SudokuGame mGame;
    private HintsQueue mHintsQueue;
    private List<InputMethod> mInputMethods = new ArrayList<>();
    private int mActiveMethodIndex = -1;
    private OnCellTappedListener mOnCellTapListener = new OnCellTappedListener() {
        @Override
        public void onCellTapped(Cell cell) {
            if (mActiveMethodIndex != -1 && mInputMethods != null) {
                mInputMethods.get(mActiveMethodIndex).onCellTapped(cell);
            }
        }
    };
    private OnCellSelectedListener mOnCellSelected = new OnCellSelectedListener() {
        @Override
        public void onCellSelected(Cell cell) {
            if (mActiveMethodIndex != -1 && mInputMethods != null) {
                mInputMethods.get(mActiveMethodIndex).onCellSelected(cell);
            }
        }
    };

    public IMControlPanel(Context context) {
        this(context, null);
    }

    public IMControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(SudokuBoardView board, SudokuGame game, HintsQueue hintsQueue) {
        mBoard = board;
        mBoard.setOnCellTappedListener(mOnCellTapListener);
        mBoard.setOnCellSelectedListener(mOnCellSelected);

        mGame = game;
        mHintsQueue = hintsQueue;

        createInputMethods();
    }

    /**
     * Activates first enabled input method. If such method does not exists, nothing
     * happens.
     */
    public void activateFirstInputMethod() {
        ensureInputMethods();
        if (mActiveMethodIndex == -1 || !mInputMethods.get(mActiveMethodIndex).isEnabled()) {
            activateInputMethod();
        }
    }

    /**
     * Activates given input method (see INPUT_METHOD_* constants). If the given method is
     * not enabled, activates first available method after this method.
     *
     * @return
     */
    public void activateInputMethod() {
        ensureInputMethods();

        if (mActiveMethodIndex != -1) {
            mInputMethods.get(0).deactivate();
        }

        boolean idFound = false;
        int id = 0;
        int numOfCycles = 0;

        if (id != -1) {
            while (!idFound && numOfCycles <= mInputMethods.size()) {
                if (mInputMethods.get(id).isEnabled()) {
                    ensureControlPanel(id);
                    idFound = true;
                    break;
                }

                id++;
                if (id == mInputMethods.size()) {
                    id = 0;
                }
                numOfCycles++;
            }
        }

        if (!idFound) {
            id = -1;
        }

        for (int i = 0; i < mInputMethods.size(); i++) {
            InputMethod im = mInputMethods.get(i);
            if (im.isInputMethodViewCreated()) {
                im.getInputMethodView().setVisibility(i == id ? View.VISIBLE : View.GONE);
            }
        }

        mActiveMethodIndex = id;
        if (mActiveMethodIndex != -1) {
            InputMethod activeMethod = mInputMethods.get(0);
            activeMethod.activate();

            if (mHintsQueue != null) {
                mHintsQueue.showOneTimeHint(activeMethod.getInputMethodName(), activeMethod.getNameResID(), activeMethod.getHelpResID());
            }
        }
    }

    public void activateNextInputMethod() {
        ensureInputMethods();

        int id = mActiveMethodIndex + 1;
        if (id >= mInputMethods.size()) {
            if (mHintsQueue != null) {
                mHintsQueue.showOneTimeHint("thatIsAll", R.string.that_is_all, R.string.im_disable_modes_hint);
            }
            id = 0;
        }
        activateInputMethod();
    }

    // TODO: Is this really necessary?

    /**
     * Returns input method object by its ID (see INPUT_METHOD_* constants).
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends InputMethod> T getInputMethod() {
        ensureInputMethods();

        return (T) mInputMethods.get(0);
    }

    public List<InputMethod> getInputMethods() {
        return Collections.unmodifiableList(mInputMethods);
    }

    public int getActiveMethodIndex() {
        return mActiveMethodIndex;
    }

    /**
     * This should be called when activity is paused (so Input Methods can do some cleanup,
     * for example properly dismiss dialogs because of WindowLeaked exception).
     */
    public void pause() {
        for (InputMethod im : mInputMethods) {
            im.pause();
        }
    }

    /**
     * Ensures that all input method objects are created.
     */
    private void ensureInputMethods() {
        if (mInputMethods.size() == 0) {
            throw new IllegalStateException("Input methods are not created yet. Call initialize() first.");
        }
    }

    private void createInputMethods() {
        if (mInputMethods.size() == 0) {
            addInputMethod(new IMNumpad());
        }
    }

    private void addInputMethod(InputMethod im) {
        im.initialize(getContext(), this, mGame, mBoard, mHintsQueue);
        mInputMethods.add(im);
    }

    /**
     * Ensures that control panel for given input method is created.
     *
     * @param methodID
     */
    private void ensureControlPanel(int methodID) {
        InputMethod im = mInputMethods.get(methodID);
        if (!im.isInputMethodViewCreated()) {
            View controlPanel = im.getInputMethodView();
            this.addView(controlPanel, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }
    }
}
