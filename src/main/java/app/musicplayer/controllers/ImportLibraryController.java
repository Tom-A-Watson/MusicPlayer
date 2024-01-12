/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.MusicPlayerApp;
import app.musicplayer.model.Album;
import app.musicplayer.model.Artist;
import app.musicplayer.model.Library;
import app.musicplayer.model.Song;
import app.musicplayer.model.serializable.SerializableLibrary;
import app.musicplayer.model.serializable.Serializer;
import app.musicplayer.Config;
import com.almasb.fxgl.logging.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ImportLibraryController {

    private static final Logger log = Logger.get(ImportLibraryController.class);

    @FXML
    private Label label;

    @FXML
    private Button importMusicButton;

    @FXML
    private ProgressBar progressBar;

    private Stage ownerStage = null;
    private Consumer<Library> onFinished = null;

    public void setOwnerStage(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    public void setOnFinished(Consumer<Library> onFinished) {
        this.onFinished = onFinished;
    }

    public void loadFromLibrary(SerializableLibrary library) {
        startLoadingTask(new DeserializeLibraryTask(library));
    }

    @FXML
    private void onClickImport() {
        var dirChooser = new DirectoryChooser();
        File selectedDir = dirChooser.showDialog(ownerStage);

        if (selectedDir == null) {
            log.info("User did not select any directory");
            return;
        }

        startLoadingTask(new ImportLibraryTask(selectedDir.toPath()));
    }

    private void startLoadingTask(Task<Library> task) {
        importMusicButton.setVisible(false);
        progressBar.setVisible(true);

        task.setOnSucceeded(e -> {
            onFinished.accept(task.getValue());
        });

        label.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        MusicPlayerApp.getExecutorService().submit(task);
    }

    private static class ImportLibraryTask extends Task<Library> {

        private final Path directory;

        private ImportLibraryTask(Path directory) {
            this.directory = directory;
        }

        @Override
        protected Library call() throws Exception {
            var songs = loadSongs();
            var albums = loadAlbums(songs);
            var artists = loadArtists(albums);

            return new Library(directory, songs, albums, artists);
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
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();

            String title = tag.getFirst(FieldKey.TITLE);
            if (isEmpty(title)) {
                var fileName = file.getFileName().toString();
                title = fileName.substring(0, fileName.lastIndexOf('.'));
            }

            String artistTitle = tag.getFirst(FieldKey.ALBUM_ARTIST);

            if (isEmpty(artistTitle)) {
                artistTitle = tag.getFirst(FieldKey.ARTIST);
            }

            String albumTitle = tag.getFirst(FieldKey.ALBUM);

            int lengthSeconds = header.getTrackLength();
            String track = tag.getFirst(FieldKey.TRACK);
            int trackNumber = isEmpty(track) ? 0 : Integer.parseInt(track);

            String disc = tag.getFirst(FieldKey.DISC_NO);
            int discNumber = isEmpty(disc) ? 0 : Integer.parseInt(disc);

            return new Song(
                    id,
                    makeSafe(title),
                    makeSafe(artistTitle),
                    makeSafe(albumTitle),
                    lengthSeconds,
                    trackNumber,
                    discNumber,
                    0,
                    LocalDateTime.now(),
                    file
            );
        }

        protected List<Album> loadAlbums(List<Song> songs) {
            List<Album> albums = new ArrayList<>();

            // album title -> songs
            var albumMap = songs.stream().collect(Collectors.groupingBy(Song::getAlbumTitle));

            int id = 0;

            for (var entry : albumMap.entrySet()) {
                var albumTitle = entry.getKey();
                var albumSongs = entry.getValue();

                updateMessage("Loading album: " + albumTitle);

                var album = new Album(id++, albumTitle, albumSongs.get(0).getArtistTitle(), loadArtwork(albumSongs.get(0)), albumSongs);

                albums.add(album);

                albumSongs.forEach(song -> song.setAlbum(album));

                updateProgress(id, albumMap.size());
            }

            return albums;
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

        protected List<Artist> loadArtists(List<Album> albums) {
            updateMessage("Loading artists");

            List<Artist> artists = new ArrayList<>();

            // artist title -> albums
            var artistMap = albums.stream().collect(Collectors.groupingBy(Album::getArtistTitle));

            for (var entry : artistMap.entrySet()) {
                var artistTitle = entry.getKey();
                var artistAlbums = entry.getValue();

                var artist = new Artist(artistTitle, new Image(Config.IMG + "artistsIcon.png"), artistAlbums);

                artists.add(artist);

                artistAlbums.forEach(album -> {
                    album.setArtist(artist);
                    album.getSongs().forEach(song -> song.setArtist(artist));
                });
            }

            return artists;
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

            var songs = library.songs().stream().map(Serializer::fromSerializable).toList();
            var albums = loadAlbums(songs);
            var artists = loadArtists(albums);

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
                    albums,
                    artists,
                    playlists
            );
        }
    }

    private static String makeSafe(String s) {
        if (isEmpty(s))
            return "Unknown";

        return s;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty() || s.equals("null");
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
