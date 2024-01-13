/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public final class SplashScreenController {

    @FXML
    private Label progressLabel;

    @FXML
    private ProgressBar progressBar;

    public Label getProgressLabel() {
        return progressLabel;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
}
