package main;

import controller.MapApp2DController;
import controller.mainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Main extends Application {

	public void start(Stage primaryStage) {
		try {
			mainController controller = new mainController();
			controller._stage_main = primaryStage;
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
			loader.setController(controller);
			Parent root = (Parent) loader.load();
			Scene sceneGIS2 = new Scene(root, 1024, 768);
			
			Stage stage = new Stage();
			stage.setTitle("3D圖台顯示");
			stage.setScene(sceneGIS2);
			stage.show();
			stage.setOnCloseRequest(e -> {
				stop();
				Platform.exit();
				System.exit(0);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}


		MapApp2DController mapApp2DController;
		try {
			// create stack pane and application scene
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MapApp2D.fxml"));
			Parent parent = loader.load();
			mapApp2DController = loader.<MapApp2DController>getController();
			Scene scene = new Scene(parent, 1024, 768);

			// set title, size, and add scene to stage
			Stage stage = new Stage();
			stage.setTitle("2D圖台顯示");
			stage.setScene(scene);
			mapApp2DController._stage_main = primaryStage;
			stage.show();

			stage.setOnCloseRequest(e -> {
				mapApp2DController.terminate();
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void stop() {

	}

	public static void main(String[] args) {
		launch(args);
	}

}
