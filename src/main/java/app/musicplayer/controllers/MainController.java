/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.events.UserDataEvent;
import app.musicplayer.events.UserEvent;
import app.musicplayer.model.Library;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import app.musicplayer.model.serializable.Serializer;
import com.almasb.fxgl.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static app.musicplayer.Config.*;
import static app.musicplayer.model.Playlist.PlaylistType.*;
import static com.almasb.fxgl.dsl.FXGL.*;

public class MainController implements Initializable, PlaylistBoxController.PlaylistBoxHandler {

    private static final Logger log = Logger.get(MainController.class);

    @FXML
    private VBox playlistBox;

    @FXML
    private VBox mediaPane;
    @FXML
    private MediaPaneController mediaPaneController;

    @FXML
    private TableView<Song> songTableView;
    @FXML
    private SongTableViewController songTableViewController;

    @FXML
    private TextField searchField;

    private Library library;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        onEvent(UserEvent.CLICK_IMPORT, event -> {
            onClickImport();
        });

        onEvent(UserDataEvent.PLAY_SONG, event -> {
            Song song = (Song) event.getData();

            mediaPaneController.play(songTableViewController.getPlaylist(), song);

            // TODO:

//		ObservableList<Song> songList = tableView.getItems();
//		if (MusifyApp.isShuffleActive()) {
//			Collections.shuffle(songList);
//			songList.remove(song);
//			songList.add(0, song);
//		}
        });

        initSearchField();

