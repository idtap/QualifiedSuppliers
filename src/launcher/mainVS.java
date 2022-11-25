package launcher;

import controller.ViewShedController;
import controller.mainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class mainVS extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
//		try {
//			mainController controller = new mainController();
//			controller._stage_main = primaryStage;
//			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
//			loader.setController(controller);
//			Parent root = (Parent) loader.load();
//			Scene sceneGIS2 = new Scene(root, 1024, 768);
//			
//			Stage stage = new Stage();
//			stage.setTitle("3D圖台顯示");
//			stage.setScene(sceneGIS2);
//			stage.show();
//			stage.setOnCloseRequest(e -> {
//				try {
//					stop();
//				} catch (Exception e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//				Platform.exit();
//				System.exit(0);
//			});
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ViewShed.fxml"));
			Parent root = loader.load();
//		    controller = loader.getController();
			Scene scene = new Scene(root);

//			ViewShedController controller = new ViewShedController();
////			controller._stage_main = primaryStage;
//			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ViewShed.fxml"));
//			loader.setController(controller);
//			Parent root = (Parent) loader.load();
//			Scene scene = new Scene(root, 1024, 768);
			
			
			// set up the stage
			Stage stage = new Stage();
			stage.setTitle("Animate 3d Graphic Sample");
			stage.setWidth(900);
			stage.setHeight(800);
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

}
