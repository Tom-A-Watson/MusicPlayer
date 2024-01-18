/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.MusifyApp;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import com.almasb.fxgl.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static app.musicplayer.Config.SUPPORTED_FILE_EXTENSIONS;

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

	private Playlist playlist = null;

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

		// TODO: needed?
//		tableView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
//			tableView.requestFocus();
//		});

		initRowFactory();

		tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (oldSelection != null) {
				oldSelection.setSelected(false);
			}

			if (newSelection != null && tableView.getSelectionModel().getSelectedIndices().size() == 1) {
				newSelection.setSelected(true);
				// TODO:
				//selectedSong = newSelection;
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

	public BooleanProperty allSongsPlaylistProperty() {
		return isAllSongsPlaylist;
	}

	public boolean isAllSongsPlaylist() {
		return isAllSongsPlaylist.get();
	}

	public void play() {
		// TODO: extract MusifyApp class
//		Song song = selectedSong;
//		ObservableList<Song> songList = tableView.getItems();
//		if (MusifyApp.isShuffleActive()) {
//			Collections.shuffle(songList);
//			songList.remove(song);
//			songList.add(0, song);
//		}
//		MusifyApp.setNowPlayingList(songList);
//		MusifyApp.setNowPlaying(song);
//		MusifyApp.play();
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






	public void setPlaylist(Playlist playlist) {
		isAllSongsPlaylist.set(playlist.getType() == Playlist.PlaylistType.ALL_SONGS);

		// TODO: placeholder text for all other playlists

		System.out.println(isAllSongsPlaylist);
	}




	@Override
	public void onClickPlaySong() {
		play();
	}

	@Override
	public void onClickAddToPlaylist() {

	}

	@FXML
	private void onClickImport() {
		var dirChooser = new DirectoryChooser();
		File selectedDir = dirChooser.showDialog(tableView.getScene().getWindow());

		if (selectedDir == null) {
			log.info("User did not select any directory");
			return;
		}

		var task = new LoadSongsTask(selectedDir.toPath());
        task.setOnSucceeded(e -> {
            var lib = MusifyApp.getLibrary();
            lib.addSongsNoDuplicateCheck(task.getValue());
        });

        MusifyApp.getExecutorService().submit(task);
	}

	private static class LoadSongsTask extends Task<List<Song>> {

		private final Path directory;

		private LoadSongsTask(Path directory) {
			this.directory = directory;
		}

		@Override
		protected List<Song> call() throws Exception {
			List<Song> songs = new ArrayList<>();

			try (Stream<Path> filesStream = Files.walk(directory)) {
				var files = filesStream
						.filter(file -> Files.isRegularFile(file) && isSupportedFileType(file))
						.toList();

				int id = 0;
				int numFiles = files.size();

				for (var file : files) {
					updateMessage("Loading: " + file.getFileName());

					var song = loadSongData(id++, file);
					songs.add(song);

					updateProgress(id, numFiles);
				}

			} catch (Exception e) {
				log.warning("Failed to load song data", e);
			}

			return songs;
		}

		private Song loadSongData(int id, Path file) throws Exception {
			AudioFile audioFile = AudioFileIO.read(file.toFile());

			int lengthSeconds = 0;

			if (audioFile != null && audioFile.getAudioHeader() != null)
				lengthSeconds = audioFile.getAudioHeader().getTrackLength();

			String fileName = file.getFileName().toString();
			String title = fileName.substring(0, fileName.lastIndexOf('.'));

			return new Song(
					id,
					title,
					lengthSeconds,
					0,
					LocalDateTime.now(),
					file
			);
		}

        private static boolean isSupportedFileType(Path file) {
            var fileName = file.toString();

            return SUPPORTED_FILE_EXTENSIONS.stream()
                    .anyMatch(fileName::endsWith);
        }
	}

	private static Image loadArtwork(Path songFile) {
		try {
			AudioFile audioFile = AudioFileIO.read(songFile.toFile());
			Tag tag = audioFile.getTag();

			if (tag.getFirstArtwork() != null) {
				byte[] bytes = tag.getFirstArtwork().getBinaryData();
				ByteArrayInputStream in = new ByteArrayInputStream(bytes);
				Image artwork = new Image(in, 300, 300, true, true);

				if (!artwork.isError())
					return artwork;
			}
		} catch (Exception e) {
			log.warning("Failed to load artwork for: " + songFile, e);
		}

		return new Image(Config.IMG + "albumsIcon.png");
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
