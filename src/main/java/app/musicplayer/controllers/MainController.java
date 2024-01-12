package app.musicplayer.controllers;

import app.musicplayer.MusifyApp;
import app.musicplayer.model.*;
import app.musicplayer.Config;
import app.musicplayer.view.SubView;
import com.almasb.fxgl.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    private static final Logger log = Logger.get(MainController.class);

    private boolean isSideBarExpanded = true;
    private double expandedWidth = 250;
    private double collapsedWidth = 50;
    private double expandedHeight = 50;
    private double collapsedHeight = 0;
    private double searchExpanded = 180;
    private double searchCollapsed = 0;
    private SubView subViewController;
    private Stage volumePopup;
    private Stage searchPopup;
    private CountDownLatch viewLoadedLatch;

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
    @FXML private HBox volumePane;
    @FXML private VolumeBoxController volumePaneController;

    @FXML private Separator letterSeparator;

    @FXML private Pane playButton;
    @FXML private Pane pauseButton;
    @FXML private Pane loopButton;
    @FXML private Pane shuffleButton;
    @FXML private HBox controlBox;

    @FXML private TextField searchBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        resetLatch();

        // remove pause button until needed
        controlBox.getChildren().remove(3);

        frontSliderTrack.prefWidthProperty().bind(timeSlider.widthProperty().multiply(timeSlider.valueProperty().divide(timeSlider.maxProperty())));

        createSearchPopup();

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

        timeSlider.setFocusTraversable(false);

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
            // TODO: && !isTimeSliderPressed()
            if (!timeSlider.isValueChanging() && current != previous + 1) {

                int seconds = (int) Math.round(current / 4.0);
                timeSlider.setValue(seconds * 4);
                MusifyApp.seek(seconds);
            }
        });

        unloadLettersAnimation.setOnFinished(x -> {
            letterSeparator.setPrefHeight(0);
        });

        searchBox.textProperty().addListener((observable, oldText, newText) -> {
            String text = newText.trim();
            if (text.isEmpty()) {
                if (searchPopup.isShowing() && !searchHideAnimation.getStatus().equals(Status.RUNNING)) {
                    searchHideAnimation.play();
                }
            } else {
                Search.search(text);
            }
        });

        Search.hasResultsProperty().addListener((observable, hadResults, hasResults) -> {
            if (hasResults) {
                Search.SearchResult result = Search.getResult();
                Platform.runLater(() -> {
                    showSearchResults(result);
                    MusifyApp.getStage().toFront();
                });
                int height = 0;
                int songs = result.songResults().size();
                if (songs > 0) height += (songs * 50) + 50;
                if (height == 0) height = 50;
                searchPopup.setHeight(height);
            }
        });

        MusifyApp.getStage().xProperty().addListener((observable, oldValue, newValue) -> {
            if (searchPopup.isShowing() && !searchHideAnimation.getStatus().equals(Status.RUNNING)) {
                searchHideAnimation.play();
            }
        });

        MusifyApp.getStage().yProperty().addListener((observable, oldValue, newValue) -> {
            if (searchPopup.isShowing() && !searchHideAnimation.getStatus().equals(Status.RUNNING)) {
                searchHideAnimation.play();
            }
        });

        updateNowPlayingButton();
        initializeTimeSlider();
        initializeTimeLabels();
        initializePlaylists();

        loadView("songs");
    }

    void resetLatch() {
        viewLoadedLatch = new CountDownLatch(1);
    }

    CountDownLatch getLatch() {
        return viewLoadedLatch;
    }

    private void createSearchPopup() {
        try {
            Stage stage = MusifyApp.getStage();
            VBox view = new VBox();
            view.getStylesheets().add(Config.CSS + "MainStyle.css");
            view.getStyleClass().add("searchPopup");
            Stage popup = new Stage();
            popup.setScene(new Scene(view));
            popup.initStyle(StageStyle.UNDECORATED);
            popup.initOwner(stage);
            searchHideAnimation.setOnFinished(x -> popup.hide());

            popup.show();
            popup.hide();
            searchPopup = popup;

        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    public void updateNowPlayingButton() {
        Song song = MusifyApp.getNowPlaying();
        if (song != null) {
            nowPlayingTitle.setText(song.getTitle());
            nowPlayingArtist.setText(song.getArtistTitle());
            nowPlayingArtwork.setImage(song.getAlbum().getArtwork());
        } else {
            nowPlayingTitle.setText("");
            nowPlayingArtist.setText("");
            nowPlayingArtwork.setImage(null);
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
        timeSlider.increment();
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

    @SuppressWarnings("unchecked")
    private void initializePlaylists() {
        for (Playlist playlist : MusifyApp.getLibrary().getPlaylists()) {
            try {
                FXMLLoader loader = new FXMLLoader(this.getClass().getResource(Config.FXML + "PlaylistCell.fxml"));
                HBox cell = loader.load();
                Label label = (Label) cell.getChildren().get(1);
                label.setText(playlist.getTitle());

                cell.setOnMouseClicked(x -> {
                    selectView(x);
                    ((PlaylistsController) subViewController).selectPlaylist(playlist);
                });

                cell.setOnDragDetected(event -> {
                    PseudoClass pressed = PseudoClass.getPseudoClass("pressed");
                    cell.pseudoClassStateChanged(pressed, false);
                    Dragboard db = cell.startDragAndDrop(TransferMode.ANY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("Playlist");
                    db.setContent(content);
                    MusifyApp.setDraggedItem(playlist);
                    db.setDragView(cell.snapshot(null, null), 125, 25);
                    event.consume();
                });

                PseudoClass hover = PseudoClass.getPseudoClass("hover");

                cell.setOnDragEntered(event -> {
                    if (!(playlist instanceof MostPlayedPlaylist)
                            && !(playlist instanceof RecentlyPlayedPlaylist)
                            && event.getGestureSource() != cell
                            && event.getDragboard().hasString()) {

                        cell.pseudoClassStateChanged(hover, true);
                        //cell.getStyleClass().setAll("sideBarItemSelected");
                    }
                });

                cell.setOnDragExited(event -> {
                    if (!(playlist instanceof MostPlayedPlaylist)
                            && !(playlist instanceof RecentlyPlayedPlaylist)
                            && event.getGestureSource() != cell
                            && event.getDragboard().hasString()) {

                        cell.pseudoClassStateChanged(hover, false);
                        //cell.getStyleClass().setAll("sideBarItem");
                    }
                });

                cell.setOnDragOver(event -> {
                    if (!(playlist instanceof MostPlayedPlaylist)
                            && !(playlist instanceof RecentlyPlayedPlaylist)
                            && event.getGestureSource() != cell
                            && event.getDragboard().hasString()) {

                        event.acceptTransferModes(TransferMode.ANY);
                    }
                    event.consume();
                });

                cell.setOnDragDropped(event -> {
                    String dragString = event.getDragboard().getString();
                    new Thread(() -> {
                        switch (dragString) {
                            case "Artist":
                                Artist artist = (Artist) MusifyApp.getDraggedItem();
                                for (Album album : artist.albums()) {
                                    for (Song song : album.getSongs()) {
                                        if (!playlist.getSongs().contains(song)) {
                                            playlist.addSong(song);
                                        }
                                    }
                                }
                                break;
                            case "Album":
                                Album album = (Album) MusifyApp.getDraggedItem();
                                for (Song song : album.getSongs()) {
                                    if (!playlist.getSongs().contains(song)) {
                                        playlist.addSong(song);
                                    }
                                }
                                break;
                            case "Playlist":
                                Playlist list = (Playlist) MusifyApp.getDraggedItem();
                                for (Song song : list.getSongs()) {
                                    if (!playlist.getSongs().contains(song)) {
                                        playlist.addSong(song);
                                    }
                                }
                                break;
                            case "Song":
                                Song song = (Song) MusifyApp.getDraggedItem();
                                if (!playlist.getSongs().contains(song)) {
                                    playlist.addSong(song);
                                }
                                break;
                            case "List":
                                ObservableList<Song> songs = (ObservableList<Song>) MusifyApp.getDraggedItem();
                                for (Song s : songs) {
                                    if (!playlist.getSongs().contains(s)) {
                                        playlist.addSong(s);
                                    }
                                }
                                break;
                        }
                    }).start();

                    event.consume();
                });

                playlistBox.getChildren().add(cell);

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    @FXML
    private void selectView(Event e) {

        HBox eventSource = ((HBox)e.getSource());

        eventSource.requestFocus();

        Optional<Node> previous = sideBar.getChildren().stream()
                .filter(x -> x.getStyleClass().get(0).equals("sideBarItemSelected")).findFirst();

        if (previous.isPresent()) {
            HBox previousItem = (HBox) previous.get();
            previousItem.getStyleClass().setAll("sideBarItem");
        } else {
            previous = playlistBox.getChildren().stream()
                    .filter(x -> x.getStyleClass().get(0).equals("sideBarItemSelected")).findFirst();
            if (previous.isPresent()) {
                HBox previousItem = (HBox) previous.get();
                previousItem.getStyleClass().setAll("sideBarItem");
            }
        }

        ObservableList<String> styles = eventSource.getStyleClass();

        if (styles.get(0).equals("sideBarItem")) {
            styles.setAll("sideBarItemSelected");
            loadView(eventSource.getId());
        } else if (styles.get(0).equals("bottomBarItem")) {
            loadView(eventSource.getId());
        }
    }

    private static Playlist getPlaylist(String title) {
        return MusifyApp.getLibrary().findPlaylistByTitle(title).get();
    }
    
    @FXML
    private void newPlaylist() {

        if (!newPlaylistAnimation.getStatus().equals(Status.RUNNING)) {

            try {

                FXMLLoader loader = new FXMLLoader(this.getClass().getResource(Config.FXML + "PlaylistCell.fxml"));
                HBox cell = loader.load();

                Label label = (Label) cell.getChildren().get(1);
                label.setVisible(false);
                HBox.setMargin(label, new Insets(0, 0, 0, 0));

                TextField textBox = new TextField();
                textBox.setPrefHeight(30);
                cell.getChildren().add(textBox);
                HBox.setMargin(textBox, new Insets(10, 10, 10, 9));

                textBox.focusedProperty().addListener((obs, oldValue, newValue) -> {
                    if (oldValue && !newValue) {
                        String text = textBox.getText().equals("") ? "New Playlist" : textBox.getText();
                        text = checkDuplicatePlaylist(text, 0);
                        label.setText(text);
                        cell.getChildren().remove(textBox);
                        HBox.setMargin(label, new Insets(10, 10, 10, 10));
                        label.setVisible(true);

                        MusifyApp.getLibrary().addPlaylist(text);
                    }
                });

                textBox.setOnKeyPressed(x -> {
                    if (x.getCode() == KeyCode.ENTER)  {
                        sideBar.requestFocus();
                    }
                });

                cell.setOnMouseClicked(x -> {
                    selectView(x);
                    Playlist playlist = getPlaylist(label.getText());
                    ((PlaylistsController) subViewController).selectPlaylist(playlist);
                });

                cell.setOnDragDetected(event -> {
                    PseudoClass pressed = PseudoClass.getPseudoClass("pressed");
                    cell.pseudoClassStateChanged(pressed, false);
                    Playlist playlist = getPlaylist(label.getText());
                    Dragboard db = cell.startDragAndDrop(TransferMode.ANY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("Playlist");
                    db.setContent(content);
                    MusifyApp.setDraggedItem(playlist);
                    SnapshotParameters sp = new SnapshotParameters();
                    sp.setTransform(Transform.scale(1.5, 1.5));
                    db.setDragView(cell.snapshot(sp, null));
                    event.consume();
                });

                PseudoClass hover = PseudoClass.getPseudoClass("hover");

                cell.setOnDragEntered(event -> {
                    Playlist playlist = getPlaylist(label.getText());
                    if (!(playlist instanceof MostPlayedPlaylist)
                            && !(playlist instanceof RecentlyPlayedPlaylist)
                            && event.getGestureSource() != cell
                            && event.getDragboard().hasString()) {

                        cell.pseudoClassStateChanged(hover, true);
                    }
                });

                cell.setOnDragExited(event -> {
                    Playlist playlist = getPlaylist(label.getText());
                    if (!(playlist instanceof MostPlayedPlaylist)
                            && !(playlist instanceof RecentlyPlayedPlaylist)
                            && event.getGestureSource() != cell
                            && event.getDragboard().hasString()) {

                        cell.pseudoClassStateChanged(hover, false);
                    }
                });

                cell.setOnDragOver(event -> {
                    Playlist playlist = getPlaylist(label.getText());
                    if (!(playlist instanceof MostPlayedPlaylist)
                            && !(playlist instanceof RecentlyPlayedPlaylist)
                            && event.getGestureSource() != cell
                            && event.getDragboard().hasString()) {

                        event.acceptTransferModes(TransferMode.ANY);
                    }
                    event.consume();
                });

                cell.setOnDragDropped(event -> {
                    Playlist playlist = getPlaylist(label.getText());
                    String dragString = event.getDragboard().getString();
                    new Thread(() -> {
                        switch (dragString) {
                            case "Artist":
                                Artist artist = (Artist) MusifyApp.getDraggedItem();
                                for (Album album : artist.albums()) {
                                    for (Song song : album.getSongs()) {
                                        if (!playlist.getSongs().contains(song)) {
                                            playlist.addSong(song);
                                        }
                                    }
                                }
                                break;
                            case "Album":
                                Album album = (Album) MusifyApp.getDraggedItem();
                                for (Song song : album.getSongs()) {
                                    if (!playlist.getSongs().contains(song)) {
                                        playlist.addSong(song);
                                    }
                                }
                                break;
                            case "Playlist":
                                Playlist list = (Playlist) MusifyApp.getDraggedItem();
                                for (Song song : list.getSongs()) {
                                    if (!playlist.getSongs().contains(song)) {
                                        playlist.addSong(song);
                                    }
                                }
                                break;
                            case "Song":
                                Song song = (Song) MusifyApp.getDraggedItem();
                                if (!playlist.getSongs().contains(song)) {
                                    playlist.addSong(song);
                                }
                                break;
                            case "List":
                                ObservableList<Song> songs = (ObservableList<Song>) MusifyApp.getDraggedItem();
                                for (Song s : songs) {
                                    if (!playlist.getSongs().contains(s)) {
                                        playlist.addSong(s);
                                    }
                                }
                                break;
                        }
                    }).start();

                    event.consume();
                });

                cell.setPrefHeight(0);
                cell.setOpacity(0);

                playlistBox.getChildren().add(1, cell);

                textBox.requestFocus();

            } catch (Exception e) {

                e.printStackTrace();
            }

            newPlaylistAnimation.play();
        }
    }

    private String checkDuplicatePlaylist(String text, int i) {
        for (Playlist playlist : MusifyApp.getLibrary().getPlaylists()) {
            if (playlist.getTitle().equals(text)) {

                int index = text.lastIndexOf(' ') + 1;
                if (index != 0) {
                    try {
                        i = Integer.parseInt(text.substring(index));
                    } catch (Exception ex) {
                        // do nothing
                    }
                }

                i++;

                if (i == 1) {
                    text = checkDuplicatePlaylist(text + " " + i, i);
                } else {
                    text = checkDuplicatePlaylist(text.substring(0, index) + i, i);
                }
                break;
            }
        }

        return text;
    }

    public SubView loadView(String viewName) {
        try {

            boolean loadLetters;
            boolean unloadLetters;

            switch (viewName.toLowerCase()) {
                case "songs":
                    if (subViewController instanceof SongsController) {
                        loadLetters = false;
                        unloadLetters = false;
                    } else {
                        loadLetters = true;
                        unloadLetters = false;
                    }
                    break;
                default:
                    if (subViewController instanceof SongsController) {
                        loadLetters = false;
                        unloadLetters = true;
                    } else {
                        loadLetters = false;
                        unloadLetters = false;
                    }
                    break;
            }

            final boolean loadLettersFinal = loadLetters;
            final boolean unloadLettersFinal = unloadLetters;

            String fileName = viewName.substring(0, 1).toUpperCase() + viewName.substring(1) + ".fxml";
            fileName = "/assets/ui/" + fileName;

            System.out.println("Loading: " + fileName);

            FXMLLoader loader = new FXMLLoader(this.getClass().getResource(fileName));
            Node view = loader.load();

            CountDownLatch latch = new CountDownLatch(1);

            Task<Void> task = new Task<Void>() {
                @Override protected Void call() throws Exception {
                    Platform.runLater(() -> {
                        MusifyApp.getLibrary().getSongs().stream().filter(x -> x.isSelected()).forEach(x -> x.setSelected(false));
                        subViewRoot.setVisible(false);
                        subViewRoot.setContent(view);
                        subViewRoot.getContent().setOpacity(0);
                        latch.countDown();
                    });
                    return null;
                }
            };

            task.setOnSucceeded(x -> new Thread(() -> {
                try {
                    latch.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    subViewRoot.setVisible(true);
                    if (loadLettersFinal) {
                        loadLettersAnimation.play();
                    }
                    loadViewAnimation.play();
                });
            }).start());

            Thread thread = new Thread(task);

            unloadViewAnimation.setOnFinished(x -> thread.start());

            loadViewAnimation.setOnFinished(x -> viewLoadedLatch.countDown());

            if (subViewRoot.getContent() != null) {
                if (unloadLettersFinal) {
                    unloadLettersAnimation.play();
                }
                unloadViewAnimation.play();
            } else {
                subViewRoot.setContent(view);
                if (loadLettersFinal) {
                    loadLettersAnimation.play();
                }
                loadViewAnimation.play();
            }

            subViewController = loader.getController();
            return subViewController;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @FXML
    private void navigateToCurrentSong() {

        Optional<Node> previous = sideBar.getChildren().stream()
                .filter(x -> x.getStyleClass().get(0).equals("sideBarItemSelected")).findFirst();

        if (previous.isPresent()) {
            HBox previousItem = (HBox) previous.get();
            previousItem.getStyleClass().setAll("sideBarItem");
        } else {
            previous = playlistBox.getChildren().stream()
                    .filter(x -> x.getStyleClass().get(0).equals("sideBarItemSelected")).findFirst();
            if (previous.isPresent()) {
                HBox previousItem = (HBox) previous.get();
                previousItem.getStyleClass().setAll("sideBarItem");
            }
        }

        sideBar.getChildren().get(2).getStyleClass().setAll("sideBarItemSelected");

        Song song = MusifyApp.getNowPlaying();

        var songsController = (SongsController) loadView("Songs");
        songsController.selectSong(song);
    }

    @FXML
    private void onClickSettings(Event e) {
        sideBar.requestFocus();
        searchBox.setText("");

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

    public void showSearchResults(Search.SearchResult result) {
        VBox root = (VBox) searchPopup.getScene().getRoot();
        ObservableList<Node> list = root.getChildren();
        list.clear();

        if (!result.songResults().isEmpty()) {
            Label header = new Label("Songs");
            header.setTextFill(Color.DARKGRAY);
            list.add(header);
            VBox.setMargin(header, new Insets(10, 10, 10, 10));

            result.songResults().forEach(song -> {
                HBox cell = new HBox();
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPrefWidth(226);
                cell.setPrefHeight(50);
                Label label = new Label(song.getTitle());
                label.setTextOverrun(OverrunStyle.CLIP);
                label.getStyleClass().setAll("searchLabel");
                cell.getChildren().add(label);
                HBox.setMargin(label, new Insets(10, 10, 10, 10));
                cell.getStyleClass().add("searchResult");
                cell.setOnMouseClicked(event -> {

                    var songsController = (SongsController) loadView("Songs");
                    songsController.selectSong(song);

                    searchBox.setText("");
                    sideBar.requestFocus();
                });
                list.add(cell);
            });
        }

        if (list.isEmpty()) {
            Label label = new Label("No Results");
            list.add(label);
            VBox.setMargin(label, new Insets(10, 10, 10, 10));
        }

        if (!searchPopup.isShowing()) {
            Stage stage = MusifyApp.getStage();
            searchPopup.setX(stage.getX() + 18);
            searchPopup.setY(stage.getY() + 80);
            searchPopup.show();
            searchShowAnimation.play();
        }
    }

    public Slider getVolumeSlider() {
        return volumePaneController.getSlider();
    }

//    public boolean isTimeSliderPressed() {
//        return sliderSkin.getThumb().isPressed() || sliderSkin.getTrack().isPressed();
//    }

    public SubView getSubViewController() {

        return subViewController;
    }

    ScrollPane getScrollPane() {
        return this.subViewRoot;
    }

    VBox getPlaylistBox() {
        return playlistBox;
    }

    public void updatePlayPauseIcon(boolean isPlaying) {
        controlBox.getChildren().remove(2);
        if (isPlaying) {
            controlBox.getChildren().add(2, pauseButton);
        } else {
            controlBox.getChildren().add(2, playButton);
        }
    }

    private void setSlideDirection() {
        isSideBarExpanded = !isSideBarExpanded;
    }

    private Animation searchShowAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }

        protected void interpolate(double frac) {
            searchPopup.setOpacity(frac);
        }
    };

    private Animation searchHideAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            searchPopup.setOpacity(1.0 - frac);
        }
    };

    private Animation loadViewAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            subViewRoot.setVvalue(0);
            double curHeight = collapsedHeight + (expandedHeight - collapsedHeight) * (frac);
            subViewRoot.getContent().setTranslateY(expandedHeight - curHeight);
            subViewRoot.getContent().setOpacity(frac);
        }
    };

    private Animation unloadViewAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            double curHeight = collapsedHeight + (expandedHeight - collapsedHeight) * (1 - frac);
            subViewRoot.getContent().setTranslateY(expandedHeight - curHeight);
            subViewRoot.getContent().setOpacity(1 - frac);
        }
    };

    private Animation loadLettersAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            letterSeparator.setPrefHeight(25);
            letterSeparator.setOpacity(frac);
        }
    };

    private Animation unloadLettersAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            letterSeparator.setOpacity(1.0 - frac);
        }
    };

    private Animation newPlaylistAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(500));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            HBox cell = (HBox) playlistBox.getChildren().get(1);
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
                            .collect(Collectors.toList());

                    if (searchThread.isInterrupted()) {
                        throw new InterruptedException();
                    }

                    if (songResults.size() > 3)
                        songResults = songResults.subList(0, 3);

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
}
