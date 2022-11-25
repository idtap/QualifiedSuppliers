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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.RasterLayer;
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
import com.esri.arcgisruntime.raster.ColormapRenderer;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.symbology.ColorUtil;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingJob;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameter;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameters;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingRaster;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingString;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingTask;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ViewShedController {

	@FXML
	private Button btnGenerate;
	@FXML
	private Button btnClear;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private SceneView sceneView;
	private ArcGISScene arcgisScene;
	@FXML
	private StackPane mapPane;
	private GeoprocessingTask gpTask;
	private LocalGeoprocessingService localGPService;
	private GraphicsOverlay graphicsOverlayDraw3D;
	private Point pointLocation;
	private SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFFF0000,
			10);
	private ShapefileFeatureTable myShapefile;
	private FeatureLayer newFeatureLayer;

	private static LocalServer server;

	/**
	 * Called after FXML loads. Sets up scene and map and configures property
	 * bindings.
	 */
	public void initialize() {
//		ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000528559,none,1JHJH7E3CZ4HH6JRP132");
		String stand = "runtimestandard,1000,rud000433798,none,NKLFD4SZ8L2K8YAJM070";
		String analysis = "runtimeanalysis,1000,rud000012252,none,YYPH5AZAM8J8AZJTR058";

		List<String> ls = new ArrayList<String>();
		ls.add(analysis);

		ArcGISRuntimeEnvironment.setLicense(stand, ls);
		
		init1();
		try {
//			// authentication with an API key or named user is required to access basemaps
//			// and other location services
//			String yourAPIKey = "AAPKc09de8d4475749268343720606d7ac25ExYd7IKhiGdeC0PMB-2Y_W4QxDJQpkc2-GJiuUxSex-NDOcU9YQnwUBcSLntlcYE";
//			ArcGISRuntimeEnvironment.setApiKey(yourAPIKey);

			// create a map with the light gray basemap style
			arcgisScene = new ArcGISScene(Basemap.createImagery());

			// set the map to the map view
			// set the map to be displayed in this view
			sceneView = new SceneView();
			sceneView.setAttributionTextVisible(false);
			sceneView.setArcGISScene(arcgisScene);
			mapPane.getChildren().add(sceneView);
			
			TileCache tileCache_Raster3D = new TileCache("C:\\test\\map\\raster-L11.tpk");
			ArcGISTiledLayer tiledLayer_Raster3D = new ArcGISTiledLayer(tileCache_Raster3D);
			Basemap basemap_Raster3D = new Basemap(tiledLayer_Raster3D);

			arcgisScene.setBasemap(basemap_Raster3D);
			
			// load tiled layer and zoom to location
//			String rasterURL =  new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
//					.getAbsolutePath();
//			Raster raster = new Raster(rasterURL);
//			RasterLayer myRasterLayer = new RasterLayer(raster);
//			myRasterLayer.setVisible(false);
//			arcgisScene.getOperationalLayers().add(myRasterLayer);


//			TileCache tileCache_Raster3D = new TileCache("C:\\test\\map" + "/raster-L11.tpk");
//			ArcGISTiledLayer tiledLayer_Raster3D = new ArcGISTiledLayer(tileCache_Raster3D);
//			Basemap basemap_Raster3D = new Basemap(tiledLayer_Raster3D);
//
//			arcgisScene.setBasemap(basemap_Raster3D);
			
			// 高程離線圖資
			String url1 = new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
					.getAbsolutePath();
//			String url2 = new File(System.getProperty("data.dir"), "samples-data/raster-file/Shasta_Elevation.tif")
//					.getAbsolutePath();
//			String url3 = new File(System.getProperty("data.dir"), "samples-data/dsm/DSM_n5600w15915P_1984.tif")
//					.getAbsolutePath();

			ArrayList<String> list = new ArrayList<String>();
			list.add(url1);
//			list.add(url2);
//			list.add(url3);

			Surface surface = new Surface();
			surface.getElevationSources().add(new RasterElevationSource(list));
			surface.setElevationExaggeration(1);
			arcgisScene.setBaseSurface(surface);

			Camera camera = new Camera(23.763, 121.163, 20000, 0, 10, 0);
			sceneView.setViewpointCamera(camera);

			graphicsOverlayDraw3D = new GraphicsOverlay();
			sceneView.getGraphicsOverlays().add(graphicsOverlayDraw3D);

			sceneView.setOnMouseClicked(e -> {
				if (e.isStillSincePress()) {
					if (e.getButton() == MouseButton.PRIMARY) {
						Point2D screenPoint = new Point2D(e.getX(), e.getY());

						// 開始畫軍隊符號
						ListenableFuture<Point> mapPoint = sceneView.screenToLocationAsync(screenPoint);

						mapPoint.addDoneListener(() -> {
							try {
								graphicsOverlayDraw3D.getGraphics().clear();

								pointLocation = mapPoint.get();
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
									"./samples-data/local_server/Visibility.gpkx").getAbsolutePath();
							// need map server result to add contour lines to map
							localGPService = new LocalGeoprocessingService(gpServiceURL,
									ServiceType.ASYNCHRONOUS_SUBMIT_WITH_MAP_SERVER_RESULT);
						} catch (Exception e) {
							e.printStackTrace();
						}

						localGPService.addStatusChangedListener(s -> {
							// create geoprocessing task once local geoprocessing service is started
							if (s.getNewStatus() == LocalServerStatus.STARTED) {
								// add `/Contour` to use contour geoprocessing tool
								gpTask = new GeoprocessingTask(localGPService.getUrl() + "/Visibility");
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

//				LocalServer.INSTANCE.setTempDataPath("C:\\TMP");
				server.startAsync();
			} else {
				showMessage("Local Server Load Error", "Local Server install path couldn't be located.");
			}

		} catch (Exception e) {
			// on any exception, print the stack trace
			e.printStackTrace();
		}

	}

	private void init1() {
		// CopyFiles(new File("C:\\TMP\\Viewshed2"), new File("C:\\TMP\\Viewshed"));

		// create a shapefile feature table from the local file
		File shapefile = new File("C:\\TMP\\Viewshed\\Points.shp");
		myShapefile = new ShapefileFeatureTable(shapefile.getAbsolutePath());

		// use the shapefile feature table to create a feature layer
		newFeatureLayer = new FeatureLayer(myShapefile);
		myShapefile.loadAsync();
		newFeatureLayer.addDoneLoadingListener(() -> {
			if (newFeatureLayer.getLoadStatus() == LoadStatus.LOADED) {
				// zoom to the area containing the layer's features
				System.out.println("LoadStatus.LOADED");
			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR, newFeatureLayer.getLoadError().getMessage());
				alert.show();
			}
		});

	}

	/**
	 * Creates a Map Image Layer that displays contour lines on the map using the
	 * interval the that is set.
	 */
	@FXML
	protected void handleGenerateContours() {

		QueryParameters queryParameters = new QueryParameters();
		queryParameters.setWhereClause("1=1");

		ListenableFuture<FeatureQueryResult> queryResult = newFeatureLayer.getFeatureTable()
				.queryFeaturesAsync(queryParameters);

		FeatureQueryResult f;
		try {
			f = queryResult.get();
			f.forEach((e) -> {
				e.setGeometry(pointLocation);
				myShapefile.updateFeatureAsync(e);
			});

			doViewShed();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doViewShed() {
		// tracking progress of creating contour map
		progressBar.setVisible(true);
		// create parameter using interval set
		GeoprocessingParameters gpParameters = new GeoprocessingParameters(
				GeoprocessingParameters.ExecutionType.ASYNCHRONOUS_SUBMIT);
	
		String rasterURL = new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
				.getAbsolutePath();

		final Map<String, GeoprocessingParameter> inputs = gpParameters.getInputs();
		inputs.put("in_raster", new GeoprocessingRaster(rasterURL, "Raster"));

		File shapefile = new File("C:\\TMP\\Viewshed\\Points.shp");
		inputs.put("in_observer_features", new GeoprocessingString(shapefile.getAbsolutePath()));
		inputs.put("inner_radius", new GeoprocessingString("0"));
		inputs.put("outer_radius", new GeoprocessingString("2000"));
		
		// adds contour lines to map
		GeoprocessingJob gpJob = gpTask.createJob(gpParameters);
		
		gpJob.addProgressChangedListener(() -> {
			progressBar.setProgress(((double) gpJob.getProgress()) / 100);
		});

		gpJob.addJobDoneListener(() -> {
			if (gpJob.getStatus() == Job.Status.SUCCEEDED) {
				Map<String, GeoprocessingParameter> result = gpJob.getResult().getOutputs();
				GeoprocessingRaster r = (GeoprocessingRaster) result.get("out_raster");

				System.out.println(r.getUrl());

				File file = new File(r.getUrl());
				if (file.exists())
					file.delete();

				String tmpTif = new File(System.getProperty("data.dir"), "./samples-data/test.tif").getAbsolutePath();

				try (BufferedInputStream in = new BufferedInputStream(new URL(r.getUrl()).openStream());
						FileOutputStream fileOutputStream = new FileOutputStream(tmpTif)) {
					byte dataBuffer[] = new byte[1024];
					int bytesRead;
					while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
						fileOutputStream.write(dataBuffer, 0, bytesRead);
					}

					// Load the raster file
					Raster myRasterFile = new Raster(tmpTif);

					// Create a color map where values 0-149 are red and 150-249 are yellow.
					List<Integer> colors = new ArrayList<Integer>();
					colors.add(ColorUtil.colorToArgb(Color.TRANSPARENT));
					colors.add(ColorUtil.colorToArgb(Color.ORANGE));
					
					// Create a colormap renderer.
					ColormapRenderer colormapRenderer = new ColormapRenderer(colors);

					// Create the layer
					RasterLayer myRasterLayer = new RasterLayer(myRasterFile);

					myRasterLayer.setOpacity(0.5f);
					// Set the colormap renderer on the raster layer.
					myRasterLayer.setRasterRenderer(colormapRenderer);

					// Add the layer to the map
					arcgisScene.getOperationalLayers().add(myRasterLayer);

				} catch (IOException e) {
					// handle exception
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

		if (arcgisScene.getOperationalLayers().size() > 1) {
			arcgisScene.getOperationalLayers().remove(1);
			btnGenerate.setDisable(false);
		}
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
