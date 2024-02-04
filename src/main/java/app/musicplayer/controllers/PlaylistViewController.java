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
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.ui.FontType;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import java.util.List;

import static app.musicplayer.Config.VAR_DRAGGED_SONGS;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public final class PlaylistViewController {

    @FXML
    private HBox playlistBox;

    @FXML
    private HBox menuButton;

    @FXML
    private HBox titleBox;

    @FXML
    private Label playlistTitleLabel;

    private Playlist playlist;

    // TODO: unnecessary code, here and in ControlBox
    private PlaylistBoxHandler handler = new PlaylistBoxHandler() {
        @Override
        public void onClickPlaylistMenu(MouseEvent e, Playlist playlist) {
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
                        playlist.titleProperty().concat(Bindings.size(playlist.getSongs()).asString(" (%d)"))
                );
            }

            default -> {
                playlistTitleLabel.textProperty().bind(
                        playlist.titleProperty()
                );
            }
        }

        if (!playlist.isModifiable()) {
            playlistBox.getChildren().remove(menuButton);
            return;
        }

        PseudoClass hover = PseudoClass.getPseudoClass("hover");
        // TODO: this toggles .sideBarItem:hover from MainScene.css
        playlistBox.setOnDragEntered(event -> {
            if (canDragDrop(event)) {
                titleBox.pseudoClassStateChanged(hover, true);
            }
        });

        playlistBox.setOnDragExited(event -> {
            if (canDragDrop(event)) {
                titleBox.pseudoClassStateChanged(hover, false);
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
                List<Song> songs = FXGL.geto(VAR_DRAGGED_SONGS);

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
    private void onClickPlaylistMenu(MouseEvent e) {
        handler.onClickPlaylistMenu(e, playlist);
    }

    public interface PlaylistBoxHandler {
        void onClickPlaylistMenu(MouseEvent e, Playlist playlist);
    }
}
