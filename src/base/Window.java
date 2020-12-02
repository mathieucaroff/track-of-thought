package base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import base.obj.Ball;
import base.obj.FullTrack;
import base.obj.Station;
import base.obj.Track;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Window extends Application {
	
	public static Stage window;		// main stage
	public static int points = 0;	// points counter
	public static boolean levelCreator = false;	// temporary variable for level creation

	private static AnimationTimer gameTimer;	// main game timer
	
	private static int level;
	private static int seconds = 0;				// seconds counter for ball releasing
	private static int finishedBalls = 0;
	private static boolean skip = false;

	@Override
	public void start(Stage primaryStage) {
		window = primaryStage;
		window.setTitle("Track of thought");
		window.setResizable(false);
		
		Setup.runSetup();
		window.show();
	}
		
	public static void game(FullTrack allNodes) {
		final Pane root = Scenes.getRootPane();							// game root pane
		final Scene scene = Scenes.getSceneWithCSS(root, "game.css");	// main game scene
		
		final List<Ball> balls = allNodes.getBalls();				// list of all balls
		final List<Track> tracks = allNodes.getTracks();			// list of all tracks
		final List<Station> stations = allNodes.getStations();		// list of all stations
				
//		stations.forEach(station -> station.setColor(Color.TRANSPARENT));
		
		/* StackPane for points counter */
		final StackPane pointsStack = new StackPane();
			pointsStack.setTranslateX(0);
			pointsStack.setTranslateY(7);
			pointsStack.setPrefSize(850, 30);

		/* text with points value */
		final Text pointsText = new Text("0/0");
			pointsText.setFill(Utils.COLOR_ACCENT);
			pointsText.setFont(Font.font("Hind Guntur Bold", 23));

		/* add everything to the root pane */
		pointsStack.getChildren().add(pointsText);
		root.getChildren().addAll(tracks);
		root.getChildren().add(allNodes.getStartStation().getFix());
		root.getChildren().addAll(balls);
		root.getChildren().addAll(stations);
		root.getChildren().add(pointsStack);
		
//		for(int i=0; i<15; i++) { root.getChildren().addAll(Scenes.GRID[i]);}	// grid drawing

		level = stations.size()-1;
		setScene(scene);	// set scene with all elements
									
		gameTimer = new AnimationTimer() {
			private long lastUpdate = 0;
			private long secondsUpdate = 0;
						
			@Override
			public void handle(long now) {
				try {
					/* game timer */
					if(now - lastUpdate >= 17_000_000) {
						gameHandle(root, balls, pointsText, allNodes);
						lastUpdate = now;
					}
					
				} catch (Exception e) {
					Log.error(e.toString());
				}
				/* timer for counting seconds & managing new balls */
				if(now - secondsUpdate >= 1_000_000_000) {
					seconds++;
					secondsHandle(root, balls, allNodes);
					secondsUpdate = now;
				}
			}
		}; gameTimer.start();
				
	}
	
	/* main game */
	private static void gameHandle(Pane root, List<Ball> balls, Text pointsText, FullTrack allNodes) {
		List<Ball> toRemove = new ArrayList<Ball>();	// list of balls that should be removed
		for(Ball ball : balls) {
			/* update ball only if should be, or already is 'released' */
			if(seconds >= ball.getDelay()) {
				ball.update(allNodes);
				
				/* if ball finished 'parking' */
				if(ball.getCounter() == 25) {
					finishedBalls++;
					allNodes.removeActiveBall(ball);
					
					Station finalStation = allNodes.findStation(ball.getColumn(), ball.getRow());	// get final station				
					/* if the station is correct update points value */
					if(finalStation.getColor() == ball.getColor() && (finalStation.hasBorder() == ball.hasBorder())) {
						points++;
					} else {
						skip = true;
					}
					pointsText.setText(String.valueOf(points) + "/" + String.valueOf(finishedBalls));
					root.getChildren().remove(ball);	// remove the ball from root pane when 'parked'
					toRemove.add(ball);					// add to removal list
				}
			}
		}
		balls.removeAll(toRemove);	// remove balls that finished track
	}
	
	/* updates seconds and sets colors of the new balls */
	private static void secondsHandle(Pane root, List<Ball> balls, FullTrack allNodes) {
		for(Ball ball : balls) {
			if(seconds == ball.getDelay()) {
				
				/* allow skipping only if there is enough active balls */
				if(skip) {
					skip = false;
					if(allNodes.getActiveBalls().size() > level) {
						balls.remove(ball);
						root.getChildren().remove(ball);
						break;
					}
				}
				
				String newColor = Scenes.getNextBallColor(allNodes);
				ball.setColor(newColor);
				
				allNodes.addActiveBall(ball);
				break;
			}
		}
	}
			
	/* sets the main scene */
	public static void setScene(Scene scene) {
		window.setScene(scene);
	}
	
	public static void main (String[] args) throws FileNotFoundException {
		
		/* parses arguments */
		if(args.length>0) {
			/* 
			 	currently available arguments:
			 		- log: enables logging to file for debugging 
			 		- create: allows to create new levels
			*/
			for(String arg : args) {
				switch(arg) {
					case "--create":
						levelCreator = true;
						Log.success("Creator mode enabled");
						break;
					case "--log":
						Log.success("Logging enabled");
						PrintStream outputLog = new PrintStream(new FileOutputStream(new File("log.txt")));
							System.setOut(outputLog);
							System.setErr(outputLog);
					break;
					default:
						Log.warning("Unknown argument: " + arg);
				}
			}
		}
		try {
			launch(args);
		} catch(Exception e) {
			Log.error(e.toString());
		}
	}
	

}
