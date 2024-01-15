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
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public final class ControlBoxController {

    private ControlBoxHandler handler = new ControlBoxHandler() {
        @Override
        public void onClickPlaySong() {}

        @Override
        public void onClickAddToPlaylist() {}
    };

    @FXML
    private Pane playButton;
    @FXML
    private Pane playlistButton;

    private ContextMenu contextMenu;

    private final Animation showMenuAnimation = new Transition() {
        {
            setCycleDuration(Duration.millis(250));
            setInterpolator(Interpolator.EASE_BOTH);
        }

        @Override
        protected void interpolate(double frac) {
            contextMenu.setOpacity(frac);
        }
    };

    public void setHandler(ControlBoxHandler handler) {
        this.handler = handler;
    }

    @FXML
    private void onClickPlaySong(Event e) {
        handler.onClickPlaySong();
    }

    @FXML
    private void onClickAddToPlaylist(Event e) {
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

    public interface ControlBoxHandler {
        void onClickPlaySong();
        void onClickAddToPlaylist();
    }
}
