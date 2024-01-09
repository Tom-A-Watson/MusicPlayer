/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public record Artist(
        String title,
        Image image,
        List<Album> albums
) implements Comparable<Artist> {

    @Override
    public List<Album> albums() {
        return new ArrayList<>(albums);
    }

    @Override
    public int compareTo(Artist other) {
        return title.compareTo(other.title);
    }
}