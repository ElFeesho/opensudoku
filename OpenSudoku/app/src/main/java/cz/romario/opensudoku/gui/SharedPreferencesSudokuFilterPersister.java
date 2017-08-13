package cz.romario.opensudoku.gui;

import android.content.SharedPreferences;

import cz.romario.opensudoku.game.SudokuGame;

class SharedPreferencesSudokuFilterPersister {
    private static final String FILTER_STATE_NOT_STARTED = "filter" + SudokuGame.GAME_STATE_NOT_STARTED;
    private static final String FILTER_STATE_PLAYING = "filter" + SudokuGame.GAME_STATE_PLAYING;
    private static final String FILTER_STATE_SOLVED = "filter" + SudokuGame.GAME_STATE_COMPLETED;
    private SharedPreferences settings;

    public SharedPreferencesSudokuFilterPersister(SharedPreferences settings) {
        this.settings = settings;
    }

    public void invoke(SudokuListFilter filter) {
        settings.edit()
                .putBoolean(FILTER_STATE_NOT_STARTED, filter.showStateNotStarted)
                .putBoolean(FILTER_STATE_PLAYING, filter.showStatePlaying)
                .putBoolean(FILTER_STATE_SOLVED, filter.showStateCompleted)
                .apply();
    }
}
