package controller;
/*
 * Copyright 2017 Esri.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService.ServiceType;
import com.esri.arcgisruntime.localserver.LocalServer;
import com.esri.arcgisruntime.localserver.LocalServerStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingFeatures;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingJob;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingMultiValue;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameter;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameters;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingRaster;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingString;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingTask;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class StackProfileController {

	@FXML
	private Button btnGenerate;
	@FXML
	private Button btnClear;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private SceneView sceneView;
	private ArcGISScene arcgisScene;

	private GeoprocessingTask gpTask;
	private LocalGeoprocessingService localGPService;
	private GraphicsOverlay graphicsOverlayDraw3D;
	private PolylineBuilder lineBuilder;
	private SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFFF0000,
			10);
	private String rasterURL;
	private static LocalServer server;
	public Stage _stage_main;

	/**
	 * Called after FXML loads. Sets up scene and map and configures property
	 * bindings.
	 */
	public void initialize() {
		//ArcGISRuntimeEnvironment.setLicense("runtimeanalysis,1000,rud000012252,none,YYPH5AZAM8J8AZJTR058");
//		ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000528559,none,1JHJH7E3CZ4HH6JRP132");

		try {
			// authentication with an API key or named user is required to access basemaps
			// and other location services
			String yourAPIKey = "AAPKc09de8d4475749268343720606d7ac25ExYd7IKhiGdeC0PMB-2Y_W4QxDJQpkc2-GJiuUxSex-NDOcU9YQnwUBcSLntlcYE";
			ArcGISRuntimeEnvironment.setApiKey(yourAPIKey);

			TileCache tileCache_Raster = new TileCache("C:/test/World.tpk");
			ArcGISTiledLayer tiledLayer_Raster = new ArcGISTiledLayer(tileCache_Raster);
			Basemap basemap_Raster = new Basemap(tiledLayer_Raster);
			arcgisScene = new ArcGISScene(basemap_Raster);

			// set the map to the map view
			sceneView.setArcGISScene(arcgisScene);

			// 高程離線圖資
			rasterURL = new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
					.getAbsolutePath();
			ArrayList<String> list = new ArrayList<String>();
			list.add(rasterURL);

			Surface surface = new Surface();
			surface.getElevationSources().add(new RasterElevationSource(list));
			surface.setElevationExaggeration(1);
			arcgisScene.setBaseSurface(surface);

			Camera camera = new Camera(23, 121, 50000, 0, 10, 0);
			sceneView.setViewpointCamera(camera);

			graphicsOverlayDraw3D = new GraphicsOverlay();
			sceneView.getGraphicsOverlays().add(graphicsOverlayDraw3D);

			lineBuilder = new PolylineBuilder(sceneView.getSpatialReference());
			sceneView.setOnMouseClicked(e -> {
				if (e.isStillSincePress()) {
					if (e.getButton() == MouseButton.PRIMARY) {
						Point2D screenPoint = new Point2D(e.getX(), e.getY());

						// 開始畫軍隊符號
						ListenableFuture<Point> mapPoint = sceneView.screenToLocationAsync(screenPoint);

						mapPoint.addDoneListener(() -> {
							try {

								Point pointLocation = mapPoint.get();

								lineBuilder.addPoint(pointLocation);
								Graphic drawingGraphic = new Graphic(pointLocation, redCircleSymbol);
								graphicsOverlayDraw3D.getGraphics().add(drawingGraphic);

							} catch (Exception ex) {
								ex.printStackTrace();
							}
						});
					}
				}
			});

			// check that local server install path can be accessed
			if (LocalServer.INSTANCE.checkInstallValid()) {
				progressBar.setVisible(true);
				server = LocalServer.INSTANCE;
				// start the local server
				server.addStatusChangedListener(status -> {
					if (server.getStatus() == LocalServerStatus.STARTED) {
						try {
							String gpServiceURL = new File(System.getProperty("data.dir"),
									"./samples-data/local_server/StackProfile.gpkx").getAbsolutePath();
							// need map server result to add contour lines to map
							localGPService = new LocalGeoprocessingService(gpServiceURL,
									ServiceType.ASYNCHRONOUS_SUBMIT);
						} catch (Exception e) {
							e.printStackTrace();
						}

						localGPService.addStatusChangedListener(s -> {
							// create geoprocessing task once local geoprocessing service is started
							if (s.getNewStatus() == LocalServerStatus.STARTED) {
								// add `/Contour` to use contour geoprocessing tool
								gpTask = new GeoprocessingTask(localGPService.getUrl() + "/Stack Profile");
								btnClear.disableProperty().bind(btnGenerate.disabledProperty().not());
								btnGenerate.setDisable(false);
								progressBar.setVisible(false);
							}
						});
						localGPService.startAsync();
					} else if (server.getStatus() == LocalServerStatus.FAILED) {
						showMessage("Local Geoprocessing Load Error", "Local Geoprocessing Failed to load.");
					}
				});

				LocalServer.INSTANCE.setTempDataPath("C:\\TMP");
				server.startAsync();
			} else {
				showMessage("Local Server Load Error", "Local Server install path couldn't be located.");
			}

		} catch (Exception e) {
			// on any exception, print the stack trace
			e.printStackTrace();
		}

	}

	private static double totalDist = 0.0;

	/**
	 * Creates a Map Image Layer that displays contour lines on the map using the
	 * interval the that is set.
	 * 
	 * @throws IOException
	 */
	@FXML
	protected void handleGenerateContours() throws IOException {

		if (graphicsOverlayDraw3D.getGraphics().size() < 2)
			return;

		// tracking progress of creating contour map
		progressBar.setVisible(true);
		// create parameter using interval set
		GeoprocessingParameters gpParameters = new GeoprocessingParameters(
				GeoprocessingParameters.ExecutionType.ASYNCHRONOUS_SUBMIT);

		String json = GenerateContours();
		System.out.println(rasterURL);

		GeoprocessingMultiValue strings = new GeoprocessingMultiValue(GeoprocessingParameter.Type.GEOPROCESSING_RASTER);
		strings.getValues().add(new GeoprocessingRaster(rasterURL, "Raster"));

		final Map<String, GeoprocessingParameter> inputs = gpParameters.getInputs();
		inputs.put("in_line_features", new GeoprocessingString(json));
		inputs.put("profile_targets", strings);

		// adds contour lines to map
		GeoprocessingJob gpJob = gpTask.createJob(gpParameters);

		gpJob.addProgressChangedListener(() -> {
			progressBar.setProgress(((double) gpJob.getProgress()) / 100);
		});

		gpJob.addJobDoneListener(() -> {
			if (gpJob.getStatus() == Job.Status.SUCCEEDED) {

				Map<String, GeoprocessingParameter> result = gpJob.getResult().getOutputs();
				GeoprocessingFeatures features = (GeoprocessingFeatures) result.get("out_table");

				LineChartController.series.getData().clear();
				
				features.getFeatures().forEach((e) -> {
					double FIRST_DIST = (double) e.getAttributes().get("FIRST_DIST") / 10;
					totalDist += FIRST_DIST;
					double FIRST_Z = (double) e.getAttributes().get("FIRST_Z");

					System.out.println(totalDist + ", " + FIRST_Z);
					
					LineChartController.series.getData().add(new XYChart.Data(totalDist, FIRST_Z));
				});

				try {
					FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LineChart.fxml"));
					Parent root = loader.load();
					Scene scene = new Scene(root);

					Stage stage = new Stage();
					stage.setTitle("剖面分析結果");
					stage.initOwner(_stage_main);
					stage.initModality(Modality.NONE);
					stage.setScene(scene);

					Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
					stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
					stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 2);

					stage.showAndWait();
					stage.setOnCloseRequest(event -> {

					});
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				btnGenerate.setDisable(true);
			} else {
				Alert dialog = new Alert(AlertType.ERROR);
				dialog.setHeaderText("Geoprocess Job Fail");
				dialog.setContentText("Error: " + gpJob.getError().getAdditionalMessage());
				dialog.showAndWait();
			}
			progressBar.setVisible(false);
		});
		gpJob.start();

	}

	/**
	 * Removes contour lines from map if any are applied.
	 */
	@FXML
	protected void handleClearResults() {
		graphicsOverlayDraw3D.getGraphics().clear();
		btnGenerate.setDisable(false);
	}

	private String GenerateContours() {
		String jsonGeometry = "";

		String jsonFile = new File(System.getProperty("data.dir"), "./samples-data/Polyline_FeaturesToJSON.json")
				.getAbsolutePath();

		BufferedReader br = null;
		try {
			String jsonString = "";
			br = new BufferedReader(new FileReader(jsonFile));

			String line = br.readLine();

			while (line != null) {
				jsonString += line;
				line = br.readLine();
			}

			jsonGeometry = "{\"hasZ\": false, \"paths\":[[";
			for (Point p : lineBuilder.getParts().getPartsAsPoints()) {
				jsonGeometry += "[" + p.getX() + "," + p.getY() + "],";
			}

			jsonGeometry = jsonGeometry.substring(0, jsonGeometry.length() - 1) + "]],\n"
					+ "				\"spatialReference\": {\n" + "					\"wkid\": 4326\n"
					+ "				}   }";

			jsonGeometry = jsonString.replace("%", jsonGeometry);

			System.out.println(jsonGeometry);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return jsonGeometry;
	}

	private void ShapefileEdit(Geometry editGeometry) {
		// create a shapefile feature table from the local file
		File shapefile = new File(System.getProperty("data.dir"), "./samples-data/stackprofile/Polylines_12.shp");
		ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(shapefile.getAbsolutePath());

		// use the shapefile feature table to create a feature layer
		FeatureLayer featureLayer = new FeatureLayer(shapefileFeatureTable);
		featureLayer.addDoneLoadingListener(() -> {
			if (featureLayer.getLoadStatus() == LoadStatus.LOADED) {
				// zoom to the area containing the layer's features
				QueryParameters queryParameters = new QueryParameters();
				queryParameters.setWhereClause("1=1");
				ListenableFuture<FeatureQueryResult> queryResult = featureLayer.getFeatureTable()
						.queryFeaturesAsync(queryParameters);
				try {
					FeatureQueryResult fqr = queryResult.get();
					fqr.forEach((feature) -> {
						feature.setGeometry(editGeometry);
						shapefileFeatureTable.updateFeatureAsync(feature);
						return;
					});
					shapefileFeatureTable.close();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR, featureLayer.getLoadError().getMessage());
				alert.show();
			}
		});

		shapefileFeatureTable.loadAsync();
	}

	private void showMessage(String title, String message) {

		Platform.runLater(() -> {
			Alert dialog = new Alert(AlertType.INFORMATION);
			dialog.initOwner(sceneView.getScene().getWindow());
			dialog.setHeaderText(title);
			dialog.setContentText(message);
			dialog.showAndWait();

			Platform.exit();
		});
	}

	/**
	 * Stops and releases all resources used in application.
	 */
	void terminate() {

		if (server != null)
			server.stopAsync();
		
		if (sceneView != null) {
			sceneView.dispose();
		}
	}
}
