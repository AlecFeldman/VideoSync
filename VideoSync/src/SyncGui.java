import java.io.File;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.stage.Stage;

//Start of Program 
public class SyncGui extends Application 
{
    // Start of GUI Class
	@Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setTitle("Theater");

        // Creating the browse button
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(new EventHandler<ActionEvent>() 
        {
            @Override public void handle(ActionEvent event) 
            {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Resource File");
                fileChooser.getExtensionFilters().addAll(
                        new ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
                        new ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv", "*.mts"),
                        //new ExtensionFilter("Text Files", "*.txt", "*.docx", "*.xmlx", "*.doc", "*.xml", "*.pdf"),
                        //new ExtensionFilter("Image Files", "*.jpg", "*.png", "*.gif", "*.jpeg"),
                        new ExtensionFilter("All Files", "*.*"));
                File selectedFile;
                selectedFile = fileChooser.showOpenDialog(primaryStage);
                if (selectedFile != null) 
                {
                	/*
                	Media source = new Media(selectedFile.toURI().toString());
                	MediaPlayer mp = new MediaPlayer(source);
                	MediaView mv = new MediaView(mp);
                    mp.play();
                    */
                	//Media source = new Media("file://" + selectedFile.getPath());
                	//return;
                }
            }
        });

		// Constraint variables
        RowConstraints rowConst1, rowConst2, rowConst3;
        ColumnConstraints colConst1, colConst2;

        // Video/Media Window
        Pane videoPane = new Pane();
        videoPane.setStyle("-fx-background-color: #000000;");

        // Right Hand Side (Chat)
        GridPane gridPaneChat = new GridPane();
        gridPaneChat.setStyle("-fx-background-color: #ffa500;");
        TextArea textAreaChat = new TextArea();
        textAreaChat.setMaxWidth(200);
        //textAreaChat.setPercentWidth(100);
        textAreaChat.setMinHeight(375);
        //textAreaChat.setPercentHeight(90);
        TextField textFieldChat = new TextField();
        textFieldChat.setMinWidth(200);
        //textFieldChat.setPercentWidth(100);
        gridPaneChat.add(textAreaChat, 0, 0);
        gridPaneChat.add(textFieldChat, 0, 1);
        
        // Creating a Video Player
        /*
        MediaPlayer mp = new MediaPlayer(source);
        MediaView mv = new MediaView(mp);
        DoubleProperty width = mv.fitWidthProperty();
        DoubleProperty height = mv.fitHeightProperty();
        width.bind(Bindings.selectDouble(mv.sceneProperty(), "width"));
        height.bind(Bindings.selectDouble(mv.sceneProperty(), "height"));
        
        volumeSlider.setValue(mp.getVolume()*100);
        volumeSlider.valueProperty().addListener(new InvalidationListener() 
        {@Override public void invalidated(Observable observable) 
        	{
        		mp.setVolume(volumeSlider.getValue() / 100);
        	}
        });
        mv.setPreserveRatio(true);
        */
        
        // Video Buttons Area
        FlowPane flowPaneButtonBar = new FlowPane();
        
        // Play Button
        Button playButton = new Button("Play");
        /*
        playButton.setOnAction(new EventHandler<ActionEvent>() 
        {@Override public void handle(ActionEvent event) 
        {
        	mp.play();
        	mp.setRate(1);
        }});
        */
        
        // Pause Button 
        Button pauseButton = new Button("Pause");
        /*
        pauseButton.setOnAction(new EventHandler<ActionEvent>() 
        {@Override public void handle(ActionEvent event) 
        {
        	mp.pause();
        }});
        */
        
        // Stop Button
        Button stopButton = new Button("Stop");
        /*
        stopButton.setOnAction(new EventHandler<ActionEvent>() 
        {@Override public void handle(ActionEvent event) 
        {
        	mp.seek(mp.getStartTime());
            mp.stop();
        }});
        */
        
        //Add the Buttons
        flowPaneButtonBar.getChildren().add(playButton);
        flowPaneButtonBar.getChildren().add(pauseButton);
        flowPaneButtonBar.getChildren().add(stopButton);

        // Left Hand Side (Media Player)
        GridPane gridPaneMediaPlayer = new GridPane();
        gridPaneMediaPlayer.setStyle("-fx-background-color: #808080;");
        colConst1 = new ColumnConstraints();
        colConst1.setPercentWidth(100); // 100%
        rowConst1 = new RowConstraints();
        //rowConst1.setMinHeight(50);
        rowConst1.setPercentHeight(15);
        rowConst2 = new RowConstraints();
        //rowConst2.setMinHeight(300);
        rowConst2.setPercentHeight(70);
        rowConst3 = new RowConstraints();
        //rowConst3.setMinHeight(50);
        rowConst3.setPercentHeight(15);
        gridPaneMediaPlayer.getRowConstraints().addAll(rowConst1, rowConst2, rowConst3);
        gridPaneMediaPlayer.getColumnConstraints().add(colConst1);
        
        //gridPaneMediaPlayer.getChildren().add(mv);
        
        gridPaneMediaPlayer.add(browseButton, 0, 0);
        gridPaneMediaPlayer.add(videoPane, 0, 1);
        gridPaneMediaPlayer.add(flowPaneButtonBar, 0, 2);

        //Layout
        GridPane gridPaneRoot = new GridPane();
        colConst1 = new ColumnConstraints();
        //colConst1.setMinWidth(400);
        colConst1.setPercentWidth(66.5);
        colConst2 = new ColumnConstraints();
        //colConst2.setMinWidth(200);
        colConst2.setPercentWidth(33.5);
        gridPaneRoot.getColumnConstraints().addAll(colConst1, colConst2);
        gridPaneRoot.add(gridPaneMediaPlayer, 0, 0);
        gridPaneRoot.add(gridPaneChat, 1, 0);

        // Create Group
        Group group = new Group(gridPaneRoot);
        colConst1 = new ColumnConstraints();
        colConst1.setPercentWidth(100); // 100%

        //Creating a Scene by passing the group object, height and width
        Scene scene = new Scene(group ,600, 400);
        primaryStage.setScene(scene);
        
        /*
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        */

        //this line of code sets a window to open at maximized size.
        //primaryStage.setMaximized(true);

        //this line of code sets a window to open at full screen.
        //primaryStage.setFullScreen(true);

        //The show() method is used to display the contents of the Stage
        primaryStage.show();
        
    }//End of GUI Class
    
    //Start of Main Method
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        launch(args);
    }//End of Main Method 
}// End of Program