package app.musicplayer.controllers;

import app.musicplayer.Config;
import app.musicplayer.MusifyApp;
import app.musicplayer.model.Song;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
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
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class SongTableViewController implements Initializable {

	@FXML
	private TableView<Song> tableView;

    @FXML
	private TableColumn<Song, Boolean> playingColumn;
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

		// 50 (playingColumn) + 70 (controlColumn)
    	titleColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(120).multiply(0.75));
        lengthColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(120).multiply(0.25));

        playingColumn.setCellFactory(x -> new PlayingTableCell<>());
		controlColumn.setCellFactory(x -> new ControlPanelTableCell<>());
        //titleColumn.setCellFactory(x -> new ControlPanelTableCell<>());
        lengthColumn.setCellFactory(x -> new ClippedTableCell<>());

        playingColumn.setCellValueFactory(new PropertyValueFactory<>("playing"));
		controlColumn.setCellValueFactory(param -> new SimpleStringProperty(""));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        lengthColumn.setCellValueFactory(new PropertyValueFactory<>("displayLength"));
        
        lengthColumn.setSortable(false);
        
        tableView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        	tableView.requestFocus();
        	event.consume();
        });
        
        // Retrieves the list of songs in the library, sorts them, and adds them to the table.
        List<Song> songs = MusifyApp.getLibrary().getSongs();

        Collections.sort(songs);
        
        tableView.setItems(FXCollections.observableArrayList(songs));

        tableView.setRowFactory(x -> {
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
                	if (indices.size() < 1) {
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
            	ClipboardContent content = new ClipboardContent();
            	if (tableView.getSelectionModel().getSelectedIndices().size() > 1) {
            		content.putString("List");
                    db.setContent(content);
                	MusifyApp.setDraggedItem(tableView.getSelectionModel().getSelectedItems());
            	} else {
            		content.putString("Song");
                    db.setContent(content);
                	MusifyApp.setDraggedItem(row.getItem());
            	}
            	ImageView image = new ImageView(row.snapshot(null, null));
            	Rectangle2D rectangle = new Rectangle2D(0, 0, 250, 50);
            	image.setViewport(rectangle);
            	db.setDragView(image.snapshot(null, null), 125, 25);
                event.consume();
            });

            return row;
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
        
        // Plays selected song when enter key is pressed.
        tableView.setOnKeyPressed(event -> {
        	if (event.getCode().equals(KeyCode.ENTER)) {
        		play();
        	}
        });
    }

    public void play() {
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

//    public void scroll(char letter) {
//    	if (!tableView.getSortOrder().isEmpty()) {
//    		currentSortColumn = tableView.getSortOrder().get(0).getId();
//    		currentSortOrder = tableView.getSortOrder().get(0).getSortType().toString().toLowerCase();
//    	}
//
//    	// Retrieves songs from table.
//    	ObservableList<Song> songTableItems = tableView.getItems();
//    	// Initializes counter for cells. Used to determine what cell to scroll to.
//    	int selectedCell = 0;
//    	int selectedLetterCount = 0;
//
//    	// Retrieves the table view scroll bar.
//    	if (scrollBar == null) {
//    		scrollBar = (ScrollBar) tableView.lookup(".scroll-bar");
//    	}
//
//    	double startVvalue = scrollBar.getValue();
//    	double finalVvalue;
//
//    	if ("descending".equals(currentSortOrder)) {
//    		finalVvalue = 1 - (((selectedCell + selectedLetterCount) * 50 - scrollBar.getHeight()) /
//    				(songTableItems.size() * 50 - scrollBar.getHeight()));
//    	} else {
//    		finalVvalue = (double) (selectedCell * 50) / (songTableItems.size() * 50 - scrollBar.getHeight());
//    	}
//
//    	Animation scrollAnimation = new Transition() {
//            {
//                setCycleDuration(Duration.millis(500));
//            }
//            protected void interpolate(double frac) {
//                double vValue = startVvalue + ((finalVvalue - startVvalue) * frac);
//                scrollBar.setValue(vValue);
//            }
//        };
//        scrollAnimation.play();
//    }
    
    public Song getSelectedSong() {
    	return selectedSong;
    }

    public void selectSong(Song selectedSong) {
        tableView.getSelectionModel().select(selectedSong);
    }

	public void setSongs(List<Song> songs) {
		tableView.setItems(FXCollections.observableArrayList(songs));

		if (!songs.isEmpty()) {
			selectSong(songs.get(0));
		}
	}

	public class PlayingTableCell<S, T> extends TableCell<S, T> {

		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null || !(Boolean) item) {
				setText(null);
				setGraphic(null);
			} else {
				// TODO: remove this column and instead make the row stand out in some other way
				var node = new Label("***");
				setGraphic(node);
			}
		}
	}


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

				String fileName = Config.FXML + "ControlPanel.fxml";

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
