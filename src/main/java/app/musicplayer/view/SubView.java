/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.view;

import app.musicplayer.model.Song;

public interface SubView {

    void scroll(char letter);
    void play();
    Song getSelectedSong();
}
