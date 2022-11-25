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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService.ServiceType;
import com.esri.arcgisruntime.localserver.LocalServer;
import com.esri.arcgisruntime.localserver.LocalServerStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingJob;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameter;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameters;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingRaster;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingString;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingTask;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class SlopeController {

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

	private static LocalServer server;

	/**
	 * Called after FXML loads. Sets up scene and map and configures property
	 * bindings.
	 */
	public void initialize() {
//		ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000528559,none,1JHJH7E3CZ4HH6JRP132");

		try {
			// authentication with an API key or named user is required to access basemaps
			// and other location services
//			String yourAPIKey = "AAPKc09de8d4475749268343720606d7ac25ExYd7IKhiGdeC0PMB-2Y_W4QxDJQpkc2-GJiuUxSex-NDOcU9YQnwUBcSLntlcYE";
//			ArcGISRuntimeEnvironment.setApiKey(yourAPIKey);

			TileCache tileCache_Raster = new TileCache("C:/test/World.tpk");
			ArcGISTiledLayer tiledLayer_Raster = new ArcGISTiledLayer(tileCache_Raster);
			Basemap basemap_Raster = new Basemap(tiledLayer_Raster);
			arcgisScene = new ArcGISScene(basemap_Raster);
			
			// set the map to the map view
			sceneView.setArcGISScene(arcgisScene);

			// load tiled layer and zoom to location
			String rasterURL = new File(System.getProperty("data.dir"), "./samples-data/raster-file/Shasta.tif")
					.getAbsolutePath();
			Raster raster = new Raster(rasterURL);
			RasterLayer myRasterLayer = new RasterLayer(raster);
			arcgisScene.getOperationalLayers().add(myRasterLayer);
			
			// 高程離線圖資
			ArrayList<String> list = new ArrayList<String>();
			list.add(new File(System.getProperty("data.dir"), "./samples-data/raster-file/Shasta_Elevation.tif")
					.getAbsolutePath());

			Surface surface = new Surface();
			surface.getElevationSources().add(new RasterElevationSource(list));
			surface.setElevationExaggeration(1);
			arcgisScene.setBaseSurface(surface);


			Camera camera = new Camera(40.763, -122.163, 7000, 0, 10, 0);
			sceneView.setViewpointCamera(camera);

			// check that local server install path can be accessed
			if (LocalServer.INSTANCE.checkInstallValid()) {
				progressBar.setVisible(true);
				server = LocalServer.INSTANCE;
				// start the local server
				server.addStatusChangedListener(status -> {
					if (server.getStatus() == LocalServerStatus.STARTED) {
						try {
//							String gpServiceURL = new File(System.getProperty("data.dir"),
//									"./samples-data/local_server/Slope.gpkx").getAbsolutePath();
							String gpServiceURL = new File(System.getProperty("data.dir"),
									"./samples-data/local_server/ClipRaster.gpkx").getAbsolutePath();
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
//								gpTask = new GeoprocessingTask(localGPService.getUrl() + "/Slope");
								gpTask = new GeoprocessingTask(localGPService.getUrl() + "/Clip Raster");
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

	/**
	 * Creates a Map Image Layer that displays contour lines on the map using the
	 * interval the that is set.
	 */
	@FXML
	protected void handleGenerateContours() {

		// tracking progress of creating contour map
		progressBar.setVisible(true);
		// create parameter using interval set
		GeoprocessingParameters gpParameters = new GeoprocessingParameters(
				GeoprocessingParameters.ExecutionType.ASYNCHRONOUS_SUBMIT);

		String rasterURL = new File(System.getProperty("data.dir"), "./samples-data/raster-file/Slope_Taiwan1.tif")
				.getAbsolutePath();

		final Map<String, GeoprocessingParameter> inputs = gpParameters.getInputs();
		inputs.put("in_raster", new GeoprocessingRaster(rasterURL, "Raster"));
		inputs.put("rectangle", new GeoprocessingString("13457221.4808812 2595905.57478484 13502241.4808812 2715665.57478484"));
		
		// adds contour lines to map
		GeoprocessingJob gpJob = gpTask.createJob(gpParameters);

		gpJob.addProgressChangedListener(() -> {
			progressBar.setProgress(((double) gpJob.getProgress()) / 100);
		});

		gpJob.addJobDoneListener(() -> {
			if (gpJob.getStatus() == Job.Status.SUCCEEDED) {
				Map<String, GeoprocessingParameter> result = gpJob.getResult().getOutputs();
				for (Object value : result.values()) {
					GeoprocessingRaster r = (GeoprocessingRaster) value;
					System.out.println(r.getUrl());
					
					File file = new File(r.getUrl());
					if (file.exists())
						file.delete();
					
					String tmpTif = new File(System.getProperty("data.dir"), "./samples-data/test.tif")
							.getAbsolutePath();
					
					try (BufferedInputStream in = new BufferedInputStream(new URL(r.getUrl()).openStream());
							FileOutputStream fileOutputStream = new FileOutputStream(tmpTif)) {
						byte dataBuffer[] = new byte[1024];
						int bytesRead;
						while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
							fileOutputStream.write(dataBuffer, 0, bytesRead);
						}
						
						// Load the raster file
		                Raster myRasterFile = new Raster(tmpTif);

		                // Create the layer
		                RasterLayer myRasterLayer = new RasterLayer(myRasterFile);

		                // Add the layer to the map
		                arcgisScene.getOperationalLayers().add(myRasterLayer);
						
					} catch (IOException e) {
						// handle exception
					}
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
