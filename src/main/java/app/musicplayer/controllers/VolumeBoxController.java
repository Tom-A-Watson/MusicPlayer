/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import com.almasb.fxgl.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.ResourceBundle;

public final class VolumeBoxController implements Initializable {

    private static final Logger log = Logger.get(VolumeBoxController.class);

    @FXML
    private Slider volumeSlider;

    @FXML
    private Region frontVolumeTrack;

    @FXML
    private Label volumeLabel;

    private BooleanProperty muted = new SimpleBooleanProperty(false);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        frontVolumeTrack.prefWidthProperty().bind(
                volumeSlider.widthProperty().subtract(30).multiply(volumeSlider.valueProperty().divide(volumeSlider.maxProperty()))
        );

        volumeLabel.textProperty().bind(volumeSlider.valueProperty().asString("%.0f"));
    }

    public DoubleProperty volumeProperty() {
        return volumeSlider.valueProperty();
    }

    public boolean isMuted() {
        return muted.get();
    }

    public BooleanProperty mutedProperty() {
        return muted;
    }

    @FXML
    private void onClickMute() {
        muted.set(true);
    }

    @FXML
    private void onClickUnmute() {
        muted.set(false);
    }
}
