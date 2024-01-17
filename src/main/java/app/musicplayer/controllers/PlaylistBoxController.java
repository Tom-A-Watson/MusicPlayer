/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.MusifyApp;
import app.musicplayer.model.MostPlayedPlaylist;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.RecentlyPlayedPlaylist;
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

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;

        initializePlaylist();
    }

    private void initializePlaylist() {
        playlistTitleLabel.textProperty().bind(
                Bindings.size(playlist.getSongs()).asString(playlist.getTitle() + " (%d songs)")
        );

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
        return !(playlist instanceof MostPlayedPlaylist)
                && !(playlist instanceof RecentlyPlayedPlaylist)
                && event.getGestureSource() != playlistBox
                && event.getDragboard().hasContent(Config.DRAG_SONG_LIST);
    }

    @FXML
    private void onClickMenu() {

    }
}
