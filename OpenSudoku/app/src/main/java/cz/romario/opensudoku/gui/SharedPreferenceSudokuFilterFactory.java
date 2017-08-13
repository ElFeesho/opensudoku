package cz.romario.opensudoku.gui;

import android.content.Context;
import android.content.SharedPreferences;

import cz.romario.opensudoku.game.SudokuGame;

class SharedPreferenceSudokuFilterFactory {
    private static final String FILTER_STATE_NOT_STARTED = "filter" + SudokuGame.GAME_STATE_NOT_STARTED;
    private static final String FILTER_STATE_PLAYING = "filter" + SudokuGame.GAME_STATE_PLAYING;
    private static final String FILTER_STATE_SOLVED = "filter" + SudokuGame.GAME_STATE_COMPLETED;
    private SharedPreferences settings;

    public SharedPreferenceSudokuFilterFactory(SharedPreferences settings) {
        this.settings = settings;
    }

    public SudokuListFilter create(Context context) {
        SudokuListFilter filter = new SudokuListFilter(context);
        filter.showStateNotStarted = settings.getBoolean(FILTER_STATE_NOT_STARTED, true);
        filter.showStatePlaying = settings.getBoolean(FILTER_STATE_PLAYING, true);
        filter.showStateCompleted = settings.getBoolean(FILTER_STATE_SOLVED, true);
        return filter;
    }
}
