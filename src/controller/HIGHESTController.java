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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.PolygonBuilder;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
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
import javafx.stage.Stage;

public class HIGHESTController {

	@FXML
	private Button btnGenerate;
	@FXML
	private Button btnClear;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private SceneView sceneView;
	private ArcGISScene arcgisScene;

	public GeoprocessingTask gpTask;
	private LocalGeoprocessingService localGPService;
	private GraphicsOverlay graphicsOverlay;
	private PolygonBuilder areaBuilder;
	private SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFFF0000,
			10);
	private SimpleMarkerSymbol yellowCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFF00FF00,
			10);
	private String rasterURL;

	public static LocalServer server;
	private Polygon polygon;
	/**
	 * Called after FXML loads. Sets up scene and map and configures property
	 * bindings.
	 */
	public void initialize(String rasterURL, ProgressBar progressBar, GraphicsOverlay graphicsOverlay) {
		this.rasterURL = rasterURL;
		this.progressBar = progressBar;
		this.graphicsOverlay = graphicsOverlay;
//		ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000528559,none,1JHJH7E3CZ4HH6JRP132");
//		ArcGISRuntimeEnvironment.setLicense("runtimeanalysis,1000,rud000012252,none,YYPH5AZAM8J8AZJTR058");

		try {

			// check that local server install path can be accessed
			if (LocalServer.INSTANCE.checkInstallValid()) {
				progressBar.setVisible(true);
				server = LocalServer.INSTANCE;
				// start the local server
				server.addStatusChangedListener(status -> {
					if (server.getStatus() == LocalServerStatus.STARTED) {
						try {
							String gpServiceURL = new File(System.getProperty("data.dir"),
									"./samples-data/local_server/HIGHEST_V1.gpkx").getAbsolutePath();
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
								gpTask = new GeoprocessingTask(localGPService.getUrl() + "/highest0419");
								System.out.println(localGPService.getUrl() + "/highest0419");
//								btnClear.disableProperty().bind(btnGenerate.disabledProperty().not());
//								btnGenerate.setDisable(false);
								progressBar.setVisible(false);
							}
						});
						localGPService.startAsync();
					} else if (server.getStatus() == LocalServerStatus.FAILED) {
						showMessage("Local Geoprocessing Load Error", "Local Geoprocessing Failed to load.");
					}
				});

//				LocalServer.INSTANCE.setTempDataPath("C:\\TMP");
//				server.startAsync();
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
	public void DoHighest(Polygon polygon) throws IOException {
		
		this.polygon = polygon; // pb.toGeometry();
		
		String json = Polygon_FeaturesToJSON();

		// tracking progress of creating contour map
		progressBar.setVisible(true);
		// create parameter using interval set
		GeoprocessingParameters gpParameters = new GeoprocessingParameters(
				GeoprocessingParameters.ExecutionType.ASYNCHRONOUS_SUBMIT);

		final Map<String, GeoprocessingParameter> inputs = gpParameters.getInputs();
		inputs.put("TaiwanDem_new_P_tif", new GeoprocessingString(rasterURL));
		inputs.put("Polygons_shp", new GeoprocessingString(json));

		System.out.println(json);

		// adds contour lines to map
		GeoprocessingJob gpJob = gpTask.createJob(gpParameters);

		gpJob.addProgressChangedListener(() -> {
			progressBar.setProgress(((double) gpJob.getProgress()) / 100);
		});

		gpJob.addJobDoneListener(() -> {
			if (gpJob.getStatus() == Job.Status.SUCCEEDED) {
				String serviceUrl = localGPService.getUrl();
				
				//這是使用 Local Server Utility-> Runtime Setting -> Enable 'logging' -> 觀察到的MapServer URL(執行結果)
				String mapServerUrl = serviceUrl.replace("GPServer", "MapServer/jobs/" + gpJob.getServerJobId())
						+ "/0/query?objectIds=&where=1%3D1&time=&geometry=&geometryType=esriGeometryEnvelope&inSR=&spatialRel=esriSpatialRelIntersects&relationParam=&outFields=Shape&returnGeometry=true&outSR=&returnIdsOnly=false&f=json";
				try {
					JSONObject result = readJsonFromUrl(mapServerUrl);
					JSONArray a = (JSONArray)result.get("features");
					JSONObject b = (JSONObject) a.get(0);
					JSONObject c = (JSONObject)b.get("geometry");
					Point p = new Point(c.getDouble("x"),c.getDouble("y"), SpatialReferences.getWebMercator());
					Point p1 = (Point) GeometryEngine.project(p, SpatialReferences.getWgs84());
					
					Graphic drawingGraphic = new Graphic(p1, yellowCircleSymbol);
					graphicsOverlay.getGraphics().add(drawingGraphic);
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

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

	private String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	/**
	 * Removes contour lines from map if any are applied.
	 */
	@FXML
	protected void handleClearResults() {
		graphicsOverlay.getGraphics().clear();
		btnGenerate.setDisable(false);
	}

	private String Polygon_FeaturesToJSON() {
		String jsonGeometry = "";

		String jsonFile = new File(System.getProperty("data.dir"), "./samples-data/Polygons_FeaturesToJSON.json")
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

			jsonGeometry = "{\"rings\":[[";
			for (Point p : polygon.getParts().getPartsAsPoints()) {
				jsonGeometry += "[" + p.getX() + "," + p.getY() + "],";
			}

			jsonGeometry = jsonGeometry.substring(0, jsonGeometry.length() - 1) + "]]}";

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
