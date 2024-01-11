/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.view;

import app.musicplayer.util.Config;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Pane;

public class PlayingTableCell<S, T> extends TableCell<S, T> {

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || !(Boolean) item) {
            setText(null);
            setGraphic(null);
        } else {
            try {
                String fileName = Config.FXML + "PlayingIcon.fxml";
                FXMLLoader loader = new FXMLLoader(this.getClass().getResource(fileName));
                Pane pane = loader.load();
                setGraphic(pane);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}