/*
 * Copyright 2018 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geoanalysis.LocationLineOfSight;
import com.esri.arcgisruntime.geoanalysis.LocationViewshed;
import com.esri.arcgisruntime.geoanalysis.Viewshed;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointBuilder;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.PolygonBuilder;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.ArcGISVectorTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.localserver.LocalServer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.AnalysisOverlay;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties.SurfacePlacement;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.symbology.ColorUtil;
import com.esri.arcgisruntime.symbology.ModelSceneSymbol;
import com.esri.arcgisruntime.symbology.SceneSymbol;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSceneSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSceneSymbol.Style;

import controller.MapApp2DController.ApState;

import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import main.MapManager;
import tool.Slope;
import tool.StackProfile;
import tool.Volume;

public class mainController implements Initializable {

	public enum ApState {
		normal, load3DModel, 點, 線, 面, 球體, 椎體, 柱體, 立方體, 取高度, StackProfile, LineOfShight, ViewShed, Slope, Highest,
		Volume, 地表直線
	}

	public static ApState apState = ApState.normal;

	Map<String, String> symbolStyleName = new HashMap<>();

	@FXML
	private ComboBox<String> cmbDraw;
	@FXML
	private TextField txtAltitude;

	public static ObservableList<String> drawOptions = FXCollections.observableArrayList("點", "線", "面", "球體", "椎體",
			"柱體", "立方體");

	public Stage _stage_main;
	milSymbolPickerController milSymbolPicker;
	Stage stagePicker;
	GraphicsOverlay tmpGraphicsOverlay = null; // 暫時繪點之圖層
	GraphicsOverlay highGraphicsOverlay = null; // 取高度用圖層

	GraphicsOverlay tmpDropGraphicsOverlay = null; // 暫時繪點之圖層
	GraphicsOverlay dropGraphicsOverlay = null; // StackProfile用圖層

	GraphicJson gf = new GraphicJson("Overlay3D.json");
	GraphicJson gf2525B = new GraphicJson("Overlay2525B.json");

	private boolean blnDrawMilSymbol = false;
	private Graphic drawingGraphic = null;
	String URL_TaiwanDem_new_P = "";

	// create a new point collection for polyline
	PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());

	private PolylineBuilder lineBuilder = null;
	private PolygonBuilder areaBuilder = null;
	private String selectedSIC = "";
	private String selectedShape = "";

	private Graphic selectedGraphic = null;
	private String ModelFilePath = "";

	private StackProfile stackprofile = null;
	private Slope slope = null;
	private HIGHESTController highest = null;
	private Volume volume = null;

	private AnalysisOverlay analysisOverlay;
	private LocationViewshed viewshed;
	private AnalysisOverlay analysisOverlay2;
	private LocationLineOfSight lineOfSight;

	private Point viewPoint = null;
	@FXML
	private TextField txtName;

	@FXML
	private Button btnUpdateName;

	@FXML
	private Button btnCancel;

	@FXML
	private Button btnComploete;

	@FXML
	private Button btnDraw;

	@FXML
	private StackPane mapPane;

	@FXML
	private AnchorPane sketchPane;

	@FXML
	private ProgressBar progressBar;

	@FXML
	private ToggleButton visibilityToggle;
	@FXML
	private ToggleButton frustumToggle;
	@FXML
	private Slider headingSlider;
	@FXML
	private Slider pitchSlider;
	@FXML
	private Slider horizontalAngleSlider;
	@FXML
	private Slider verticalAngleSlider;
	@FXML
	private Slider minDistanceSlider;
	@FXML
	private Slider maxDistanceSlider;
	@FXML
	private GridPane gridPane1;

	@FXML
	void handlebtnUpdateName(ActionEvent event) {
		if (selectedGraphic.getSymbol().getClass().equals(TextSymbol.class))
			((TextSymbol) selectedGraphic.getSymbol()).setText(txtName.getText());
		else
			selectedGraphic.getAttributes().put("name", txtName.getText());

		// 儲存3D GraphicOverlay的Graphics到Json檔
		gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
	}

	@FXML
	void handleCancelDraw(ActionEvent event) {
		btnComploete.setDisable(true);
		btnCancel.setDisable(true);

		blnDrawMilSymbol = false;
		selectedSIC = "";
		MapManager.graphicsOverlayDraw3D.getGraphics().clear();
	}

	@FXML
	void handleCompleteDraw(ActionEvent event) {
		btnComploete.setDisable(true);
		btnCancel.setDisable(true);

		// 2525B
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("_type", "position_report");
		attributes.put("_action", "update");
		attributes.put("sidc", selectedSIC); // sidc 15碼
		attributes.put("datetimevalid", "291122111SEP22"); // 日時組
		attributes.put("speed", "350"); // 速度

		attributes.put("name", "自訂2525B符號");

		Geometry drawGeometry = null;
		switch (milSymbolPicker.getSelectedShape()) {
		case "POINT":
			drawGeometry = drawingGraphic.getGeometry();
			break;
		case "LINE":
			drawGeometry = lineBuilder.toGeometry();
			break;
		case "AREA":
			drawGeometry = areaBuilder.toGeometry();
			break;
		}

		Graphic millitaryGraphic = new Graphic(drawGeometry, attributes);
		MapManager.graphicsOverlay_military_3Dsymbol.getGraphics().add(millitaryGraphic);

		// 儲存2525B GraphicOverlay的Graphics到Json檔
		gf2525B.SaveGraphicsToJsonFile(MapManager.graphicsOverlay_military_3Dsymbol, null);

		blnDrawMilSymbol = false;
		selectedSIC = "";
		MapManager.graphicsOverlayDraw3D.getGraphics().clear();
	}

	@FXML
	void handleDrawMilSymbol(ActionEvent event) {
		try {
			if (stagePicker == null) {
				milSymbolPicker = new milSymbolPickerController();
				milSymbolPicker._stage_main = _stage_main;
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/milSymbolPicker.fxml"));
				loader.setController(milSymbolPicker);
				Parent root = (Parent) loader.load();
				Scene scene = new Scene(root, 800, 660);
				stagePicker = new Stage();
				milSymbolPicker.setDialogStage(stagePicker);
				stagePicker.setScene(scene);
				stagePicker.initModality(Modality.NONE);
				stagePicker.setMaximized(false);
				stagePicker.setResizable(false);
				stagePicker.setTitle("軍隊符號(2525B)選擇視窗");
				stagePicker.setScene(scene);
			}
			stagePicker.showAndWait();

			if (milSymbolPicker.isApplyClicked()) {
				System.out.println("I want draw " + milSymbolPicker.getDrawSymbolID());
				blnDrawMilSymbol = true;
				btnComploete.setDisable(false);
				btnCancel.setDisable(false);
				switch (milSymbolPicker.getSelectedShape()) {
				case "POINT":
					drawingGraphic = null;
					break;
				case "LINE":
					lineBuilder = new PolylineBuilder(MapManager.getSpatialReference());
					break;
				case "AREA":
					areaBuilder = new PolygonBuilder(MapManager.getSpatialReference());
					break;
				}

				selectedSIC = milSymbolPicker.getDrawSymbolID();
				selectedShape = milSymbolPicker.getSelectedShape();
			} else {
				blnDrawMilSymbol = false;
				btnComploete.setDisable(true);
				btnCancel.setDisable(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void InitLocalServerTask() {
		if (volume == null) {
			volume = new Volume(URL_TaiwanDem_new_P, progressBar, _stage_main);
			volume.Init();
		}

		if (highest == null) {
			highest = new HIGHESTController();
			highest.initialize(URL_TaiwanDem_new_P, progressBar, dropGraphicsOverlay);
		}

		if (stackprofile == null) {
			stackprofile = new StackProfile(URL_TaiwanDem_new_P, progressBar, _stage_main);
		}

		if (slope == null) {
			slope = new Slope(URL_TaiwanDem_new_P, progressBar, _stage_main, MapManager.arcgisScene);
		}

		Volume.server.startAsync();
	}

	@FXML
	void handleAreaClicked(ActionEvent event) {
		StopLocalSever();
		pointCollection.clear();

		apState = ApState.Volume;
		tmpDropGraphicsOverlay.getGraphics().clear();
		dropGraphicsOverlay.getGraphics().clear();

//		try {
//			// set up the scene
//			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Area.fxml"));
//			Parent root = loader.load();
//
//			AreaController areaController = loader.getController();
//			Scene scene = new Scene(root);
//
//			// set up the stage
//			Stage stage = new Stage();
//			stage.setTitle("面積及體積計算");
//			stage.setWidth(1024);
//			stage.setHeight(768);
//			stage.setScene(scene);
//			stage.initModality(Modality.WINDOW_MODAL);
//			stage.initOwner(_stage_main);
//			stage.showAndWait();
//			areaController.terminate();
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	@FXML
	void handleModelClicked(ActionEvent event) {
		apState = ApState.load3DModel;

		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File(System.getProperty("data.dir"), "./3dObject"));
		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("OBJ | DAE | 3DS", "*.obj;*.dae;*.3ds"));

		File selectedFile = fileChooser.showOpenDialog(_stage_main);
		if (selectedFile == null)
			return;

		ModelFilePath = selectedFile.getAbsolutePath();
		Alert alert = new Alert(Alert.AlertType.INFORMATION, "請於圖台上標註物件位置!!");
		alert.showAndWait();

	}

	@FXML
	void handleDistanceClicked(ActionEvent event) {
		try {
			// set up the scene
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DistanceMeasurementAnalysis.fxml"));
			Parent root = loader.load();

			DistanceMeasurementAnalysisController distanceMeasurementAnalysisController = loader.getController();
			Scene scene = new Scene(root);

			// set up the stage
			Stage stage = new Stage();
			stage.setTitle("距離量測");
			stage.setWidth(1024);
			stage.setHeight(768);
			stage.setScene(scene);
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(_stage_main);
			stage.showAndWait();
			distanceMeasurementAnalysisController.terminate();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	void handleHighestClicked(ActionEvent event) {
		StopLocalSever();
		pointCollection.clear();

		apState = ApState.Highest;
		tmpDropGraphicsOverlay.getGraphics().clear();
		dropGraphicsOverlay.getGraphics().clear();
	}

	@FXML
	void handleSlopeClicked(ActionEvent event) {
		StopLocalSever();
		pointCollection.clear();

		apState = ApState.Slope;
		tmpDropGraphicsOverlay.getGraphics().clear();
		dropGraphicsOverlay.getGraphics().clear();
	}

	private void StopLocalSever() {
		return;
//		LocalServer server = LocalServer.INSTANCE;
//		boolean GPtoolInstanceRun = false;
//
//		if (null != volume) {
//			GPtoolInstanceRun = true;
//			volume = null;
//		}
//		
//		if (null != stackprofile) {
//			GPtoolInstanceRun = true;
//			stackprofile = null;
//		}
//
//		if (null != slope) {
//			MapManager.arcgisScene.getOperationalLayers().remove(slope.myRasterLayer);
//			GPtoolInstanceRun = true;
//			slope = null;
//		}
//
//		if (null != highest) {
//			GPtoolInstanceRun = true;
//			highest = null;
//		}
//
//		if (GPtoolInstanceRun) {
//			try {
//				if (server != null)
//					server.stopAsync();
//				Thread.sleep(2500);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}

	@FXML
	void handleStackprofileClicked(ActionEvent event) {
		StopLocalSever();
		pointCollection.clear();

		if (stackprofile == null) {
			stackprofile = new StackProfile(URL_TaiwanDem_new_P, progressBar, _stage_main);
		}
		apState = ApState.StackProfile;
		tmpDropGraphicsOverlay.getGraphics().clear();
		dropGraphicsOverlay.getGraphics().clear();
	}

	private boolean doViewshed = false;

	@FXML
	void handleViewshedLocation2Clicked(ActionEvent event) {

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ViewShed.fxml"));
			Parent root = loader.load();
//		    controller = loader.getController();
			Scene scene = new Scene(root);

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

	@FXML
	void handleViewshedLocationClicked(ActionEvent event) {
		if (!doViewshed) {
			apState = ApState.ViewShed;
			ViewshedLocation();
			analysisOverlay.setVisible(true);
			gridPane1.setVisible(true);
			doViewshed = true;
		} else {
			apState = ApState.normal;
			analysisOverlay.setVisible(false);
			gridPane1.setVisible(false);
			doViewshed = false;
		}
	}

	@FXML
	void handleDSMClicked(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File(System.getProperty("data.dir"), "./samples-data"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("DEM | DSM", "*.tif"));

		File selectedFile = fileChooser.showOpenDialog(_stage_main);
		if (selectedFile == null)
			return;

		doPath(selectedFile);
	}

	@FXML
	void handlePackageClicked(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File(System.getProperty("data.dir"), "./samples-data"));
		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("TPKX | MMPK | VTPK", "*.tpkx;*.mmpk;*.vtpk"));

		File selectedFile = fileChooser.showOpenDialog(_stage_main);
		if (selectedFile == null)
			return;

		doPath2(selectedFile);
	}

	private void doPath(File selectedFile) {

		DSM(selectedFile.getAbsolutePath());
	}

	private void doPath2(File selectedFile) {
		if (selectedFile.getName().endsWith(".tpkx")) {
			handleTPKXClicked(selectedFile.getAbsolutePath());
		} else if (selectedFile.getName().endsWith(".mmpk")) {
			handleMMPKClicked(selectedFile.getAbsolutePath());
		} else if (selectedFile.getName().endsWith(".vtpk")) {
			handleVTPKClicked(selectedFile.getAbsolutePath());
		}
	}

	private void handleTPKXClicked(String path) {
		// load tiled layer and zoom to location
		TileCache tileCache = new TileCache(path);
		ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(tileCache);
		tiledLayer.loadAsync();
		tiledLayer.addDoneLoadingListener(() -> {
			if (tiledLayer.getLoadStatus() == LoadStatus.LOADED) {
				MapManager.arcgisScene.getOperationalLayers().add(tiledLayer);
				Point center = tiledLayer.getFullExtent().getCenter();
				Point center84 = (Point) GeometryEngine.project(center, SpatialReferences.getWgs84());
				Camera camera = new Camera(center84.getY(), center84.getX(), 10000, 0, 45, 0);
				MapManager.getSceneView().setViewpointCamera(camera);
			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR, "Tiled Layer Failed to Load!");
				alert.show();
			}
		});
	}

	private void handleMMPKClicked(String path) {
		/*
		 * mmpk如要取出圖層,加入現有的View
		 * 第一先載入mmpk到新的MapView
		 * 第二從MapView的OperationLayers取出每個圖層並COPY(複製)
		 * 第三將複製的圖層加入現有的View
		 */
		MapView mapView = new MapView();
		MobileMapPackage mobileMapPackage = new MobileMapPackage(path);

		mobileMapPackage.loadAsync();
		mobileMapPackage.addDoneLoadingListener(() -> {
			if (mobileMapPackage.getLoadStatus() == LoadStatus.LOADED && mobileMapPackage.getMaps().size() > 0) {
				
				ArcGISMap map = (ArcGISMap)mobileMapPackage.getMaps().get(0);
				mapView.setMap(map);
				

				boolean setCamera[] = {false};
				mapView.getMap().getOperationalLayers().forEach((layer) -> {
					FeatureLayer clone = ((FeatureLayer)layer).copy();
					clone.addDoneLoadingListener(() -> {
						if (setCamera[0] == false && clone.getLoadStatus() == LoadStatus.LOADED) {
							Point center = clone.getFullExtent().getCenter();
							Point center84 = (Point) GeometryEngine.project(center, SpatialReferences.getWgs84());
							Camera camera = new Camera(center84.getY(), center84.getX(), 10000, 0, 45, 0);
							MapManager.getSceneView().setViewpointCamera(camera);
							setCamera[0] = true;
						}
					});
					MapManager.arcgisScene.getOperationalLayers().add(clone);
				});		
				
				
			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load the mobile map package");
				alert.show();
			}
		});
	}

	private void handleVTPKClicked(String path) {
		ArcGISVectorTiledLayer tiledLayer_Raster = new ArcGISVectorTiledLayer(path);
		tiledLayer_Raster.addDoneLoadingListener(() -> {
			Point center = tiledLayer_Raster.getFullExtent().getCenter();
			Point center84 = (Point) GeometryEngine.project(center, SpatialReferences.getWgs84());
			Camera camera = new Camera(center84.getY(), center84.getX(), 10000, 0, 0, 0);
			MapManager.getSceneView().setViewpointCamera(camera);			
		});
		Basemap bMap = MapManager.arcgisScene.getBasemap();
		bMap.getBaseLayers().add(tiledLayer_Raster);
	}

	private void DSM(String path) {
		Raster raster = new Raster(path);
		RasterLayer myRasterLayer = new RasterLayer(raster);

		myRasterLayer.addDoneLoadingListener(() -> {
			if (myRasterLayer.getLoadStatus() == LoadStatus.LOADED) {
				MapManager.arcgisScene.getOperationalLayers().add(myRasterLayer);
				Point center = myRasterLayer.getFullExtent().getCenter();
				Point center84 = (Point) GeometryEngine.project(center, SpatialReferences.getWgs84());
				Camera camera = new Camera(center84.getY(), center84.getX(), 10000, 0, 45, 0);
				MapManager.getSceneView().setViewpointCamera(camera);

				Alert alert = new Alert(Alert.AlertType.INFORMATION, "載入/dsm/DSM_n5600w15915P_1984.tif");
				alert.show();
			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR, myRasterLayer.getLoadError().getMessage());
				alert.initOwner(_stage_main);
				alert.show();
			}
		});
		raster.loadAsync();
	}

	@FXML
	void handleSurfaceDistanceClicked(ActionEvent event) {
		apState = ApState.地表直線;
	}

	@FXML
	void handleElevationClicked(ActionEvent event) {
		apState = ApState.取高度;
//    	GetElevationAtAPointSample getElevationAtAPointSample = new GetElevationAtAPointSample();
//		getElevationAtAPointSample.Start(_stage_main);
	}

	private boolean doLineOfShght = false;

	@FXML
	void handleLineOfSightClicked(ActionEvent event) {
		if (!doLineOfShght) {
			apState = ApState.LineOfShight;
			LineOfSightLocation();
			analysisOverlay2.setVisible(true);
			doLineOfShght = true;
		} else {
			apState = ApState.normal;
			analysisOverlay2.setVisible(false);
			doLineOfShght = false;
		}

//    	LineOfSightLocationSample lineOfSightLocationSample = new LineOfSightLocationSample();
//    	lineOfSightLocationSample.start(_stage_main);
	}

	@FXML
	void handle3DClicked(ActionEvent event) {
		MapManager.setDefaultCamera();
	}
	
	@FXML
	void handle2DClicked(ActionEvent event) {
		Point2D screenCenter = MapManager.getSceneView().screenToLocal(MapManager.getSceneView().getWidth() * 0.5, MapManager.getSceneView().getHeight() * 0.5);
		ListenableFuture<Point> mapPoint = MapManager.getSceneView().screenToLocationAsync(screenCenter);
	
		mapPoint.addDoneListener(() -> {
			// get the current camera
		    Camera currentCamera = MapManager.getSceneView().getCurrentViewpointCamera();

		    if (currentCamera == null || currentCamera.getPitch() == 0.0)
		      return;

		    // rotate the camera using the delta pitch value
		    Camera newCamera;
			try {
				Point p = mapPoint.get();
				if (!(p.getX() == 0.0 && p.getY() == 0.0)) {
					newCamera = currentCamera.rotateAround(p, 0., -currentCamera.getPitch(), 0.);
				    // set the sceneview to the new camera
				    MapManager.getSceneView().setViewpointCameraAsync(newCamera, 2.0f);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
			
		});
	}

	@FXML
	void handlePrint2DClicked(ActionEvent event) {
		// create a file chooser for saving image
	      final FileChooser fileChooser = new FileChooser();
	      fileChooser.setInitialFileName("map-screenshot");
	      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG file (*.png)", "*.png"));
		
		
		// export image from map view
        ListenableFuture<Image> mapImage = MapManager.getSceneView().exportImageAsync();
        mapImage.addDoneListener(() -> {
          try {
            // get image
            Image image = mapImage.get();
            // choose a location to save the file
            File file = fileChooser.showSaveDialog(_stage_main);
            if (file != null) {
              // write the image to the save location
              ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });
	}
	
	@FXML
	void handleWorldClicked(ActionEvent event) {
		MapManager.getSceneView().setViewpointAsync(MapManager.getSceneView().getArcGISScene().getInitialViewpoint(), 2.0f);
	}
	
	private void Init() {
		// license with a license key

		// ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000433798,none,NKLFD4SZ8L2K8YAJM070");
		// ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000528559,none,1JHJH7E3CZ4HH6JRP132");

		String stand = "runtimestandard,1000,rud000433798,none,NKLFD4SZ8L2K8YAJM070";
		String analysis = "runtimeanalysis,1000,rud000012252,none,YYPH5AZAM8J8AZJTR058";

		List<String> ls = new ArrayList<String>();
		ls.add(analysis);

		ArcGISRuntimeEnvironment.setLicense(stand, ls);

//		ArcGISRuntimeEnvironment.setLicense("runtimeanalysis,1000,rud000012252,none,YYPH5AZAM8J8AZJTR058");
		// ArcGISRuntimeEnvironment.setLicense("runtimeanalysis,1000,rud000528559,none,RP53JNCLEJN71R1DF132");

		URL_TaiwanDem_new_P = new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
				.getAbsolutePath();

		symbolStyleName.put("CUBE", "立方體");
		symbolStyleName.put("CONE", "錐體");
		symbolStyleName.put("CYLINDER", "柱體");
		symbolStyleName.put("DIAMOND", "鑽石");
		symbolStyleName.put("SPHERE", "球體");
		symbolStyleName.put("TETRAHEDRON", "四面體");

		MapManager.InitMap3D();

		tmpGraphicsOverlay = new GraphicsOverlay();
		tmpGraphicsOverlay.getSceneProperties().setSurfacePlacement(SurfacePlacement.ABSOLUTE);
		MapManager.getSceneView().getGraphicsOverlays().add(0, tmpGraphicsOverlay);

		highGraphicsOverlay = new GraphicsOverlay(GraphicsOverlay.RenderingMode.DYNAMIC);
		highGraphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
		MapManager.getSceneView().getGraphicsOverlays().add(highGraphicsOverlay);

		tmpDropGraphicsOverlay = new GraphicsOverlay();
		tmpDropGraphicsOverlay.getSceneProperties().setSurfacePlacement(SurfacePlacement.DRAPED);
		MapManager.getSceneView().getGraphicsOverlays().add(0, tmpDropGraphicsOverlay);

		dropGraphicsOverlay = new GraphicsOverlay();
		dropGraphicsOverlay.getSceneProperties().setSurfacePlacement(SurfacePlacement.DRAPED);
		MapManager.getSceneView().getGraphicsOverlays().add(dropGraphicsOverlay);

		// create a point symbol and graphic to mark where elevation is being measured
		SimpleLineSymbol elevationLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFF0000, 3.0f);
		Graphic polylineGraphic = new Graphic();
		polylineGraphic.setSymbol(elevationLineSymbol);

		// create a text symbol and graphic to display the elevation of selected points
		TextSymbol elevationTextSymbol = new TextSymbol(20, null, 0xFFFF0000, TextSymbol.HorizontalAlignment.CENTER,
				TextSymbol.VerticalAlignment.BOTTOM);
		Graphic elevationTextGraphic = new Graphic();
		elevationTextGraphic.setSymbol(elevationTextSymbol);

		mapPane.getChildren().addAll(MapManager.sceneView);
		btnComploete.setDisable(true);
		btnCancel.setDisable(true);

		cmbDraw.getItems().addAll(drawOptions);
		cmbDraw.valueProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue ov, String oldValue, String newValue) {
//				Alert alertPoint = new Alert(Alert.AlertType.INFORMATION, "請於圖台上標註單點圖形繪製位置!!");
//				Alert alertPoint2 = new Alert(Alert.AlertType.INFORMATION, "請於圖台上標註多點圖形繪製形狀!!");

				switch (newValue) {
				case "點":
					apState = ApState.點;
//					alertPoint.showAndWait();
					break;
				case "球體":
					apState = ApState.球體;
//					alertPoint.showAndWait();
					break;
				case "椎體":
					apState = ApState.椎體;
//					alertPoint.showAndWait();
					break;
				case "柱體":
					apState = ApState.柱體;
//					alertPoint.showAndWait();
					break;
				case "立方體":
					apState = ApState.立方體;
//					alertPoint.showAndWait();
					break;
				case "線":
					apState = ApState.線;
//					alertPoint2.showAndWait();
					break;
				case "面":
					apState = ApState.面;
//					alertPoint2.showAndWait();
					break;
				}
			};
		});

		MapManager.getSceneView().setOnMouseClicked(e -> {
			if (e.isStillSincePress()) {
				if (e.getButton() == MouseButton.PRIMARY) {
					// clear any existing graphics from the HiGH graphics overlay
					highGraphicsOverlay.getGraphics().clear();

					Point2D screenPoint = new Point2D(e.getX(), e.getY());
					switch (apState) {
					case 地表直線:
						ListenableFuture<Point> mapPoint31 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint31.addDoneListener(() -> {
							try {
								Point p1 = mapPoint31.get();
								if (e.getClickCount() == 1)
									AddTmpDropPointGraphic(p1);

							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case Volume:
						ListenableFuture<Point> mapPoint48 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint48.addDoneListener(() -> {
							try {
								Point p1 = mapPoint48.get();
								if (e.getClickCount() == 1)
									AddTmpDropPointGraphic(p1);

							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case Highest:
						ListenableFuture<Point> mapPoint38 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint38.addDoneListener(() -> {
							try {
								Point p1 = mapPoint38.get();
								if (e.getClickCount() == 1)
									AddTmpDropPointGraphic(p1);

							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case Slope:
						ListenableFuture<Point> mapPoint28 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint28.addDoneListener(() -> {
							try {
								Point p1 = mapPoint28.get();
								if (e.getClickCount() == 1)
									AddTmpDropPointGraphic(p1);

							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case ViewShed:
						if (doViewshed) {
							ListenableFuture<Point> pointFuture = MapManager.getSceneView()
									.screenToLocationAsync(screenPoint);
							pointFuture.addDoneListener(() -> {
								try {
									Point point = pointFuture.get();
									PointBuilder pointBuilder = new PointBuilder(point);
									pointBuilder.setZ(point.getZ() + 50.0);
									viewPoint = pointBuilder.toGeometry();
									viewshed.setLocation(viewPoint);

								} catch (InterruptedException | ExecutionException ex) {
									ex.printStackTrace();
								}
							});
						}
						break;

					case LineOfShight:
						if (doLineOfShght) {
							// get the scene location from the screen position
							ListenableFuture<Point> pointFuture = MapManager.getSceneView()
									.screenToLocationAsync(screenPoint);
							pointFuture.addDoneListener(() -> {
								try {
									Point point = pointFuture.get();
									// update the target location
									lineOfSight.setTargetLocation(point);
								} catch (InterruptedException | ExecutionException ex) {
									ex.printStackTrace();
								}
							});
						}
						break;
					case StackProfile:
						ListenableFuture<Point> mapPoint18 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint18.addDoneListener(() -> {
							try {
								Point p1 = mapPoint18.get();
								if (e.getClickCount() == 1)
									AddTmpDropPointGraphic(p1);

							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case 取高度:
						// convert the screen point to a point on the surface
						Point relativeSurfacePoint = MapManager.getSceneView().screenToBaseSurface(screenPoint);

						// check that the point is on the surface
						if (relativeSurfacePoint != null) {
							// construct a polyline to use as a marker
							PolylineBuilder polylineBuilder = new PolylineBuilder(
									relativeSurfacePoint.getSpatialReference());
							Point baseOfPolyline = new Point(relativeSurfacePoint.getX(), relativeSurfacePoint.getY(),
									0);
							polylineBuilder.addPoint(baseOfPolyline);
							Point topOfPolyline = new Point(baseOfPolyline.getX(), baseOfPolyline.getY(), 750);
							polylineBuilder.addPoint(topOfPolyline);
							Polyline markerPolyline = polylineBuilder.toGeometry();
							polylineGraphic.setGeometry(markerPolyline);
							highGraphicsOverlay.getGraphics().add(polylineGraphic);

							// get the surface elevation at the surface point
							ListenableFuture<Double> elevationFuture = MapManager.arcgisScene.getBaseSurface()
									.getElevationAsync(relativeSurfacePoint);
							elevationFuture.addDoneListener(() -> {
								try {
									// get the surface elevation
									Double elevation = elevationFuture.get();

									// update the text in the elevation marker
									elevationTextSymbol.setText(Math.round(elevation) + " 公尺");
									elevationTextGraphic.setGeometry(topOfPolyline);
									highGraphicsOverlay.getGraphics().add(elevationTextGraphic);

									apState = ApState.normal;
								} catch (InterruptedException | ExecutionException ex) {
									new Alert(Alert.AlertType.ERROR, ex.getCause().getMessage()).show();
								}
							});
						}
						break;
					case 點:
						ListenableFuture<Point> mapPoint2 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint2.addDoneListener(() -> {
							try {
								Point p1 = mapPoint2.get();
								Point mapAns1 = QueryLocation(p1, 1000.0);
								if (mapAns1 == null)
									return;
								AddPointGraphics(mapAns1);
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case 線:
						ListenableFuture<Point> mapPoint8 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint8.addDoneListener(() -> {
							try {
								Point p1 = mapPoint8.get();
								Point mapAns1 = QueryLocation(p1, 1000.0);
								if (mapAns1 == null)
									return;
								AddTmpPointGraphic(mapAns1);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case 面:
						ListenableFuture<Point> mapPoint7 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint7.addDoneListener(() -> {
							try {
								Point p1 = mapPoint7.get();
								Point mapAns1 = QueryLocation(p1, 1000.0);
								if (mapAns1 == null)
									return;
								AddTmpPointGraphic(mapAns1);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case 球體:
						ListenableFuture<Point> mapPoint3 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint3.addDoneListener(() -> {
							try {
								Point p1 = mapPoint3.get();
								Point mapAns1 = QueryLocation(p1, 1000.0);
								if (mapAns1 == null)
									return;
								DrawSymbolStyles(Style.SPHERE, mapAns1);
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case 椎體:
						ListenableFuture<Point> mapPoint4 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint4.addDoneListener(() -> {
							try {
								Point p1 = mapPoint4.get();
								Point mapAns1 = QueryLocation(p1, 1000.0);
								if (mapAns1 == null)
									return;
								DrawSymbolStyles(Style.CONE, mapAns1);
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case 柱體:
						ListenableFuture<Point> mapPoint5 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint5.addDoneListener(() -> {
							try {
								Point p1 = mapPoint5.get();
								Point mapAns1 = QueryLocation(p1, 1000.0);
								if (mapAns1 == null)
									return;
								DrawSymbolStyles(Style.CYLINDER, mapAns1);
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case 立方體:
						ListenableFuture<Point> mapPoint6 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);
						mapPoint6.addDoneListener(() -> {
							try {
								Point p1 = mapPoint6.get();
								Point mapAns1 = QueryLocation(p1, 1000.0);
								if (mapAns1 == null)
									return;
								DrawSymbolStyles(Style.CUBE, mapAns1);
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
						break;
					case load3DModel:
						ListenableFuture<Point> mapPoint1 = MapManager.getSceneView()
								.screenToLocationAsync(screenPoint);

						mapPoint1.addDoneListener(() -> {
							try {
								Point p1 = mapPoint1.get();
								Point mapAns1 = QueryLocation(p1, 1000);
								if (mapAns1 == null)
									return;
								DrawModelSysmbol(mapAns1);
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						});
						break;
					default:
						if (blnDrawMilSymbol) {
							// 開始畫軍隊符號
							ListenableFuture<Point> mapPoint = MapManager.getSceneView()
									.screenToLocationAsync(screenPoint);

							mapPoint.addDoneListener(() -> {
								try {
									Point p = mapPoint.get();
									Point pointLocation = QueryLocation(p, 1000.0);
									if (pointLocation == null)
										return;
									switch (selectedShape) {
									case "POINT":
										if (drawingGraphic == null) {
											drawingGraphic = new Graphic(pointLocation, MapManager.redCircleSymbol);
											MapManager.graphicsOverlayDraw3D.getGraphics().add(drawingGraphic);
										} else {
											drawingGraphic.setGeometry(pointLocation);
										}
										break;
									case "LINE":
										lineBuilder.addPoint(pointLocation);
										drawingGraphic = new Graphic(pointLocation, MapManager.redCircleSymbol);
										MapManager.graphicsOverlayDraw3D.getGraphics().add(drawingGraphic);
										break;
									case "AREA":
										areaBuilder.addPoint(pointLocation);
										drawingGraphic = new Graphic(pointLocation, MapManager.redCircleSymbol);
										MapManager.graphicsOverlayDraw3D.getGraphics().add(drawingGraphic);
										break;
									}
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (ExecutionException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							});
						} else {
							// 先辨別3D graphicsOverlay圖層
							ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics = MapManager.getSceneView()
									.identifyGraphicsOverlayAsync(MapManager.graphicsOverlay3D, screenPoint, 10, false);

							identifyGraphics.addDoneListener(() -> Platform.runLater(() -> {
								try {
									IdentifyGraphicsOverlayResult result = identifyGraphics.get();
									List<Graphic> graphics = result.getGraphics();
									if (!graphics.isEmpty()) {
										Graphic graphic = graphics.get(0);
										if (graphic.getAttributes().containsKey("name")) {
											txtName.setText(graphic.getAttributes().get("name").toString());
											selectedGraphic = graphic;
										} else {
											if (graphic.getSymbol().getClass().equals(TextSymbol.class)) {
												txtName.setText(((TextSymbol) graphic.getSymbol()).getText());
												selectedGraphic = graphic;
											}
										}

										if (graphic.getSymbol().getClass() == SimpleLineSymbol.class) {
											double distance = getPolylinDistance((Polyline) graphic.getGeometry());

											Alert dialog = new Alert(AlertType.INFORMATION);
											dialog.setHeaderText("空間直線距離結果");
											dialog.setContentText(distance + "公里");
											dialog.showAndWait();
										}
									} else {
										// 後辨別兵棋符號圖層
										ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics2 = MapManager
												.getSceneView().identifyGraphicsOverlayAsync(
														MapManager.graphicsOverlay_military_3Dsymbol, screenPoint, 10,
														false);

										identifyGraphics2.addDoneListener(() -> Platform.runLater(() -> {
											try {
												IdentifyGraphicsOverlayResult result2 = identifyGraphics2.get();
												List<Graphic> graphics2 = result2.getGraphics();
												if (!graphics2.isEmpty()) {
													Graphic graphic = graphics2.get(0);
													if (graphic.getAttributes().containsKey("name")) {
														txtName.setText(graphic.getAttributes().get("name").toString());
														selectedGraphic = graphic;
													}
												} else {
													// 後辨別是否為 Drop Graphic Overlay上的線,
													// 計算地表距離
													ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics3 = MapManager
															.getSceneView().identifyGraphicsOverlayAsync(
																	dropGraphicsOverlay, screenPoint, 10, false);

													identifyGraphics3.addDoneListener(() -> Platform.runLater(() -> {
														try {
															IdentifyGraphicsOverlayResult result3 = identifyGraphics3
																	.get();
															List<Graphic> graphics3 = result3.getGraphics();
															if (!graphics3.isEmpty()) {
																Polyline graphic = (Polyline) graphics3.get(0)
																		.getGeometry();

																double v = getPolylinDistance(graphic);
																Alert dialog = new Alert(AlertType.INFORMATION);
																dialog.setHeaderText("地表直線距離:");
																dialog.setContentText(String.format("%.2f公里", v));
																dialog.showAndWait();

															}
														} catch (Exception e1) {
															// TODO Auto-generated catch block
															e1.printStackTrace();
														}
													}));
												}

											} catch (Exception e1) {
												// TODO Auto-generated catch block
												e1.printStackTrace();
											}
										}));
									}

								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}));
						}
						break;
					}
				}
			}

			if (e.getButton() == MouseButton.SECONDARY) {
				// clear any existing graphics from the HiGH graphics overlay
				highGraphicsOverlay.getGraphics().clear();

				Point2D screenPoint = new Point2D(e.getX(), e.getY());
				switch (apState) {
				case 線:
					ListenableFuture<Point> mapPoint8 = MapManager.getSceneView().screenToLocationAsync(screenPoint);
					mapPoint8.addDoneListener(() -> {
						try {
							Point p1 = mapPoint8.get();
							Point mapAns1 = QueryLocation(p1, 500.0);
							if (mapAns1 == null)
								return;
							AddTmpPointGraphic(mapAns1);

							if (pointCollection.size() >= 2) {
								AddLineGraphics();
								tmpGraphicsOverlay.getGraphics().clear();
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							}
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					});
					break;
				case 面:
					ListenableFuture<Point> mapPoint7 = MapManager.getSceneView().screenToLocationAsync(screenPoint);
					mapPoint7.addDoneListener(() -> {
						try {
							Point p1 = mapPoint7.get();
							Point mapAns1 = QueryLocation(p1, 1000.0);
							if (mapAns1 == null)
								return;
							AddTmpPointGraphic(mapAns1);

							if (pointCollection.size() >= 3) {
								AddPolygonGraphics();
								tmpGraphicsOverlay.getGraphics().clear();
								// 儲存3D GraphicOverlay的Graphics到Json檔
								gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
								apState = ApState.normal;
							}
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					});
					break;
				case 地表直線:
					ListenableFuture<Point> mapPoint32 = MapManager.getSceneView().screenToLocationAsync(screenPoint);
					mapPoint32.addDoneListener(() -> {
						try {
							Point p1 = mapPoint32.get();
							if (e.getClickCount() == 1)
								AddTmpDropPointGraphic(p1);

							if (pointCollection.size() >= 2) {
								AddDropLineGraphics();
								tmpDropGraphicsOverlay.getGraphics().clear();

								progressBar.setVisible(true);
								Platform.runLater(() -> {
									try {

										progressBar.setProgress(30);
										Thread.sleep(2000);
										progressBar.setProgress(70);
										Thread.sleep(5000);
										progressBar.setVisible(false);
									} catch (InterruptedException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
								});

								apState = ApState.normal;
							}
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					});
					break;
				case StackProfile:
					ListenableFuture<Point> mapPoint18 = MapManager.getSceneView().screenToLocationAsync(screenPoint);
					mapPoint18.addDoneListener(() -> {
						try {
							Point p1 = mapPoint18.get();
							if (e.getClickCount() == 1)
								AddTmpDropPointGraphic(p1);

							if (pointCollection.size() >= 2 && null != stackprofile.gpTask) {
								AddDropLineGraphics();
								tmpDropGraphicsOverlay.getGraphics().clear();
								try {
									stackprofile.DoStackProfile((Polyline) dropLineGraphic.getGeometry());
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								apState = ApState.normal;
							}
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					});
					break;
				case Slope:
					ListenableFuture<Point> mapPoint28 = MapManager.getSceneView().screenToLocationAsync(screenPoint);
					mapPoint28.addDoneListener(() -> {
						try {
							Point p1 = mapPoint28.get();
							if (e.getClickCount() == 1)
								AddTmpDropPointGraphic(p1);

							if (pointCollection.size() >= 4 && null != slope.gpTask) {
								AddDropPolygonGraphics();
								dropGraphicsOverlay.getGraphics().clear(); // 可去除,保留畫polygon
								tmpDropGraphicsOverlay.getGraphics().clear();
								try {
									slope.DoSlope((Polygon) dropPolygonGraphic.getGeometry());
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								apState = ApState.normal;
							}
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					});
					break;
				case Highest:
					ListenableFuture<Point> mapPoint38 = MapManager.getSceneView().screenToLocationAsync(screenPoint);
					mapPoint38.addDoneListener(() -> {
						try {
							Point p1 = mapPoint38.get();
							if (e.getClickCount() == 1)
								AddTmpDropPointGraphic(p1);

							if (pointCollection.size() >= 3 && null != highest.gpTask) {
								AddDropPolygonGraphics();
								tmpDropGraphicsOverlay.getGraphics().clear();
								try {
									highest.DoHighest((Polygon) dropPolygonGraphic.getGeometry());
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								apState = ApState.normal;
							}
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					});
					break;
				case Volume:
					ListenableFuture<Point> mapPoint48 = MapManager.getSceneView().screenToLocationAsync(screenPoint);
					mapPoint48.addDoneListener(() -> {
						try {
							Point p1 = mapPoint48.get();
							if (e.getClickCount() == 1)
								AddTmpDropPointGraphic(p1);

							if (pointCollection.size() >= 4 && null != volume.gpTask) {
								AddDropPolygonGraphics();
								tmpDropGraphicsOverlay.getGraphics().clear();
								try {
									volume.DoVolume((Polygon) dropPolygonGraphic.getGeometry());
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							}
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					});
					break;
				}
			}
		});

		gf.OpenJsonFileImportGraphics(MapManager.graphicsOverlay3D);
		gf2525B.OpenJsonFileImportGraphicsForMilitarySymbol(MapManager.graphicsOverlay_military_3Dsymbol);

		// 儲存3D GraphicOverlay的Graphics到Json檔
		// gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay3D, null);
	}

	private Point QueryLocation(Point p, double z) {
		// Create the custom dialog.
		Dialog<Pair<String, String>> dialog4 = new Dialog<>();
		dialog4.setTitle("座標設定");
		dialog4.setHeaderText("請設定座標位置");

		// Set the button types.
		ButtonType OKButtonType = new ButtonType("確定", ButtonData.OK_DONE);
		dialog4.getDialogPane().getButtonTypes().addAll(OKButtonType, ButtonType.CANCEL);

		// Create the username and password labels and fields.
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 40, 10, 10));

		TextField txtLon = new TextField();
		txtLon.setText(String.format("%.3f", p.getX()));
		TextField txtLat = new TextField();
		txtLat.setText(String.format("%.3f", p.getY()));
		TextField txtHeight = new TextField();
		txtHeight.setText(String.format("%.3f", z));

		grid.add(new Label("經度:"), 0, 0);
		grid.add(txtLon, 1, 0);
		grid.add(new Label("緯度:"), 0, 1);
		grid.add(txtLat, 1, 1);
		grid.add(new Label("高度:"), 0, 2);
		grid.add(txtHeight, 1, 2);

		dialog4.getDialogPane().setContent(grid);

		// Request focus on the username field by default.
		Platform.runLater(() -> txtLon.requestFocus());

		// Convert the result to a username-password-pair when the login button is
		// clicked.
		dialog4.setResultConverter(dialogButton -> {
			if (dialogButton == OKButtonType) {
				return new Pair<>(txtLon.getText() + "," + txtLat.getText(), txtHeight.getText());
			}
			return null;
		});

		Optional<Pair<String, String>> result4 = dialog4.showAndWait();
		try {

			String lonlat = result4.get().getKey();
			String[] vs = lonlat.split(",");
			String hei = result4.get().getValue();

			return new Point(Double.parseDouble(vs[0]), Double.parseDouble(vs[1]), Double.parseDouble(hei),
					p.getSpatialReference());
		} catch (Exception ex) {
			return null;
		}
	}

	private double getPolylinDistance(Polyline line) {
		int i = 0;
		double distance = 0.0;
		while (i < line.getParts().get(0).size()) {
			Point p1 = line.getParts().get(0).getPoint(i);
			Point p2 = line.getParts().get(0).getPoint(i + 1);

			Point p11 = (Point) GeometryEngine.project(p1, SpatialReferences.getWebMercator());
			Point p21 = (Point) GeometryEngine.project(p2, SpatialReferences.getWebMercator());

			distance += Math.sqrt(Math.pow(p11.getX() - p21.getX(), 2) + Math.pow(p11.getY() - p21.getY(), 2)
					+ Math.pow(p11.getZ() - p21.getZ(), 2)) / 1000;
			i++;
		}

		return Math.round(distance * 100.0) / 100.0;
	}

	private void AddDropPointGraphic(Point p1) {
		dropGraphicsOverlay.getGraphics().clear();

		Point buoy1Loc = new Point(p1.getX(), p1.getY(), Integer.parseInt(txtAltitude.getText()),
				MapManager.getSpatialReference());
		pointCollection.add(buoy1Loc);

		// create a red (0xFFFF0000) circle simple marker symbol
		SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
				ColorUtil.colorToArgb(Color.RED), 3);

		// create graphics and add to graphics overlay
		Graphic buoyGraphic1 = new Graphic(buoy1Loc, redCircleSymbol);

		dropGraphicsOverlay.getGraphics().add(buoyGraphic1);
	}

	private void AddTmpPointGraphic(Point p1) {
		pointCollection.add(p1);

		// create a red (0xFFFF0000) circle simple marker symbol
		SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
				ColorUtil.colorToArgb(Color.RED), 3);

		// create graphics and add to graphics overlay
		Graphic buoyGraphic1 = new Graphic(p1, redCircleSymbol);

		tmpGraphicsOverlay.getGraphics().add(buoyGraphic1);
	}

	private void AddTmpDropPointGraphic(Point p1) {
		Point buoy1Loc = new Point(p1.getX(), p1.getY(), 0, MapManager.getSpatialReference());
		pointCollection.add(buoy1Loc);

		// create a red (0xFFFF0000) circle simple marker symbol
		SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
				ColorUtil.colorToArgb(Color.RED), 3);

		// create graphics and add to graphics overlay
		Graphic buoyGraphic1 = new Graphic(buoy1Loc, redCircleSymbol);

		tmpDropGraphicsOverlay.getGraphics().add(buoyGraphic1);
	}

	private void AddPointGraphics(Point p1) {
		Point buoy1Loc = new Point(p1.getX(), p1.getY(), p1.getZ(), MapManager.getSpatialReference());
		// create a red (0xFFFF0000) circle simple marker symbol
		SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
				ColorUtil.colorToArgb(Color.RED), 10);

		// create graphics and add to graphics overlay
		Graphic buoyGraphic1 = new Graphic(buoy1Loc, redCircleSymbol);
		buoyGraphic1.getAttributes().put("name", "任意點");

		MapManager.graphicsOverlay3D.getGraphics().add(buoyGraphic1);
	}

	private Graphic lineGraphic = null;
	private Graphic dropLineGraphic = null;
	private Graphic dropPolygonGraphic = null;

	private void AddDropLineGraphics() {

		// create the polyline from the point collection
		Polyline polyline = new Polyline(pointCollection);

		// create a purple (0xFF800080) simple line symbol
		SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,
				ColorUtil.colorToArgb(Color.LIGHTBLUE), 2);

		// create the graphic with polyline and symbol
		dropLineGraphic = new Graphic(polyline, lineSymbol);
		dropLineGraphic.getAttributes().put("name", "任意線");

		// add graphic to the graphics overlay
		dropGraphicsOverlay.getGraphics().add(dropLineGraphic);

		pointCollection.clear();
	}

	private void AddDropPolygonGraphics() {

		// create the polyline from the point collection
		Polygon polygon = new Polygon(pointCollection);

		// create a green (ColorUtil.colorToArgb(Color.LIGHTBLUE)) simple line symbol
		SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH,
				ColorUtil.colorToArgb(Color.LIGHTBLUE), 1);
		// create a green (ColorUtil.colorToArgb(Color.LIGHTBLUE)) mesh simple fill
		// symbol
		SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.NULL,
				ColorUtil.colorToArgb(Color.TRANSPARENT), outlineSymbol);

		// create the graphic with polyline and symbol
		dropPolygonGraphic = new Graphic(polygon, fillSymbol);

		// add graphic to the graphics overlay
		dropGraphicsOverlay.getGraphics().add(dropPolygonGraphic);

		pointCollection.clear();
	}

	private void AddLineGraphics() {

		// create the polyline from the point collection
		Polyline polyline = new Polyline(pointCollection);

		// create a purple (0xFF800080) simple line symbol
		SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF800080, 4);

		// create the graphic with polyline and symbol
		lineGraphic = new Graphic(polyline, lineSymbol);
		lineGraphic.getAttributes().put("name", "任意線");

		// add graphic to the graphics overlay
		MapManager.graphicsOverlay3D.getGraphics().add(lineGraphic);

		pointCollection.clear();
	}

	private void AddPolygonGraphics() {

		// create the polyline from the point collection
		Polygon polygon = new Polygon(pointCollection);
		// create a green (ColorUtil.colorToArgb(Color.LIGHTBLUE)) simple line symbol
		SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH,
				ColorUtil.colorToArgb(Color.LIGHTBLUE), 1);
		// create a green (ColorUtil.colorToArgb(Color.LIGHTBLUE)) mesh simple fill
		// symbol
		SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.CROSS,
				ColorUtil.colorToArgb(Color.LIGHTBLUE), outlineSymbol);

		// create the graphic with polyline and symbol
		Graphic graphic = new Graphic(polygon, fillSymbol);
		graphic.getAttributes().put("name", "任意面");

		// add graphic to the graphics overlay
		MapManager.graphicsOverlay3D.getGraphics().add(graphic);

		pointCollection.clear();
	}

	private void DrawSymbolStyles(Style symbolStyle, Point p1) {
		// Create a graphic for each symbol type and add it to the scene.

		// Create the symbol.
		SimpleMarkerSceneSymbol symbol = new SimpleMarkerSceneSymbol(symbolStyle, ColorUtil.colorToArgb(Color.PURPLE),
				300, 300, 300, SceneSymbol.AnchorPosition.CENTER);

		// Create the graphic from the geometry and the symbol.
		Graphic item = new Graphic(p1, symbol);
		item.getAttributes().put("kind", symbolStyle.name());
		item.getAttributes().put("name", symbolStyleName.get(symbolStyle.toString()));

		// Add the graphic to the overlay.
		MapManager.graphicsOverlay3D.getGraphics().add(item);

	}

	private void DrawModelSysmbol(Point p1) {
		double scale = 0.0;
		if (ModelFilePath.endsWith(".dae")) {
			// 飛機
			scale = 1.0;
			ModelSceneSymbol airModelSymbol = new ModelSceneSymbol(ModelFilePath, scale);
			airModelSymbol.loadAsync();

			Graphic objectGraphic = new Graphic(p1, airModelSymbol);
			objectGraphic.getAttributes().put("HEADING", 0);
			objectGraphic.getAttributes().put("PITCH", 0);
			objectGraphic.getAttributes().put("name", "飛機");
			MapManager.graphicsOverlay3D.getGraphics().add(objectGraphic);

		} else if (ModelFilePath.endsWith(".obj")) {
			// 軍車
			double hei = 0.0;
			if (ModelFilePath.endsWith("MissileCar.obj")) {
				scale = 30.0;
				hei = 200.0;
			} else if (ModelFilePath.endsWith("x-35_obj.obj")) {
				scale = 8.0;
				hei = 200.0;
			} else {
				scale = 0.5;
				hei = p1.getZ();
			}
			ModelSceneSymbol carModelSymbol = new ModelSceneSymbol(ModelFilePath, scale);
			carModelSymbol.loadAsync();

			Point p2 = new Point(p1.getX(), p1.getY(), hei, MapManager.getSpatialReference());
			Graphic objectGraphic2 = new Graphic(p2, carModelSymbol);
			objectGraphic2.getAttributes().put("HEADING", 0);
			objectGraphic2.getAttributes().put("PITCH", 0);
			objectGraphic2.getAttributes().put("name", "軍車");
			MapManager.graphicsOverlay3D.getGraphics().add(objectGraphic2);

		}
		if (ModelFilePath.endsWith(".3ds")) {
			// 飛彈
			if (ModelFilePath.endsWith("HAL Tejas.3ds"))
				scale = 80.0;
			else if (ModelFilePath.endsWith("uh60.3ds"))
				scale = 30.0;
			else
				scale = 1.0;
			ModelSceneSymbol milModelSymbol = new ModelSceneSymbol(ModelFilePath, scale);
			milModelSymbol.loadAsync();

			Graphic objectGraphic3 = new Graphic(p1, milModelSymbol);
			objectGraphic3.getAttributes().put("HEADING", 0);
			objectGraphic3.getAttributes().put("PITCH", 0);
			objectGraphic3.getAttributes().put("name", "飛彈");
			MapManager.graphicsOverlay3D.getGraphics().add(objectGraphic3);
		}
	}

	private void ViewshedLocation() {
		// create a viewshed from the camera
		Point location = new Point(120.52, 23.5, 200.0);

		// set the camera
		Camera camera = new Camera(location, 200.0, 20.0, 90.0, 0.0);
		MapManager.getSceneView().setViewpointCamera(camera);

		if (analysisOverlay == null) {
			viewshed = new LocationViewshed(location, headingSlider.getValue(), pitchSlider.getValue(),
					horizontalAngleSlider.getValue(), verticalAngleSlider.getValue(), minDistanceSlider.getValue(),
					maxDistanceSlider.getValue());
			// set the colors of the visible and obstructed areas
			Viewshed.setVisibleColor(0xCC00FF00);
			Viewshed.setObstructedColor(0xCCFF0000);
			// set the color and show the frustum outline
			Viewshed.setFrustumOutlineColor(0xCC0000FF);
			viewshed.setFrustumOutlineVisible(true);

			// create an analysis overlay to add the viewshed to the scene view
			analysisOverlay = new AnalysisOverlay();
			analysisOverlay.getAnalyses().add(viewshed);
			MapManager.getSceneView().getAnalysisOverlays().add(analysisOverlay);

			// toggle visibility
			visibilityToggle.selectedProperty().addListener(e -> viewshed.setVisible(visibilityToggle.isSelected()));
			visibilityToggle.textProperty().bind(Bindings.createStringBinding(
					() -> visibilityToggle.isSelected() ? "ON" : "OFF", visibilityToggle.selectedProperty()));
			frustumToggle.selectedProperty()
					.addListener(e -> viewshed.setFrustumOutlineVisible(frustumToggle.isSelected()));
			frustumToggle.textProperty().bind(Bindings.createStringBinding(
					() -> frustumToggle.isSelected() ? "ON" : "OFF", frustumToggle.selectedProperty()));
			// heading slider
			headingSlider.valueProperty().addListener(e -> viewshed.setHeading(headingSlider.getValue()));
			// pitch slider
			pitchSlider.valueProperty().addListener(e -> viewshed.setPitch(pitchSlider.getValue()));
			// horizontal angle slider
			horizontalAngleSlider.valueProperty()
					.addListener(e -> viewshed.setHorizontalAngle(horizontalAngleSlider.getValue()));
			// vertical angle slider
			verticalAngleSlider.valueProperty()
					.addListener(e -> viewshed.setVerticalAngle(verticalAngleSlider.getValue()));
			// distance sliders
			minDistanceSlider.valueProperty().addListener(e -> viewshed.setMinDistance(minDistanceSlider.getValue()));
			maxDistanceSlider.valueProperty().addListener(e -> viewshed.setMaxDistance(maxDistanceSlider.getValue()));
		}
	}

	private void LineOfSightLocation() {
		if (analysisOverlay2 == null) {
			// create an analysis overlay for the line of sight
			analysisOverlay2 = new AnalysisOverlay();
			MapManager.getSceneView().getAnalysisOverlays().add(analysisOverlay2);

			// initialize a line of sight analysis and add it to the analysis overlay
			Point observerLocation = new Point(120.362, 22.748, 100, SpatialReferences.getWgs84());
			Point targetLocation = new Point(121, 22.94, 1312, SpatialReferences.getWgs84());
			lineOfSight = new LocationLineOfSight(observerLocation, targetLocation);
			analysisOverlay2.getAnalyses().add(lineOfSight);
		}
		// initialize the viewpoint
		Camera camera = new Camera(new Point(120.362, 22.748, 30590, SpatialReferences.getWgs84()), 11, 0, 0);
		MapManager.getSceneView().setViewpointCamera(camera);
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub
		Init();
		InitLocalServerTask();
	}
}