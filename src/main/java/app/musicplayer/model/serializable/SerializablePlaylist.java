/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model.serializable;

import app.musicplayer.model.Playlist;

import java.util.List;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public record SerializablePlaylist(
        Playlist.PlaylistType type,
        String title,
        List<Integer> songIDs
) { }