        if (Files.exists(LIBRARY_FILE)) {
            var task = new DeserializeLibraryTask(LIBRARY_FILE);
            task.setOnSucceeded(e -> {
                library = task.getValue();
                initPlaylists();
            });
            getExecutor().startAsync(task);
        } else {
            library = new Library();

            initPlaylists();
        }
    }

    private void initSearchField() {
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            String text = newText.trim();
            if (text.isEmpty()) {
                songTableViewController.setSongs(songTableViewController.getPlaylist().getSongs());
            } else {
                Search.search(songTableViewController.getPlaylist(), text);
            }
        });

        Search.hasResultsProperty().addListener((observable, hadResults, hasResults) -> {
            if (hasResults) {
                Search.SearchResult result = Search.getResult();
                Platform.runLater(() -> {
                    songTableViewController.setSongs(FXCollections.observableArrayList(result.songResults()));
                });
            }
        });
    }

    private void initPlaylists() {
        for (Playlist playlist : library.getPlaylists()) {
            addNewPlaylistToUI(playlist);
        }

        songTableViewController.setPlaylist(library.getLibraryPlaylist());
    }

    private void addNewPlaylistToUI(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Config.FXML + "controls/PlaylistBox.fxml"));
            HBox playlistView = loader.load();

            // TODO: safety
            playlistView.getProperties().put("PLAYLIST", playlist);

            // TODO: maybe move to FXML + controller
            var title = playlistView.getChildren().get(1);

            PlaylistBoxController controller = loader.getController();
            controller.setPlaylist(playlist);
            controller.setHandler(this);

            title.setOnMouseClicked(e -> {
                songTableViewController.setPlaylist(playlist);

                if (playlist.getType() == MOST_PLAYED) {
                    playlist.getSongs().setAll(getMostPlayedSongs());
                }

                if (playlist.getType() == RECENTLY_PLAYED) {
                    playlist.getSongs().setAll(getRecentSongs());
                }
            });

            playlistBox.getChildren().add(playlistView);

            // sort using array list, rather than in the scene graph
            var tmpCopy = new ArrayList<>(playlistBox.getChildren());
            tmpCopy.sort(
                    Comparator.comparing(view -> (Playlist) view.getProperties().get("PLAYLIST"))
            );

            playlistBox.getChildren().setAll(tmpCopy);

        } catch (Exception e) {
            log.warning("Cannot load playlist view for: " + playlist, e);
        }
    }

    @FXML
    private void onClickAddNewPlaylist() {
        if (!newPlaylistAnimation.getStatus().equals(Status.RUNNING)) {

            HBox cell = new HBox();

            TextField textBox = new TextField();
            textBox.setPrefHeight(30);
            cell.getChildren().add(textBox);
            HBox.setMargin(textBox, new Insets(10, 10, 10, 9));

            textBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    playlistBox.getChildren().remove(cell);

                    String text = textBox.getText();

                    if (!text.isEmpty()) {
                        if (library.findPlaylistByTitle(text).isPresent()) {
                            System.out.println("TODO: Playlist already exists");
                        } else {

                            if (text.length() > MAX_PLAYLIST_TITLE_LENGTH) {
                                System.out.println("TODO: Playlist title is too long");
                            } else {
                                // all good
                                var playlist = library.addPlaylist(text);

                                addNewPlaylistToUI(playlist);
                            }
                        }
                    }
                }
            });

            textBox.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER)  {
                    playlistBox.requestFocus();
                }
            });

            cell.setPrefHeight(0);
            cell.setOpacity(0);

            playlistBox.getChildren().add(0, cell);

            textBox.requestFocus();

            newPlaylistAnimation.play();
        }
    }

    @Override
    public void onClickRemovePlaylist(Playlist playlist) {
        if (!playlist.isModifiable())
            return;

        playlistBox.getChildren()
                .stream()
                .filter(view -> view.getProperties().get("PLAYLIST") == playlist)
                .findAny()
                .ifPresent(view -> {
                    // if the current playlist is the one we are removing, then set the default playlist
                    if (songTableViewController.getPlaylist() == playlist) {
                        songTableViewController.setPlaylist(library.getLibraryPlaylist());
                    }

                    library.removePlaylist(playlist);
                    playlistBox.getChildren().remove(view);
                });
    }

    private Animation newPlaylistAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(500));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            HBox cell = (HBox) playlistBox.getChildren().get(0);
            if (frac < 0.5) {
                cell.setPrefHeight(frac * 100);
            } else {
                cell.setPrefHeight(50);
                cell.setOpacity((frac - 0.5) * 2);
            }
        }
    };

    public void onExit() {
        mediaPaneController.onExit();

        Serializer.writeToFile(library, LIBRARY_FILE);
    }

    public static class Search {

        private static BooleanProperty hasResults = new SimpleBooleanProperty(false);
        private static SearchResult result;
        private static Thread searchThread;

        public static BooleanProperty hasResultsProperty() {
            return hasResults;
        }

        public static SearchResult getResult() {
            hasResults.set(false);
            return result;
        }

        public static void search(Playlist playlist, String searchText) {
            if (searchThread != null && searchThread.isAlive()) {
                searchThread.interrupt();
            }

            String text = searchText.toUpperCase();

            searchThread = new Thread(() -> {
                try {
                    hasResults.set(false);

                    List<Song> songResults = playlist
                            .getSongs()
                            .stream()
                            .filter(song -> song.getTitle().toUpperCase().contains(text))
                            .sorted((x, y) -> {
                                return compareSearchString(x.getTitle().toUpperCase(), y.getTitle().toUpperCase(), text);
                            })
                            // TODO: 10 search items
                            .limit(10)
                            .toList();

                    if (searchThread.isInterrupted()) {
                        throw new InterruptedException();
                    }

                    result = new SearchResult(songResults);

                    hasResults.set(true);

                } catch (Exception ex) {
                    // terminate thread
                }
            });
            searchThread.start();
        }

        /**
         * All arguments must be uppercase.
         *
         * @return Comparator compareTo() int
         */
        private static int compareSearchString(String s1, String s2, String text) {
            boolean xMatch = s1.equals(text);
            boolean yMatch = s2.equals(text);
            if (xMatch && yMatch)
                return 0;
            if (xMatch)
                return -1;
            if (yMatch)
                return 1;

            boolean xStartWith = s1.startsWith(text);
            boolean yStartWith = s2.startsWith(text);
            if (xStartWith && yStartWith)
                return 0;
            if (xStartWith)
                return -1;
            if (yStartWith)
                return 1;

            boolean xContains = s1.contains(" " + text);
            boolean yContains = s2.contains(" " + text);
            if (xContains && yContains)
                return 0;
            if (xContains)
                return -1;
            if (yContains)
                return 1;

            return 0;
        }

        public record SearchResult(List<Song> songResults) { }
    }

    private List<Song> getRecentSongs() {
        return library.getSongs()
                .stream()
                .filter(song -> song.getPlayCount() > 0)
                .sorted((s1, s2) -> s2.getPlayDate().compareTo(s1.getPlayDate()))
                .limit(100)
                .toList();
    }

    private List<Song> getMostPlayedSongs() {
        return library.getSongs()
                .stream()
                .filter(song -> song.getPlayCount() > 0)
                .sorted((s1, s2) -> Integer.compare(s2.getPlayCount(), s1.getPlayCount()))
                .limit(100)
                .toList();
    }

    private void onClickImport() {
        var dirChooser = new DirectoryChooser();
        File selectedDir = dirChooser.showDialog(songTableView.getScene().getWindow());

        if (selectedDir == null) {
            log.info("User did not select any directory");
            return;
        }

        var task = new LoadSongsTask(selectedDir.toPath());
        task.setOnSucceeded(e -> {
            library.addSongsNoDuplicateCheck(task.getValue());
        });

        getExecutor().startAsync(task);
    }

    private static class LoadSongsTask extends Task<List<Song>> {

        private final Path directory;

        private LoadSongsTask(Path directory) {
            this.directory = directory;
        }

        @Override
        protected List<Song> call() throws Exception {
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
                    lengthSeconds,
                    0,
                    LocalDateTime.now(),
                    file
            );
        }

        private static boolean isSupportedFileType(Path file) {
            var fileName = file.toString();

            return SUPPORTED_FILE_EXTENSIONS.stream()
                    .anyMatch(fileName::endsWith);
        }
    }

    private static Image loadArtwork(Path songFile) {
        try {
            AudioFile audioFile = AudioFileIO.read(songFile.toFile());
            Tag tag = audioFile.getTag();

            if (tag.getFirstArtwork() != null) {
                byte[] bytes = tag.getFirstArtwork().getBinaryData();
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                Image artwork = new Image(in, 300, 300, true, true);

                if (!artwork.isError())
                    return artwork;
            }
        } catch (Exception e) {
            log.warning("Failed to load artwork for: " + songFile, e);
        }

        return new Image(Config.IMG + "albumsIcon.png");
    }

    private static class DeserializeLibraryTask extends Task<Library> {

        private final Path file;

        private int songIndex = 0;
        private int playlistIndex = 0;

        private DeserializeLibraryTask(Path file) {
            this.file = file;
        }

        @Override
        protected Library call() throws Exception {
            updateMessage("Loading library");

            var library = Serializer.readFromFile(file);

            updateMessage("Loading songs");

            var numSongs = library.songs().size();
            var numPlaylists = library.playlists().size();

            var songs = library.songs()
                    .stream()
                    .map(song -> {
                        updateProgress(songIndex++, numSongs);

                        return Serializer.fromSerializable(song);
                    })
                    .toList();

            updateMessage("Loading playlists");

            var playlists = library.playlists()
                    .stream()
                    .map(p -> {
                        updateProgress(playlistIndex++, numPlaylists);

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

            return new Library(playlists);
        }
    }
}
