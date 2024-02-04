/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.events.UserEvent;
import app.musicplayer.model.Library;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import app.musicplayer.model.serializable.Serializer;
import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Popup;
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
import java.util.*;
import java.util.stream.Stream;

import static app.musicplayer.Config.*;
import static app.musicplayer.events.UserDataEvent.*;
import static app.musicplayer.model.Playlist.PlaylistType.MOST_PLAYED;
import static app.musicplayer.model.Playlist.PlaylistType.RECENTLY_PLAYED;
import static com.almasb.fxgl.dsl.FXGL.*;

public class MainController implements Initializable, PlaylistViewController.PlaylistBoxHandler {

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

    private Popup popup = new Popup();

    private boolean canAnimateNewPlaylist = true;

    private ObjectProperty<Parent> selectedPlaylistView = new SimpleObjectProperty<>(null);

    private Library library;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        var pseudoClass = PseudoClass.getPseudoClass("selected");

        selectedPlaylistView.addListener((o, oldView, newView) -> {
            if (oldView != null) {
                oldView.lookup("#titleBox").pseudoClassStateChanged(pseudoClass, false);
            }

            if (newView != null) {
                newView.lookup("#titleBox").pseudoClassStateChanged(pseudoClass, true);
            }
        });

        initEventHandlers();
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

        if (Files.exists(PREFERENCES_FILE)) {
            var map = Serializer.readPropertiesFromFile(PREFERENCES_FILE);
            PREFERENCES.addAll(map);
        }

