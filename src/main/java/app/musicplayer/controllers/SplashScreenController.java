/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.MusifyApp;
import app.musicplayer.model.Library;
import app.musicplayer.model.Song;
import app.musicplayer.model.serializable.SerializableLibrary;
import app.musicplayer.model.serializable.Serializer;
import com.almasb.fxgl.logging.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class SplashScreenController {

    private static final Logger log = Logger.get(SplashScreenController.class);

    @FXML
    private Label progressLabel;

    @FXML
    private ProgressBar progressBar;

    private Consumer<Library> onFinished = null;

    public void setOnFinished(Consumer<Library> onFinished) {
        this.onFinished = onFinished;
    }

    public void loadFromLibrary(SerializableLibrary library) {
        startLoadingTask(new DeserializeLibraryTask(library));
    }

    private void startLoadingTask(Task<Library> task) {
        progressBar.setVisible(true);

        task.setOnSucceeded(e -> {
            onFinished.accept(task.getValue());
        });

        progressLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        MusifyApp.getExecutorService().submit(task);
    }

    private static class ImportLibraryTask extends Task<Library> {

        private final Path directory;

        private ImportLibraryTask(Path directory) {
            this.directory = directory;
        }

        @Override
        protected Library call() throws Exception {
            var songs = loadSongs();

            return new Library(directory, songs);
        }

        private List<Song> loadSongs() {
            List<Song> songs = new ArrayList<>();

            try (Stream<Path> filesStream = Files.walk(directory)) {
                var files = filesStream
                        .filter(file -> Files.isRegularFile(file) && isSupportedFileType(file))
                        .toList();

                int id = 0;
                int numFiles = files.size();

                for (var file : files) {
                    updateMessage("Loading: " + file.getFileName());

                    var song = loadSongData(id++, file);
                    songs.add(song);

                    updateProgress(id, numFiles);
                }

            } catch (Exception e) {
                log.warning("Failed to load song data", e);
            }

            return songs;
        }

        private Song loadSongData(int id, Path file) throws Exception {
            AudioFile audioFile = AudioFileIO.read(file.toFile());

            int lengthSeconds = 0;

            if (audioFile != null && audioFile.getAudioHeader() != null)
                lengthSeconds = audioFile.getAudioHeader().getTrackLength();

            String fileName = file.getFileName().toString();
            String title = fileName.substring(0, fileName.lastIndexOf('.'));

            return new Song(
                    id,
                    title,
                    "Artist",
                    "Album",
                    lengthSeconds,
                    0,
                    0,
                    0,
                    LocalDateTime.now(),
                    file
            );
        }

        // TODO:
        private Image loadArtwork(Song song) {
            Image artwork = null;

            try {
                AudioFile audioFile = AudioFileIO.read(song.getFile().toFile());
                Tag tag = audioFile.getTag();

                if (tag.getFirstArtwork() != null) {
                    byte[] bytes = tag.getFirstArtwork().getBinaryData();
                    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                    artwork = new Image(in, 300, 300, true, true);

                    if (artwork.isError()) {
                        throw new RuntimeException(artwork.getException());
                    } else {
                        return artwork;
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to load artwork for " + song.getTitle(), e);
            }

            return new Image(Config.IMG + "albumsIcon.png");
        }
    }

    private static class DeserializeLibraryTask extends ImportLibraryTask {

        private final SerializableLibrary library;

        private DeserializeLibraryTask(SerializableLibrary library) {
            super(Paths.get(library.musicDirectoryPath()));
            this.library = library;
        }

        @Override
        protected Library call() throws Exception {
            updateMessage("Loading songs");

            Thread.sleep(5000);

            var songs = library.songs().stream().map(Serializer::fromSerializable).toList();

            var playlists = library.playlists()
                    .stream()
                    .map(p -> {
                        var playlist = Serializer.fromSerializable(p);

                        p.songIDs().forEach(id -> {
                            songs.stream()
                                    .filter(s -> s.getId() == id)
                                    .findAny()
                                    .ifPresent(playlist::addSong);
                        });

                        return playlist;
                    })
                    .toList();

            return new Library(
                    Paths.get(library.musicDirectoryPath()),
                    songs,
                    playlists
            );
        }
    }

    public static boolean isSupportedFileType(Path file) {
        var fileName = file.toFile().getName();

        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1).toLowerCase();
        }
        return switch (extension) {
            // MP3
            // MP4
            // WAV
            case "mp3", "mp4", "m4a", "m4v", "wav" -> true;
            default -> false;
        };
    }
}
