/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer;

import app.musicplayer.controllers.MainController;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.core.EngineService;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.logging.Logger;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.LogManager;

import static app.musicplayer.Config.*;

// TODO: serialization version for future updates
// TODO: volume is not serialized
// TODO: light and dark themes
// TODO: on startup "Your Library" is not focused
// TODO: rename playlist option
// TODO: remember stage width height
// TODO: remember last played song
// TODO: update to high res app icon
// TODO: update project title as Musify is taken
// TODO: consider free streaming music API online
// TODO: remove songs from library
// TODO: if song is removed from folder
// TODO: check shuffle and loop
// TODO: fix song table view selection when moving from a different playlist, that song is selected in the new one
// TODO: global controls e.g. spacebar to pause/resume
// TODO: allow playlist song reorder
// TODO: loop 1, loop all
public class FXGLMusicApp extends Application {

    private static final Logger log = Logger.get(FXGLMusicApp.class);

    public static class Launcher {
        public static void main(String[] args) {
            Application.launch(FXGLMusicApp.class);
        }
    }

    private static class HeadlessApp extends GameApplication {
        private final BooleanProperty fxglReady = new SimpleBooleanProperty(false);

        @Override
        protected void initSettings(GameSettings settings) {
            settings.addEngineService(FXGLReadyService.class);
        }
    }

    public static class FXGLReadyService extends EngineService {
        @Override
        public void onMainLoopStarting() {
            FXGL.<HeadlessApp>getAppCast().fxglReady.set(true);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        var fxglApp = new HeadlessApp();
        fxglApp.fxglReady.addListener((obs, o, isReady) -> {
            try {
                initJavaFXApp(stage);
            } catch (Exception e) {
                log.fatal("Failed to initialize " + TITLE, e);
                System.exit(0);
            }
        });

        GameApplication.embeddedLaunch(fxglApp);
    }

    private void initJavaFXApp(Stage stage) throws Exception {
        log.info("Initializing");

        // disable java.util.logging.Logger from jaudiotagger lib
        LogManager.getLogManager().reset();

        FXMLLoader loader = new FXMLLoader(FXGL.getAssetLoader().getURL("/assets/ui/scenes/MainScene.fxml"));
        Parent view = loader.load();
        MainController controller = loader.getController();

        stage.setMinWidth(850);
        stage.setMinHeight(600);
        stage.setTitle(TITLE + " " + VERSION);
        stage.getIcons().add(FXGL.image("Logo.png"));
        stage.setOnCloseRequest(event -> {
            controller.onExit();
            log.info("Closing stage");
            FXGL.getGameController().exit();
        });

        Scene scene = new Scene(view);
        scene.getStylesheets().add(FXGL.getAssetLoader().loadCSS("Global.css").getExternalForm());
        stage.setScene(scene);
        stage.show();

        log.info("Opening stage");
    }
}
