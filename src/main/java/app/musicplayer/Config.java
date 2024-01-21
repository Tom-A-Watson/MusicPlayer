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
import java.util.List;

public final class Config {

    public static final String TITLE = "FXGL Music";
    public static final String VERSION = "2.0";

    /**
     * This is driven by UI constraints.
     * This can be updated if UI changes.
     */
    public static final int MAX_PLAYLIST_TITLE_LENGTH = 18;

    public static final Path LIBRARY_FILE = Paths.get("library.json");

    public static final DataFormat DRAG_SONG_LIST = new DataFormat("application/javafx-song-list");

    public static final String VAR_DRAGGED_SONGS = "application/javafx-song-list";

    public static final List<String> SUPPORTED_FILE_EXTENSIONS = List.of("mp3", "mp4", "m4a", "m4v", "wav");

    public static final String FXML = "/assets/ui/";
    public static final String IMG = "/assets/textures/";
    public static final String CSS = "/assets/ui/css/";
}