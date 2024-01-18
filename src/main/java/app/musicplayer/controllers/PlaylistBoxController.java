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
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public final class PlaylistBoxController {

    @FXML
    private HBox playlistBox;

    @FXML
    private Label playlistTitleLabel;

    private Playlist playlist;

    // TODO: unnecessary code, here and in ControlBox
    private PlaylistBoxHandler handler = new PlaylistBoxHandler() {
        @Override
        public void onClickRemovePlaylist(Playlist playlist) {
        }
    };

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;

        initializePlaylist();
    }

    public void setHandler(PlaylistBoxHandler handler) {
        this.handler = handler;
    }

    private void initializePlaylist() {
        switch (playlist.getType()) {
            case ALL_SONGS, USER_CREATED -> {
                playlistTitleLabel.textProperty().bind(
                        Bindings.size(playlist.getSongs()).asString(playlist.getTitle() + " (%d)")
                );
            }

            default -> {
                playlistTitleLabel.textProperty().bind(
                        playlist.titleProperty()
                );
            }
        }

        // TODO: drag playlist to other playlists
//        playlistBox.setOnDragDetected(event -> {
//            PseudoClass pressed = PseudoClass.getPseudoClass("pressed");
//            playlistBox.pseudoClassStateChanged(pressed, false);
//            Dragboard db = playlistBox.startDragAndDrop(TransferMode.ANY);
//            ClipboardContent content = new ClipboardContent();
//            content.putString("Playlist");
//            db.setContent(content);
//            MusifyApp.setDraggedItem(playlist);
//            db.setDragView(playlistBox.snapshot(null, null), 125, 25);
//            event.consume();
//        });

        PseudoClass hover = PseudoClass.getPseudoClass("hover");
        // TODO: this toggles .sideBarItem:hover from MainScene.css
        playlistBox.setOnDragEntered(event -> {
            if (canDragDrop(event)) {
                playlistBox.pseudoClassStateChanged(hover, true);
            }
        });

        playlistBox.setOnDragExited(event -> {
            if (canDragDrop(event)) {
                playlistBox.pseudoClassStateChanged(hover, false);
            }
        });

        playlistBox.setOnDragOver(event -> {
            if (canDragDrop(event)) {
                event.acceptTransferModes(TransferMode.ANY);
            }

            event.consume();
        });

        playlistBox.setOnDragDropped(event -> {
            if (event.getDragboard().hasContent(Config.DRAG_SONG_LIST)) {
                // TODO: extra ref to App
                List<Song> songs = MusifyApp.getDraggedItems();

                songs.forEach(playlist::addSong);
            }

            event.consume();
        });
    }

    private boolean canDragDrop(DragEvent event) {
        return playlist.isModifiable()
                && event.getGestureSource() != playlistBox
                && event.getDragboard().hasContent(Config.DRAG_SONG_LIST);
    }

    @FXML
    private void onClickMenu() {
        handler.onClickRemovePlaylist(playlist);
    }

    public interface PlaylistBoxHandler {
        void onClickRemovePlaylist(Playlist playlist);
    }
}
