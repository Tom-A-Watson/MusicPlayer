/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model.serializable;

import app.musicplayer.model.Library;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public final class Serializer {

    public static SerializableSong toSerializable(Song song) {
        return null;
        //return new SerializableSong();
    }

    public static Song fromSerializable(SerializableSong song) {
        return null;
    }

    public static SerializablePlaylist toSerializable(Playlist playlist) {
        return null;
    }

    public static SerializableLibrary toSerializable(Library library) {
        return new SerializableLibrary(
                Library.getMusicDirectory().toAbsolutePath().toString(),
                Library.getSongs().stream().map(Serializer::toSerializable).toList(),
                Library.getPlaylists().stream().map(Serializer::toSerializable).toList()
        );
    }
}
