package launcher;

import controller.MapApp2DController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class main2D  extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
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

}
