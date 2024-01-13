/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.view;

import app.musicplayer.MusifyApp;
import app.musicplayer.model.Song;
import app.musicplayer.controllers.PlaylistsController;
import app.musicplayer.Config;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class ControlPanelTableCell<S, T> extends TableCell<S, T> {

    private ChangeListener<Boolean> listener = (observable, oldValue, newValue) ->
            ControlPanelTableCell.this.updateItem(ControlPanelTableCell.this.getItem(), ControlPanelTableCell.this.isEmpty());

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        Song song = (Song) this.getTableRow().getItem();

        if (empty || item == null || song == null) {
            setText(null);
            setGraphic(null);
        } else if (!song.isSelected()) {
            setText(item.toString());
            setGraphic(null);
            song.selectedProperty().removeListener(listener);
            song.selectedProperty().addListener(listener);
        } else {


            String fileName;
            // Selects the correct control panel based on whether the user is in a play list or not.
            if (MusifyApp.getMainController().getSubViewController() instanceof PlaylistsController) {
                fileName = Config.FXML + "ControlPanelPlaylists.fxml";
            } else {
                fileName = Config.FXML + "ControlPanel.fxml";
            }
            try {
                Label text = new Label(item.toString());
                text.setTextOverrun(OverrunStyle.CLIP);
                FXMLLoader loader = new FXMLLoader(this.getClass().getResource(fileName));
                HBox controlPanel = loader.load();
                BorderPane cell = new BorderPane();
                //cell.setLeft(controlPanel);
                //cell.setCenter(text);
//                BorderPane.setAlignment(text, Pos.CENTER_LEFT);
//                BorderPane.setAlignment(controlPanel, Pos.CENTER_LEFT);

                setText(null);
                setGraphic(controlPanel);

                song.selectedProperty().removeListener(listener);
                song.selectedProperty().addListener(listener);
            } catch (Exception ex) {
                ex.printStackTrace();
            }



        }
    }
}