        // TODO: async
        try {
            Parent playlistMenu = FXMLLoader.load(getAssetLoader().getURL("/assets/ui/controls/PlaylistMenuVBox.fxml"));

            popup.getContent().add(playlistMenu);
            popup.setAutoHide(true);

        } catch (Exception e) {
            log.warning("Could not load playlist menu", e);
        }
    }

    private void initEventHandlers() {
        onEvent(UserEvent.CLICK_IMPORT, event -> {
            onClickImport();
        });

        onEvent(PLAY_SONG, event -> {
            Song song = (Song) event.getData();

            mediaPaneController.play(songTableViewController.getPlaylist(), song);
        });

        onEvent(LOAD_SONG_ARTWORK, event -> {
            Song song = (Song) event.getData();

            var image = loadArtwork(song.getFile());
            song.setArtwork(image);
        });

        onEvent(NAGIVATE_TO_SONG, event -> {
            PlaylistAndSong data = (PlaylistAndSong) event.getData();

            songTableViewController.setPlaylist(data.playlist());
            songTableViewController.selectSong(data.song());
        });
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

        selectedPlaylistView.set((Parent) playlistBox.getChildren().get(0));
        songTableViewController.setPlaylist(library.getLibraryPlaylist());
    }

    private void addNewPlaylistToUI(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Config.FXML + "controls/PlaylistView.fxml"));
            HBox playlistView = loader.load();

            // TODO: safety
            playlistView.getProperties().put("PLAYLIST", playlist);

            // TODO: maybe move to FXML + controller
            var title = playlistView.getChildren().get(1);

            PlaylistViewController controller = loader.getController();
            controller.setPlaylist(playlist);
            controller.setHandler(this);

            title.setOnMouseClicked(e -> {
                if (playlistView.getProperties().containsKey("isRemoved"))
                    return;

                selectedPlaylistView.set(playlistView);

                songTableViewController.setPlaylist(playlist);

                if (playlist.getType() == MOST_PLAYED) {
                    playlist.getSongs().setAll(getMostPlayedSongs());
                }

                if (playlist.getType() == RECENTLY_PLAYED) {
                    playlist.getSongs().setAll(getRecentSongs());
                }
            });

            playlistBox.getChildren().add(playlistView);

            // TODO: a bug when creating playlists, the list may contain null

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
        if (!canAnimateNewPlaylist)
            return;

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

                canAnimateNewPlaylist = true;
            }
        });

        textBox.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)  {
                playlistBox.requestFocus();
            }
        });

        playlistBox.getChildren().add(0, cell);

        textBox.requestFocus();

        animationBuilder()
                .duration(Duration.seconds(1.0))
                .interpolator(Interpolators.EXPONENTIAL.EASE_OUT())
                .translate(playlistBox)
                .from(new Point2D(0, -50))
                .to(new Point2D(0, 0))
                .buildAndPlay();
    }

    private Optional<Node> getPlaylistView(Playlist playlist) {
        return playlistBox.getChildren()
                .stream()
                .filter(view -> view.getProperties().get("PLAYLIST") == playlist)
                .findAny();
    }

    @Override
    public void onClickPlaylistMenu(MouseEvent e, Playlist playlist) {
        Node source = (Node) e.getSource();

        Node popupContent = popup.getContent().get(0);
        Node renameButton = popupContent.lookup("#renameButton");
        Node deleteButton = popupContent.lookup("#deleteButton");

        renameButton.setOnMouseClicked(ev -> {
            popup.hide();

            // TODO: duplicate

            HBox cell = new HBox();

            TextField textBox = new TextField();
            textBox.setPrefHeight(30);
            cell.getChildren().add(textBox);
            HBox.setMargin(textBox, new Insets(10, 10, 10, 9));

            HBox view = (HBox) getPlaylistView(playlist).get();
            Node titleBox = view.lookup("#titleBox");

            view.getChildren().remove(titleBox);
            view.getChildren().add(cell);

            textBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    view.getChildren().remove(cell);
                    view.getChildren().add(titleBox);

                    String text = textBox.getText();

                    if (!text.isEmpty()) {
                        if (library.findPlaylistByTitle(text).isPresent()) {
                            System.out.println("TODO: Playlist already exists");
                        } else {

                            if (text.length() > MAX_PLAYLIST_TITLE_LENGTH) {
                                System.out.println("TODO: Playlist title is too long");
                            } else {
                                // all good
                                playlist.setTitle(text);
                            }
                        }
                    }
                }
            });

            textBox.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER)  {
                    playlistBox.requestFocus();
                }
            });

            textBox.requestFocus();
        });

        deleteButton.setOnMouseClicked(ev -> {
            popup.hide();
            removePlaylist(playlist);
        });

        var p = source.localToScreen(0, 40);

        popup.show(playlistBox.getScene().getWindow(), p.getX(), p.getY());
    }

    private void removePlaylist(Playlist playlist) {
        if (!playlist.isModifiable())
            return;

        getPlaylistView(playlist)
                .ifPresent(view -> {
                    view.getProperties().put("isRemoved", true);

                    // if the current playlist is the one we are removing, then set the default playlist
                    if (songTableViewController.getPlaylist() == playlist) {
                        selectedPlaylistView.set((Parent) playlistBox.getChildren().get(0));
                        songTableViewController.setPlaylist(library.getLibraryPlaylist());
                    }

                    animationBuilder()
                            .duration(Duration.seconds(0.65))
                            .interpolator(Interpolators.EXPONENTIAL.EASE_OUT())
                            .onFinished(() -> {
                                library.removePlaylist(playlist);
                                playlistBox.getChildren().remove(view);
                            })
                            .translate(view)
                            .from(new Point2D(0, 0))
                            .to(new Point2D(-view.getLayoutBounds().getWidth(), 0))
                            .buildAndPlay();
                });
    }

    public void onExit() {
        mediaPaneController.onExit();

        Serializer.writeToFile(library, LIBRARY_FILE);
        Serializer.writeToFile(PREFERENCES, PREFERENCES_FILE);
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

            if (tag != null && tag.getFirstArtwork() != null) {
                byte[] bytes = tag.getFirstArtwork().getBinaryData();
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                Image artwork = new Image(in, 300, 300, true, true);

                if (!artwork.isError())
                    return artwork;
            }
        } catch (Exception e) {
            log.warning("Failed to load artwork for: " + songFile, e);
        }

        return image("albumsIcon.png");
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

            var library = Serializer.readLibraryFromFile(file);

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
