/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import com.almasb.fxgl.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class MainController implements Initializable, PlaylistBoxController.PlaylistBoxHandler {

    private static final Logger log = Logger.get(MainController.class);

    @FXML
    private ScrollPane subViewRoot;

    @FXML
    private VBox sideBar;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        initSearchField();
        initPlaylists();

        //songTableViewController.setSongs(FXGLMusicApp.getLibrary().getSongs());
    }

    private void initSearchField() {
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            String text = newText.trim();
            if (text.isEmpty()) {
                // TODO: what if playlist is selected
                //songTableViewController.setSongs(FXGLMusicApp.getLibrary().getSongs());

            } else {
                Search.search(text);
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
//        for (Playlist playlist : FXGLMusicApp.getLibrary().getPlaylists()) {
//            addNewPlaylistToUI(playlist);
//        }
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

                //songTableViewController.setSongs(playlist.getSongs());
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
//                        if (FXGLMusicApp.getLibrary().findPlaylistByTitle(text).isPresent()) {
//                            System.out.println("TODO: Playlist already exists");
//                        } else {
//
//                            // TODO: if too long title
//
//                            var playlist = FXGLMusicApp.getLibrary().addPlaylist(text);
//
//                            addNewPlaylistToUI(playlist);
//                        }
                    }
                }
            });

            textBox.setOnKeyPressed(x -> {
                if (x.getCode() == KeyCode.ENTER)  {
                    sideBar.requestFocus();
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
                    // TODO: only set songs from different playlist if we are removing the selected one
                    //songTableViewController.setPlaylist(FXGLMusicApp.getLibrary().getLibraryPlaylist());
                    //songTableViewController.setSongs(MusifyApp.getLibrary().getSongs());
                    //FXGLMusicApp.getLibrary().removePlaylist(playlist);
                    playlistBox.getChildren().remove(view);
                });
    }

    private Animation loadViewAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            subViewRoot.setVvalue(0);
            subViewRoot.getContent().setOpacity(frac);
        }
    };

    private Animation unloadViewAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            subViewRoot.getContent().setOpacity(1 - frac);
        }
    };

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

        public static void search(String searchText) {
            if (searchThread != null && searchThread.isAlive()) {
                searchThread.interrupt();
            }

            String text = searchText.toUpperCase();

            searchThread = new Thread(() -> {
                try {
                    hasResults.set(false);

//                    List<Song> songResults = FXGLMusicApp.getLibrary()
//                            .getSongs()
//                            .stream()
//                            .filter(song -> song.getTitle().toUpperCase().contains(text))
//                            .sorted((x, y) -> {
//                                return compareSearchString(x.getTitle().toUpperCase(), y.getTitle().toUpperCase(), text);
//                            })
//                            // TODO: 10 search items
//                            .limit(10)
//                            .collect(Collectors.toList());
//
//                    if (searchThread.isInterrupted()) {
//                        throw new InterruptedException();
//                    }
//
//                    result = new SearchResult(songResults);

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


    // TODO:
//    @Override
//    public List<Song> getSongs() {
//        return MusifyApp.getLibrary().getSongs().stream()
//                .filter(song -> song.getPlayCount() > 0)
//                .sorted((s1, s2) -> s2.getPlayDate().compareTo(s1.getPlayDate()))
//                .limit(100)
//                .collect(Collectors.toList());
//    }

    // TODO:
//    @Override
//    public List<Song> getSongs() {
//        return MusifyApp.getLibrary().getSongs()
//                .stream()
//                .filter(song -> song.getPlayCount() > 0)
//                .sorted((s1, s2) -> Integer.compare(s2.getPlayCount(), s1.getPlayCount()))
//                .limit(100)
//                .collect(Collectors.toList());
//    }

//
//    public static Library getLibrary() {
//        return library;
//    }
//
//    private static class DeserializeLibraryTask extends Task<Library> {
//
//        private final Path file;
//
//        private int songIndex = 0;
//        private int playlistIndex = 0;
//
//        private DeserializeLibraryTask(Path file) {
//            this.file = file;
//        }
//
//        @Override
//        protected Library call() throws Exception {
//            updateMessage("Loading library");
//
//            var library = Serializer.readFromFile(file);
//
//            updateMessage("Loading songs");
//
//            var numSongs = library.songs().size();
//            var numPlaylists = library.playlists().size();
//
//            var songs = library.songs()
//                    .stream()
//                    .map(song -> {
//                        updateProgress(songIndex++, numSongs);
//
//                        return Serializer.fromSerializable(song);
//                    })
//                    .toList();
//
//            updateMessage("Loading playlists");
//
//            var playlists = library.playlists()
//                    .stream()
//                    .map(p -> {
//                        updateProgress(playlistIndex++, numPlaylists);
//
//                        var playlist = Serializer.fromSerializable(p);
//
//                        p.songIDs().forEach(id -> {
//                            songs.stream()
//                                    .filter(s -> s.getId() == id)
//                                    .findAny()
//                                    .ifPresent(playlist::addSong);
//                        });
//
//                        return playlist;
//                    })
//                    .toList();
//
//            return new Library(
//                    playlists
//            );
//        }
//    }


//                    if (library != null)
//                        Serializer.writeToFile(library, LIBRARY_FILE);
//
//                    executorService.shutdownNow();
}
