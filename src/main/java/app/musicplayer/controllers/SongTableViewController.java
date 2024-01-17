/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.MusifyApp;
import app.musicplayer.model.Song;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.*;

public final class SongTableViewController implements Initializable, ControlBoxController.ControlBoxHandler {

	@FXML
	private TableView<Song> tableView;

	@FXML
	private TableColumn<Song, String> controlColumn;
	@FXML
	private TableColumn<Song, String> titleColumn;
	@FXML
	private TableColumn<Song, String> lengthColumn;

	// Initializes table view scroll bar.
	private ScrollBar scrollBar;

	private Song selectedSong;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// 70 (controlColumn)
		titleColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(70).multiply(0.75));
		lengthColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(70).multiply(0.25));

		controlColumn.setCellFactory(x -> new ControlPanelTableCell<>());
		lengthColumn.setCellFactory(x -> new ClippedTableCell<>());

		controlColumn.setCellValueFactory(param -> new SimpleStringProperty(""));
		titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
		lengthColumn.setCellValueFactory(new PropertyValueFactory<>("displayLength"));

		tableView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			tableView.requestFocus();
			event.consume();
		});

		initRowFactory();

		tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (oldSelection != null) {
				oldSelection.setSelected(false);
			}

			if (newSelection != null && tableView.getSelectionModel().getSelectedIndices().size() == 1) {
				newSelection.setSelected(true);
				selectedSong = newSelection;
			}
		});

		// Plays selected song when enter key is pressed.
		tableView.setOnKeyPressed(event -> {
			if (event.getCode().equals(KeyCode.ENTER)) {
				play();
			}
		});
	}

	private void initRowFactory() {
		tableView.setRowFactory(view -> {
			TableRow<Song> row = new TableRow<>();

			PseudoClass playing = PseudoClass.getPseudoClass("playing");

			ChangeListener<Boolean> changeListener = (obs, oldValue, newValue) ->
					row.pseudoClassStateChanged(playing, newValue);

			row.itemProperty().addListener((obs, previousSong, currentSong) -> {
				if (previousSong != null) {
					previousSong.playingProperty().removeListener(changeListener);
				}
				if (currentSong != null) {
					currentSong.playingProperty().addListener(changeListener);
					row.pseudoClassStateChanged(playing, currentSong.isPlaying());
				} else {
					row.pseudoClassStateChanged(playing, false);
				}
			});

			row.setOnMouseClicked(event -> {
				TableViewSelectionModel<Song> sm = tableView.getSelectionModel();
				if (event.getClickCount() == 2 && !row.isEmpty()) {
					play();
				} else if (event.isShiftDown()) {
					ArrayList<Integer> indices = new ArrayList<>(sm.getSelectedIndices());
					if (indices.isEmpty()) {
						if (indices.contains(row.getIndex())) {
							sm.clearSelection(row.getIndex());
						} else {
							sm.select(row.getItem());
						}
					} else {
						sm.clearSelection();
						indices.sort((first, second) -> first.compareTo(second));
						int max = indices.get(indices.size() - 1);
						int min = indices.get(0);
						if (min < row.getIndex()) {
							for (int i = min; i <= row.getIndex(); i++) {
								sm.select(i);
							}
						} else {
							for (int i = row.getIndex(); i <= max; i++) {
								sm.select(i);
							}
						}
					}

				} else if (event.isControlDown()) {
					if (sm.getSelectedIndices().contains(row.getIndex())) {
						sm.clearSelection(row.getIndex());
					} else {
						sm.select(row.getItem());
					}
				} else {
					if (sm.getSelectedIndices().size() > 1) {
						sm.clearSelection();
						sm.select(row.getItem());
					} else if (sm.getSelectedIndices().contains(row.getIndex())) {
						sm.clearSelection();
					} else {
						sm.clearSelection();
						sm.select(row.getItem());
					}
				}
			});

			row.setOnDragDetected(event -> {
				Dragboard db = row.startDragAndDrop(TransferMode.ANY);

				List<Song> draggedSongs = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
				MusifyApp.setDraggedItems(draggedSongs);

				db.setContent(Map.of(Config.DRAG_SONG_LIST, ""));

				ImageView image = new ImageView(row.snapshot(null, null));
				Rectangle2D rectangle = new Rectangle2D(0, 0, 250, 50);
				image.setViewport(rectangle);
				db.setDragView(image.snapshot(null, null), 125, 25);
				event.consume();
			});

			return row;
		});
	}

	public void play() {
		// TODO: extract MusifyApp class
		Song song = selectedSong;
		ObservableList<Song> songList = tableView.getItems();
		if (MusifyApp.isShuffleActive()) {
			Collections.shuffle(songList);
			songList.remove(song);
			songList.add(0, song);
		}
		MusifyApp.setNowPlayingList(songList);
		MusifyApp.setNowPlaying(song);
		MusifyApp.play();
	}

	public void selectSong(Song selectedSong) {
		tableView.getSelectionModel().select(selectedSong);
	}

	public void setSongs(ObservableList<Song> songs) {
		tableView.getSelectionModel().clearSelection();

		tableView.setItems(songs);

		if (!songs.isEmpty()) {
			selectSong(songs.get(0));
		}
	}

    @Override
    public void onClickPlaySong() {
        play();
    }

    @Override
    public void onClickAddToPlaylist() {

    }

    public class ControlPanelTableCell<S, T> extends TableCell<S, T> {

		private ChangeListener<Boolean> listener = (obs, oldValue, newValue) -> updateItem(getItem(), isEmpty());

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

				String fileName = Config.FXML + "controls/ControlBox.fxml";

				try {
					FXMLLoader loader = new FXMLLoader(this.getClass().getResource(fileName));
					HBox controlPanel = loader.load();
                    ControlBoxController controller = loader.getController();
                    controller.setHandler(SongTableViewController.this);

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

	public class ClippedTableCell<S, T> extends TableCell<S, T> {

		public ClippedTableCell() {
			setTextOverrun(OverrunStyle.CLIP);
		}

		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);

			if (empty || item == null) {
				setText(null);
				setGraphic(null);
			} else {
				setText(item.toString());
			}
		}
	}
}
