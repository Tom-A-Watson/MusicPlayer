/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model.serializable;

import java.util.List;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public record SerializablePlaylist(
        int id,
        String title,
        List<Integer> songIDs
) { }
