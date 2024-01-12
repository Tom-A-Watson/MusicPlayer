/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import app.musicplayer.MusifyApp;

import java.util.List;
import java.util.stream.Collectors;

public final class RecentlyPlayedPlaylist extends Playlist {

    RecentlyPlayedPlaylist(int id) {
        super(id, "Recently Played");
    }

    @Override
    public List<Song> getSongs() {
        return MusifyApp.getLibrary().getSongs().stream()
                .filter(song -> song.getPlayCount() > 0)
                .sorted((s1, s2) -> s2.getPlayDate().compareTo(s1.getPlayDate()))
                .limit(100)
                .collect(Collectors.toList());
    }
}