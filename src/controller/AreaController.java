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
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

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
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class AreaController implements Initializable {

	@FXML
	private Button btnGenerate;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private SceneView sceneView;
	private ArcGISScene arcgisScene;

	private ArcGISTiledLayer tiledLayer; // keep loadable in scope to avoid garbage collection
	private GeoprocessingTask gpTask;
	private LocalGeoprocessingService localGPService;

	private static LocalServer server;

	/**
	 * Called after FXML loads. Sets up scene and map and configures property
	 * bindings.
	 */
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
//		ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000528559,none,1JHJH7E3CZ4HH6JRP132");
		
		try {

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
			Camera camera = new Camera(40.761, -122.163, 3900, 0, 10, 0);
			sceneView.setViewpointCamera(camera);

			// check that local server install path can be accessed
			if (LocalServer.INSTANCE.checkInstallValid()) {
				progressBar.setVisible(true);
				server = LocalServer.INSTANCE;
				// start the local server
				server.addStatusChangedListener(status -> {
					if (server.getStatus() == LocalServerStatus.STARTED) {
						try {
							String gpServiceURL = new File(System.getProperty("data.dir"),
									"./samples-data/local_server/SurfaceVolume.gpkx").getAbsolutePath();
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
								gpTask = new GeoprocessingTask(localGPService.getUrl() + "/Surface%20Volume");
								progressBar.setVisible(false);
								btnGenerate.setDisable(false);
							}
						});
						localGPService.startAsync();
					} else if (server.getStatus() == LocalServerStatus.FAILED) {
						showMessage("Local Geoprocessing Load Error", "Local Geoprocessing Failed to load.");
					}
				});
				
				LocalServer.INSTANCE.setTempDataPath("C:\\TMP");
				//server.startAsync();
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

		String rasterURL = new File(System.getProperty("data.dir"), "samples-data/raster-file/Shasta_Elevation.tif").getAbsolutePath();

		final Map<String, GeoprocessingParameter> inputs = gpParameters.getInputs();
		inputs.put("in_surface", new GeoprocessingString(rasterURL));
		inputs.put("out_text_file", new GeoprocessingString("SF.txt"));
		

		// adds contour lines to map
		GeoprocessingJob gpJob = gpTask.createJob(gpParameters);

		gpJob.addProgressChangedListener(() -> {
			progressBar.setProgress(((double) gpJob.getProgress()) / 100);
		});

		gpJob.addJobDoneListener(() -> {
			if (gpJob.getStatus() == Job.Status.SUCCEEDED) {
				// creating map image url from local geoprocessing service url
				
				File found = searchFile(new File(LocalServer.INSTANCE.getTempDataPath()), "surfaceVolume.txt");
				
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(found.getAbsoluteFile()));
					String line = br.readLine();

					while (line != null) {
						line = br.readLine();
		                String[] values = line.split(",");
		                String msg = "";
		                
		                double area2D = Double.parseDouble(values[4]) / 1000000;
		                double area3D = Double.parseDouble(values[5]) / 1000000;
		                double volume = Double.parseDouble(values[6]) / 10000000;
		                
		                msg += "平面面積:" + String.format("%.2f",area2D < 0 ? area2D * -1 : area2D) + "平方公里\n";
		                msg += "地表面積:" + String.format("%.2f",area3D < 0 ? area3D * -1 : area3D) + "平方公里\n";
		                msg += "體積:" + String.format("%.2f",volume < 0 ? volume * -1 : volume) + "立方公里\n";

		                Alert dialog = new Alert(AlertType.INFORMATION);
						dialog.setHeaderText("分析結果");
						dialog.setContentText(msg);
						dialog.showAndWait();
						
						break;
					}
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

	private File searchFile(File file, String search) {
	    if (file.isDirectory()) {
	        File[] arr = file.listFiles();
	        for (File f : arr) {
	            File found = searchFile(f, search);
	            if (found != null)
	                return found;
	        }
	    } else {
	        if (file.getName().equals(search)) {
	            return file;
	        }
	    }
	    return null;
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
