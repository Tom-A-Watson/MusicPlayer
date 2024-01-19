/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.FXGLMusicApp;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public final class MediaPaneController implements Initializable {

    @FXML
    private HBox volumePane;
    @FXML
    private VolumeBoxController volumePaneController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        volumePaneController.mutedProperty().addListener((observable, wasMuted, isMuted) -> {
            //FXGLMusicApp.mute(isMuted);
        });
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
//        sideBar.requestFocus();
//        searchField.setText("");
//
//        // TODO:
//
//        System.out.println("Clicked on settings");
    }

    @FXML
    public void playPause() {
//        sideBar.requestFocus();
//
//        if (FXGLMusicApp.isPlaying()) {
//            FXGLMusicApp.pause();
//        } else {
//            FXGLMusicApp.play();
//        }
    }

    @FXML
    private void back() {
//        sideBar.requestFocus();
//        FXGLMusicApp.back();
    }

    @FXML
    private void skip() {
//        sideBar.requestFocus();
//        FXGLMusicApp.skip();
    }
}
