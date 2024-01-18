/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.MusifyApp;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import com.almasb.fxgl.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable, PlaylistBoxController.PlaylistBoxHandler {

    private static final Logger log = Logger.get(MainController.class);

    @FXML private ScrollPane subViewRoot;
    @FXML private VBox sideBar;
    @FXML private VBox playlistBox;
    @FXML private ImageView nowPlayingArtwork;
    @FXML private Label nowPlayingTitle;
    @FXML private Label nowPlayingArtist;
    @FXML private Slider timeSlider;
    @FXML private Region frontSliderTrack;
    @FXML private Label timePassed;
    @FXML private Label timeRemaining;

    @FXML
    private HBox volumePane;
    @FXML
    private VolumeBoxController volumePaneController;

    @FXML private Pane playButton;
    @FXML private Pane pauseButton;
    @FXML private Pane loopButton;
    @FXML private Pane shuffleButton;
    @FXML private HBox controlBox;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Song> songTableView;
    @FXML
    private SongTableViewController songTableViewController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        // remove pause button until needed
        controlBox.getChildren().remove(3);

        frontSliderTrack.prefWidthProperty().bind(timeSlider.widthProperty().multiply(timeSlider.valueProperty().divide(timeSlider.maxProperty())));

        PseudoClass active = PseudoClass.getPseudoClass("active");
        loopButton.setOnMouseClicked(x -> {
            sideBar.requestFocus();
            MusifyApp.toggleLoop();
            loopButton.pseudoClassStateChanged(active, MusifyApp.isLoopActive());
        });
        shuffleButton.setOnMouseClicked(x -> {
            sideBar.requestFocus();
            MusifyApp.toggleShuffle();
            shuffleButton.pseudoClassStateChanged(active, MusifyApp.isShuffleActive());
        });

        timeSlider.valueChangingProperty().addListener((slider, wasChanging, isChanging) -> {
            if (wasChanging) {
                int seconds = (int) Math.round(timeSlider.getValue() / 4.0);
                timeSlider.setValue(seconds * 4);
                MusifyApp.seek(seconds);
            }
        });

        timeSlider.valueProperty().addListener((slider, oldValue, newValue) -> {
            double previous = oldValue.doubleValue();
            double current = newValue.doubleValue();
            if (!timeSlider.isValueChanging() && current != previous + 1 && !timeSlider.isPressed()) {
                int seconds = (int) Math.round(current / 4.0);
                timeSlider.setValue(seconds * 4);
                MusifyApp.seek(seconds);
            }
        });

        searchField.textProperty().addListener((observable, oldText, newText) -> {
            String text = newText.trim();
            if (text.isEmpty()) {
                // TODO: what if playlist is selected
                songTableViewController.setSongs(MusifyApp.getLibrary().getSongs());

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

        volumePaneController.mutedProperty().addListener((observable, wasMuted, isMuted) -> {
            MusifyApp.mute(isMuted);
        });

        updateNowPlayingButton();
        initializeTimeSlider();
        initializeTimeLabels();
        initializePlaylists();

        subViewRoot.setContent(songTableView);
        songTableViewController.setSongs(MusifyApp.getLibrary().getSongs());
    }

    public void updateNowPlayingButton() {
        Song song = MusifyApp.getNowPlaying();
        if (song != null) {
            nowPlayingTitle.setText(song.getTitle());
            nowPlayingArtist.setText("");
            //nowPlayingArtwork.setImage(song.getAlbum().getArtwork());
        } else {
            nowPlayingTitle.setText("");
            nowPlayingArtist.setText("");
        }
    }

    public void initializeTimeSlider() {
        Song song = MusifyApp.getNowPlaying();
        if (song != null) {
            timeSlider.setMin(0);
            timeSlider.setMax(song.getLengthInSeconds() * 4);
            timeSlider.setValue(0);
            timeSlider.setBlockIncrement(1);
        } else {
            timeSlider.setMin(0);
            timeSlider.setMax(1);
            timeSlider.setValue(0);
            timeSlider.setBlockIncrement(1);
        }
    }

    public void updateTimeSlider() {
        if (!timeSlider.isPressed()) {
            timeSlider.increment();
        }
    }

    public void initializeTimeLabels() {
        Song song = MusifyApp.getNowPlaying();
        if (song != null) {
            timePassed.setText("0:00");

            int minutes = song.getLengthInSeconds() / 60;
            int seconds = song.getLengthInSeconds() % 60;
            var totalTime = minutes + ":" + (seconds < 10 ? "0" + seconds : seconds);

            timeRemaining.setText(totalTime);
        } else {
            timePassed.setText("");
            timeRemaining.setText("");
        }
    }

    public void updateTimeLabels() {
        timePassed.setText(MusifyApp.getTimePassed());
        timeRemaining.setText(MusifyApp.getTimeRemaining());
    }

    private void initializePlaylists() {
        for (Playlist playlist : MusifyApp.getLibrary().getPlaylists()) {
            addNewPlaylistToUI(playlist);
        }
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
    private void onClickYourLibrary() {
        // TODO: button needed?
        songTableViewController.setPlaylist(MusifyApp.getLibrary().getLibraryPlaylist());
        //songTableViewController.setSongs(MusifyApp.getLibrary().getSongs());
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
                        if (MusifyApp.getLibrary().findPlaylistByTitle(text).isPresent()) {
                            System.out.println("TODO: Playlist already exists");
                        } else {

                            // TODO: if too long title

                            var playlist = MusifyApp.getLibrary().addPlaylist(text);

                            addNewPlaylistToUI(playlist);
                        }
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

    @FXML
    private void navigateToCurrentSong() {
        // TODO:
//        Song song = MusifyApp.getNowPlaying();
//
//        var songsController = (SongsController) loadView("ongs");
//        songsController.selectSong(song);
    }

    @FXML
    private void onClickSettings(Event e) {
        sideBar.requestFocus();
        searchField.setText("");

        // TODO:

        System.out.println("Clicked on settings");
    }

    @FXML
    public void playPause() {
        sideBar.requestFocus();

        if (MusifyApp.isPlaying()) {
            MusifyApp.pause();
        } else {
            MusifyApp.play();
        }
    }

    @FXML
    private void back() {
        sideBar.requestFocus();
        MusifyApp.back();
    }

    @FXML
    private void skip() {
        sideBar.requestFocus();
        MusifyApp.skip();
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
                    onClickYourLibrary();
                    MusifyApp.getLibrary().removePlaylist(playlist);
                    playlistBox.getChildren().remove(view);
                });
    }

    public DoubleProperty volumeProperty() {
        return volumePaneController.volumeProperty();
    }

    public void updatePlayPauseIcon(boolean isPlaying) {
        controlBox.getChildren().remove(2);
        if (isPlaying) {
            controlBox.getChildren().add(2, pauseButton);
        } else {
            controlBox.getChildren().add(2, playButton);
        }
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

                    List<Song> songResults = MusifyApp.getLibrary()
                            .getSongs()
                            .stream()
                            .filter(song -> song.getTitle().toUpperCase().contains(text))
                            .sorted((x, y) -> {
                                return compareSearchString(x.getTitle().toUpperCase(), y.getTitle().toUpperCase(), text);
                            })
                            // TODO: 10 search items
                            .limit(10)
                            .collect(Collectors.toList());

                    if (searchThread.isInterrupted()) {
                        throw new InterruptedException();
                    }

                    result = new SearchResult(songResults);

                    hasResults.set(true);

                } catch (InterruptedException ex) {
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
}
