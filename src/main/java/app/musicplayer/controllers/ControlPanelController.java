/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class ControlPanelController implements Initializable {

    @FXML private Pane playButton;
    @FXML private Pane playlistButton;

    private ContextMenu contextMenu;

    private Animation showMenuAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }
        protected void interpolate(double frac) {
            contextMenu.setOpacity(frac);
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    @FXML
    private void playSong(Event e) {
        System.out.println("TODO: PLAY SONG" + e);

        // TODO:
//        SubView controller = MusifyApp.getMainController().getSubViewController();
//        controller.play();
//        e.consume();
    }

    @FXML
    private void addToPlaylist(Event e) {
        // TODO:
        System.out.println("TODO ADD TO PLAYLIST: " + e);


        // Gets the mouse event coordinates in the screen to display the context menu in this location.
//        MouseEvent mouseEvent = (MouseEvent) e;
//        double x = mouseEvent.getScreenX();
//        double y = mouseEvent.getScreenY();
//
//        // Retrieves the selected song to add to the desired playlist.
//        Song selectedSong = MusifyApp.getMainController().getSelectedSong();
//
//        List<Playlist> playlists = MusifyApp.getLibrary().getPlaylists();
//
//        // Retrieves all the playlist titles to create menu items.
//        ObservableList<String> playlistTitles = FXCollections.observableArrayList();
//        for (Playlist playlist : playlists) {
//            String title = playlist.getTitle();
//            if (!(title.equals("Most Played") || title.equals("Recently Played")) &&
//                    !playlist.getSongs().contains(selectedSong)) {
//                playlistTitles.add(title);
//            }
//        }
//
//        contextMenu = new ContextMenu();
//
//        MenuItem playing = new MenuItem("Playing");
//        playing.setStyle("-fx-text-fill: black");
//        playing.setOnAction(e1 -> {
//            MusifyApp.addSongToNowPlayingList(selectedSong);
//        });
//
//        contextMenu.getItems().add(playing);
//
//        if (!playlistTitles.isEmpty()) {
//            SeparatorMenuItem item = new SeparatorMenuItem();
//            item.getContent().setStyle(
//                    "-fx-border-width: 1 0 0 0; " +
//                            "-fx-border-color: #c2c2c2; " +
//                            "-fx-border-insets: 5 5 5 5;");
//            contextMenu.getItems().add(item);
//        }
//
//        // Creates a menu item for each playlist title and adds it to the context menu.
//        for (String title : playlistTitles) {
//            MenuItem item = new MenuItem(title);
//            item.setStyle("-fx-text-fill: black");
//
//            item.setOnAction(e2 -> {
//                // Finds the desired playlist and adds the currently selected song to it.
//                String targetPlaylistTitle = item.getText();
//
//                // Finds the correct playlist and adds the song to it.
//                playlists.forEach(playlist -> {
//                    if (playlist.getTitle().equals(targetPlaylistTitle)) {
//                        playlist.addSong(selectedSong);
//                    }
//                });
//            });
//
//            contextMenu.getItems().add(item);
//        }
//
//        contextMenu.setOpacity(0);
//        contextMenu.show(playButton, x, y);
//        showMenuAnimation.play();
//
//        e.consume();
    }
}
