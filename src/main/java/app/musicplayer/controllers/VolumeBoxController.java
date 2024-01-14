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
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
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

    @FXML
    private Pane muteButton;

    @FXML
    private Pane mutedButton;

    private BooleanProperty muted = new SimpleBooleanProperty(false);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize()");

        frontVolumeTrack.prefWidthProperty().bind(
                volumeSlider.widthProperty().subtract(30).multiply(volumeSlider.valueProperty().divide(volumeSlider.maxProperty()))
        );

        volumeLabel.textProperty().bind(volumeSlider.valueProperty().asString("%.0f"));

        volumeSlider.setOnMousePressed(e -> {
            if (mutedButton.isVisible()) {
                onClickMute();
            }
        });
    }

    DoubleProperty volumeProperty() {
        return volumeSlider.valueProperty();
    }

    BooleanProperty mutedProperty() {
        return muted;
    }

    @FXML
    private void onClickMute() {
        // TODO: check pseudo class impl
        PseudoClass mutedClass = PseudoClass.getPseudoClass("muted");
        boolean isMuted = mutedButton.isVisible();
        muteButton.setVisible(isMuted);
        mutedButton.setVisible(!isMuted);
        volumeSlider.pseudoClassStateChanged(mutedClass, !isMuted);
        frontVolumeTrack.pseudoClassStateChanged(mutedClass, !isMuted);
        volumeLabel.pseudoClassStateChanged(mutedClass, !isMuted);

        muted.set(isMuted);
    }
}
