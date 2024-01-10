/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.model.Library;
import com.almasb.fxgl.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public final class ImportMusicController {

    private static final Logger log = Logger.get(ImportMusicController.class);

    @FXML
    private Label label;

    @FXML
    private Button importMusicButton;

    @FXML
    private ProgressBar progressBar;

    private Stage ownerStage = null;
    private Runnable onFinished = () -> {};

    public void setOwnerStage(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    @FXML
    private void onClickImport() {
        var dirChooser = new DirectoryChooser();
        File selectedDir = dirChooser.showDialog(ownerStage);

        if (selectedDir == null) {
            log.info("User did not select any directory");
            return;
        }

        label.setText("Importing music library...");
        importMusicButton.setVisible(false);
        progressBar.setVisible(true);

        var task = Library.newImportMusicTask(selectedDir.getPath());
        task.setOnSucceeded(e -> {
            onFinished.run();
        });
        task.setOnFailed(e -> {
            log.warning("ImportMusicTask failed");
        });

        progressBar.progressProperty().bind(task.progressProperty());

        new Thread(task).start();
    }
}
