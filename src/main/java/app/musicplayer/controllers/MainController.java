/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.FXGLMusicApp;
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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
//        controlBox.getChildren().remove(3);
//
//        frontSliderTrack.prefWidthProperty().bind(timeSlider.widthProperty().multiply(timeSlider.valueProperty().divide(timeSlider.maxProperty())));
//
//        PseudoClass active = PseudoClass.getPseudoClass("active");
//        loopButton.setOnMouseClicked(x -> {
//            sideBar.requestFocus();
//            //FXGLMusicApp.toggleLoop();
//            //loopButton.pseudoClassStateChanged(active, FXGLMusicApp.isLoopActive());
//        });
//        shuffleButton.setOnMouseClicked(x -> {
//            sideBar.requestFocus();
//            //FXGLMusicApp.toggleShuffle();
//            //shuffleButton.pseudoClassStateChanged(active, FXGLMusicApp.isShuffleActive());
//        });
//
//        timeSlider.valueChangingProperty().addListener((slider, wasChanging, isChanging) -> {
//            if (wasChanging) {
//                int seconds = (int) Math.round(timeSlider.getValue() / 4.0);
//                timeSlider.setValue(seconds * 4);
//                //FXGLMusicApp.seek(seconds);
//            }
//        });
//
//        timeSlider.valueProperty().addListener((slider, oldValue, newValue) -> {
//            double previous = oldValue.doubleValue();
//            double current = newValue.doubleValue();
//            if (!timeSlider.isValueChanging() && current != previous + 1 && !timeSlider.isPressed()) {
//                int seconds = (int) Math.round(current / 4.0);
//                timeSlider.setValue(seconds * 4);
//                //FXGLMusicApp.seek(seconds);
//            }
//        });
//
//        searchField.textProperty().addListener((observable, oldText, newText) -> {
//            String text = newText.trim();
//            if (text.isEmpty()) {
//                // TODO: what if playlist is selected
//                //songTableViewController.setSongs(FXGLMusicApp.getLibrary().getSongs());
//
//            } else {
//                Search.search(text);
//            }
//        });
//
//        Search.hasResultsProperty().addListener((observable, hadResults, hasResults) -> {
//            if (hasResults) {
//                Search.SearchResult result = Search.getResult();
//                Platform.runLater(() -> {
//
//                    songTableViewController.setSongs(FXCollections.observableArrayList(result.songResults()));
//                });
//            }
//        });




        updateNowPlayingButton();
        initializeTimeSlider();
        initializeTimeLabels();
        initializePlaylists();

        subViewRoot.setContent(songTableView);
        //songTableViewController.setSongs(FXGLMusicApp.getLibrary().getSongs());
    }

    public void updateNowPlayingButton() {
//        Song song = FXGLMusicApp.getNowPlaying();
//        if (song != null) {
//            nowPlayingTitle.setText(song.getTitle());
//            nowPlayingArtist.setText("");
//            //nowPlayingArtwork.setImage(song.getAlbum().getArtwork());
//        } else {
//            nowPlayingTitle.setText("");
//            nowPlayingArtist.setText("");
//        }
    }

    public void initializeTimeSlider() {
//        Song song = FXGLMusicApp.getNowPlaying();
//        if (song != null) {
//            timeSlider.setMin(0);
//            timeSlider.setMax(song.getLengthInSeconds() * 4);
//            timeSlider.setValue(0);
//            timeSlider.setBlockIncrement(1);
//        } else {
//            timeSlider.setMin(0);
//            timeSlider.setMax(1);
//            timeSlider.setValue(0);
//            timeSlider.setBlockIncrement(1);
//        }
    }

    public void updateTimeSlider() {
        if (!timeSlider.isPressed()) {
            timeSlider.increment();
        }
    }

    public void initializeTimeLabels() {
//        Song song = FXGLMusicApp.getNowPlaying();
//        if (song != null) {
//            timePassed.setText("0:00");
//
//            int minutes = song.getLengthInSeconds() / 60;
//            int seconds = song.getLengthInSeconds() % 60;
//            var totalTime = minutes + ":" + (seconds < 10 ? "0" + seconds : seconds);
//
//            timeRemaining.setText(totalTime);
//        } else {
//            timePassed.setText("");
//            timeRemaining.setText("");
//        }
    }

    public void updateTimeLabels() {
//        timePassed.setText(FXGLMusicApp.getTimePassed());
//        timeRemaining.setText(FXGLMusicApp.getTimeRemaining());
    }

    private void initializePlaylists() {
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
    private void onClickYourLibrary() {
        // TODO: button needed?
        //songTableViewController.setPlaylist(FXGLMusicApp.getLibrary().getLibraryPlaylist());
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
                    onClickYourLibrary();
                    //FXGLMusicApp.getLibrary().removePlaylist(playlist);
                    playlistBox.getChildren().remove(view);
                });
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
//    private static MainController mainController;
//    private static MediaPlayer mediaPlayer;
//    private static List<Song> nowPlayingList;
//    private static int nowPlayingIndex;
//    private static Song nowPlaying;
//    private static int timerCounter = 0;
//    private static int secondsPlayed = 0;
//    private static boolean isLoopActive = false;
//    private static boolean isShuffleActive = false;
//    private static boolean isMuted = false;
//    private static List<Song> draggedItems = new ArrayList<>();
//    private static ScheduledExecutorService executorService;
//
//    private static Library library;
//
//    private static Stage stage;
//
//    void start() {
//        executorService = Executors.newScheduledThreadPool(4);
//        executorService.scheduleAtFixedRate(new TimeUpdater(), 0, 250, TimeUnit.MILLISECONDS);
//    }
//
//
//    private static class TimeUpdater implements Runnable {
//
//        @Override
//        public void run() {
//            if (!isPlaying())
//                return;
//
//            int length = getNowPlaying().getLengthInSeconds() * 4;
//
//            Platform.runLater(() -> {
//                if (timerCounter < length) {
//                    if (++timerCounter % 4 == 0) {
//                        mainController.updateTimeLabels();
//                        secondsPlayed++;
//                    }
//
//                    // called every tick (250 ms) because in main controller max value is length in seconds * 4
//                    mainController.updateTimeSlider();
//                }
//            });
//        }
//    }
//
//    /**
//     * Plays selected song.
//     */
//    public static void play() {
//        if (mediaPlayer != null && !isPlaying()) {
//            mediaPlayer.play();
//            mainController.updatePlayPauseIcon(true);
//        }
//    }
//
//    /**
//     * Checks if a song is playing.
//     */
//    public static boolean isPlaying() {
//        return mediaPlayer != null && MediaPlayer.Status.PLAYING.equals(mediaPlayer.getStatus());
//    }
//
//    /**
//     * Pauses selected song.
//     */
//    public static void pause() {
//        if (isPlaying()) {
//            mediaPlayer.pause();
//            mainController.updatePlayPauseIcon(false);
//        }
//    }
//
//    public static void seek(int seconds) {
//        if (mediaPlayer != null) {
//            mediaPlayer.seek(new Duration(seconds * 1000));
//            timerCounter = seconds * 4;
//            mainController.updateTimeLabels();
//        }
//    }
//
//    /**
//     * Skips song.
//     */
//    public static void skip() {
//        if (nowPlayingIndex < nowPlayingList.size() - 1) {
//            boolean isPlaying = isPlaying();
//            mainController.updatePlayPauseIcon(isPlaying);
//            setNowPlaying(nowPlayingList.get(nowPlayingIndex + 1));
//            if (isPlaying) {
//                play();
//            }
//        } else if (isLoopActive) {
//            boolean isPlaying = isPlaying();
//            mainController.updatePlayPauseIcon(isPlaying);
//            nowPlayingIndex = 0;
//            setNowPlaying(nowPlayingList.get(nowPlayingIndex));
//            if (isPlaying) {
//                play();
//            }
//        } else {
//            mainController.updatePlayPauseIcon(false);
//            nowPlayingIndex = 0;
//            setNowPlaying(nowPlayingList.get(nowPlayingIndex));
//        }
//    }
//
//    public static void back() {
//        if (timerCounter > 20 || nowPlayingIndex == 0) {
//            mainController.initializeTimeSlider();
//            seek(0);
//        } else {
//            boolean isPlaying = isPlaying();
//            setNowPlaying(nowPlayingList.get(nowPlayingIndex - 1));
//            if (isPlaying) {
//                play();
//            }
//        }
//    }
//
//    public static void mute(boolean isMuted) {
//        FXGLMusicApp.isMuted = !isMuted;
//        if (mediaPlayer != null) {
//            mediaPlayer.setMute(!isMuted);
//        }
//    }
//
//    public static void toggleLoop() {
//        isLoopActive = !isLoopActive;
//    }
//
//    public static boolean isLoopActive() {
//        return isLoopActive;
//    }
//
//    public static void toggleShuffle() {
//        isShuffleActive = !isShuffleActive;
//
//        if (isShuffleActive) {
//            Collections.shuffle(nowPlayingList);
//        } else {
//            Collections.sort(nowPlayingList);
//        }
//
//        nowPlayingIndex = nowPlayingList.indexOf(nowPlaying);
//    }
//
//    public static boolean isShuffleActive() {
//        return isShuffleActive;
//    }
//
//    public static Stage getStage() {
//        return stage;
//    }
//
//    public static MainController getMainController() {
//        return mainController;
//    }
//
//    /**
//     * Gets currently playing song list.
//     * @return arraylist of now playing songs
//     */
//    public static ArrayList<Song> getNowPlayingList() {
//        return nowPlayingList == null ? new ArrayList<>() : new ArrayList<>(nowPlayingList);
//    }
//
//    public static void addSongToNowPlayingList(Song song) {
//        if (!nowPlayingList.contains(song)) {
//            nowPlayingList.add(song);
//        }
//    }
//
//    public static void setNowPlayingList(List<Song> list) {
//        nowPlayingList = new ArrayList<>(list);
//    }
//
//    public static void setNowPlaying(Song song) {
//        if (nowPlayingList.contains(song)) {
//
//            updatePlayCount();
//            nowPlayingIndex = nowPlayingList.indexOf(song);
//            if (nowPlaying != null) {
//                nowPlaying.setPlaying(false);
//            }
//            nowPlaying = song;
//            nowPlaying.setPlaying(true);
//            if (mediaPlayer != null) {
//                mediaPlayer.stop();
//                mediaPlayer.dispose();
//            }
//
//            timerCounter = 0;
//            secondsPlayed = 0;
//
//            Media media = new Media(song.getFile().toUri().toString());
//            mediaPlayer = new MediaPlayer(media);
//            mediaPlayer.volumeProperty().bind(mainController.volumeProperty().divide(200));
//            mediaPlayer.setOnEndOfMedia(FXGLMusicApp::skip);
//            mediaPlayer.setMute(isMuted);
//            mainController.updateNowPlayingButton();
//            mainController.initializeTimeSlider();
//            mainController.initializeTimeLabels();
//        }
//    }
//
//    private static void updatePlayCount() {
//        if (nowPlaying != null) {
//            int length = nowPlaying.getLengthInSeconds();
//            if ((100 * secondsPlayed / length) > 50) {
//                songPlayed(nowPlaying);
//            }
//        }
//    }
//
//    private static void songPlayed(Song song) {
//        song.playCountProperty().set(song.playCountProperty().get() + 1);
//        song.setPlayDate(LocalDateTime.now());
//    }
//
//    public static Song getNowPlaying() {
//        return nowPlaying;
//    }
//
//    public static String getTimePassed() {
//        int secondsPassed = timerCounter / 4;
//        int minutes = secondsPassed / 60;
//        int seconds = secondsPassed % 60;
//        return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
//    }
//
//    public static String getTimeRemaining() {
//        int secondsPassed = timerCounter / 4;
//        int totalSeconds = getNowPlaying().getLengthInSeconds();
//        int secondsRemaining = totalSeconds - secondsPassed;
//        int minutes = secondsRemaining / 60;
//        int seconds = secondsRemaining % 60;
//        return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
//    }
//
//    public static void setDraggedItems(List<Song> draggedItems) {
//        FXGLMusicApp.draggedItems = draggedItems;
//    }
//
//    public static List<Song> getDraggedItems() {
//        return draggedItems;
//    }
//
//    public static Library getLibrary() {
//        return library;
//    }
//
//    public static ExecutorService getExecutorService() {
//        return executorService;
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
