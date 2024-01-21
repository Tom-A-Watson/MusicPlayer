/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.events.UserDataEvent;
import app.musicplayer.events.UserEvent;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import com.almasb.fxgl.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static app.musicplayer.Config.VAR_DRAGGED_SONGS;
import static app.musicplayer.events.UserDataEvent.PLAY_SONG;
import static com.almasb.fxgl.dsl.FXGL.fire;
import static com.almasb.fxgl.dsl.FXGL.set;

public final class SongTableViewController implements Initializable, ControlBoxController.ControlBoxHandler {

	private static final Logger log = Logger.get(SongTableViewController.class);

	@FXML
	private TableView<Song> tableView;
	@FXML
	private TableColumn<Song, String> controlColumn;
	@FXML
	private TableColumn<Song, String> titleColumn;
	@FXML
	private TableColumn<Song, String> lengthColumn;

	private BooleanProperty isAllSongsPlaylist = new SimpleBooleanProperty(true);

	// playlist is never null after MainController is initialized
	private Playlist playlist;

	// this can be null (e.g. if the playlist has no songs)
	private Song selectedSong = null;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		log.info("initialize()");

		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// 70 (controlColumn)
		titleColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(70).multiply(0.75));
		lengthColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(70).multiply(0.25));

		controlColumn.setCellFactory(x -> new ControlPanelTableCell<>());
		lengthColumn.setCellFactory(x -> new ClippedTableCell<>());

		controlColumn.setCellValueFactory(param -> new SimpleStringProperty(""));
		titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
		lengthColumn.setCellValueFactory(new PropertyValueFactory<>("displayLength"));

		initRowFactory();

		// this filter allows each cell to be selected
		// TODO: cannot be present by default or button import songs cannot be pressed
		tableView.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			tableView.requestFocus();
			e.consume();
		});

		tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (oldSelection != null) {
				oldSelection.setSelected(false);
			}

			if (newSelection != null && tableView.getSelectionModel().getSelectedIndices().size() == 1) {
				newSelection.setSelected(true);
				selectedSong = newSelection;
			}
		});
	}

	private void initRowFactory() {
		tableView.setRowFactory(view -> {
			TableRow<Song> row = new TableRow<>();

			PseudoClass playing = PseudoClass.getPseudoClass("playing");

			ChangeListener<Boolean> changeListener = (obs, oldValue, newValue) -> row.pseudoClassStateChanged(playing, newValue);

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
				var sm = tableView.getSelectionModel();

				if (!sm.isSelected(row.getIndex())) {
					sm.clearSelection();
					sm.select(row.getIndex());
				}

				Dragboard db = row.startDragAndDrop(TransferMode.ANY);

				List<Song> draggedSongs = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());

				set(VAR_DRAGGED_SONGS, draggedSongs);

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

	public BooleanProperty allSongsPlaylistProperty() {
		return isAllSongsPlaylist;
	}

	public boolean isAllSongsPlaylist() {
		return isAllSongsPlaylist.get();
	}

	private void play() {
		if (selectedSong == null)
			return;

		fire(new UserDataEvent<>(PLAY_SONG, selectedSong));
	}

	public void selectSong(Song selectedSong) {
		tableView.getSelectionModel().clearSelection();
		tableView.getSelectionModel().select(selectedSong);
		tableView.scrollTo(selectedSong);
	}

	public void setSongs(ObservableList<Song> songs) {
		tableView.getSelectionModel().clearSelection();
		tableView.setItems(songs);

		if (!songs.isEmpty()) {
			selectSong(songs.get(0));
		}
	}

	public void setPlaylist(Playlist playlist) {
		this.playlist = playlist;

		isAllSongsPlaylist.set(playlist.getType() == Playlist.PlaylistType.ALL_SONGS);

		setSongs(playlist.getSongs());
	}

	public Playlist getPlaylist() {
		return playlist;
	}

	@FXML
	private void onClickImport() {
		fire(new UserEvent(UserEvent.CLICK_IMPORT));
	}

	@Override
	public void onClickPlaySong() {
		play();
	}

	@Override
	public void onClickAddToPlaylist() {
		// TODO:
	}

	private class ControlPanelTableCell<S, T> extends TableCell<S, T> {

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

	private class ClippedTableCell<S, T> extends TableCell<S, T> {

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
