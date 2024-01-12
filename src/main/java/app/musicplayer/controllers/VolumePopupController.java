/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.MusicPlayerApp;
import com.almasb.fxgl.logging.Logger;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.ResourceBundle;

public final class VolumePopupController implements Initializable {

    private static final Logger log = Logger.get(VolumePopupController.class);

    @FXML
    private Slider volumeSlider;

    @FXML
    private Region frontVolumeTrack;

    @FXML
    private Label volumeLabel;

    @FXML
    private Pane muteButton;

    @FXML
    private Pane mutedButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        frontVolumeTrack.prefWidthProperty().bind(
                volumeSlider.widthProperty().subtract(30).multiply(volumeSlider.valueProperty().divide(volumeSlider.maxProperty()))
        );

        volumeLabel.textProperty().bind(volumeSlider.valueProperty().asString("%.0f"));

        volumeSlider.setOnMousePressed(x -> {
            if (mutedButton.isVisible()) {
                onClickMute();
            }
        });
    }

    Slider getSlider() {
        return volumeSlider;
    }

    @FXML
    private void onClickVolume() {
        MusicPlayerApp.getMainController().volumeClick();
    }

    @FXML
    private void onClickMute() {
        PseudoClass muted = PseudoClass.getPseudoClass("muted");
        boolean isMuted = mutedButton.isVisible();
        muteButton.setVisible(isMuted);
        mutedButton.setVisible(!isMuted);
        volumeSlider.pseudoClassStateChanged(muted, !isMuted);
        frontVolumeTrack.pseudoClassStateChanged(muted, !isMuted);
        volumeLabel.pseudoClassStateChanged(muted, !isMuted);
        MusicPlayerApp.mute(isMuted);
    }
}
