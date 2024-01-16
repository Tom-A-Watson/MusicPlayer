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

public final class MostPlayedPlaylist extends Playlist {

    MostPlayedPlaylist(int id) {
        super(id, "Most Played");
    }

    // TODO:
//    @Override
//    public List<Song> getSongs() {
//        return MusifyApp.getLibrary().getSongs()
//                .stream()
//                .filter(song -> song.getPlayCount() > 0)
//                .sorted((s1, s2) -> Integer.compare(s2.getPlayCount(), s1.getPlayCount()))
//                .limit(100)
//                .collect(Collectors.toList());
//    }
}
