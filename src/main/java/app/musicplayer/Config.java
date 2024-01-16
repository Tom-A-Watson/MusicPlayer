/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer;

import javafx.scene.input.DataFormat;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {

    public static final String VERSION = "2.0";

    public static final Path LIBRARY_FILE = Paths.get("library.json");

    public static final DataFormat DRAG_SONG_LIST = new DataFormat("application/javafx-song-list");

    public static final String FXML = "/assets/ui/";
    public static final String IMG = "/assets/textures/";
    public static final String CSS = "/assets/ui/css/";
}