package tool;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService.ServiceType;
import com.esri.arcgisruntime.raster.MinMaxStretchParameters;
import com.esri.arcgisruntime.raster.PercentClipStretchParameters;
import com.esri.arcgisruntime.raster.RGBRenderer;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.raster.StandardDeviationStretchParameters;
import com.esri.arcgisruntime.raster.StretchParameters;
import com.esri.arcgisruntime.raster.StretchRenderer;
import com.esri.arcgisruntime.localserver.LocalServer;
import com.esri.arcgisruntime.localserver.LocalServerStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingFeatures;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingJob;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingMultiValue;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameter;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameters;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingRaster;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingString;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingTask;

import controller.LineChartController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Slope {

	private ProgressBar progressBar;
	public GeoprocessingTask gpTask;
	private LocalGeoprocessingService localGPService;
	private ArcGISScene arcGISScene;
	public RasterLayer myRasterLayer;
	private String rasterURL;
	private static LocalServer server;
	private Stage _stage_main;
	public static double surfaceDistance = 0.0;
	
	public Slope(String rasterURL, ProgressBar progressBar, Stage _stage_main, ArcGISScene arcGISScene) {
		this.rasterURL = rasterURL;
		this.progressBar = progressBar;
		this._stage_main = _stage_main;
		this.arcGISScene = arcGISScene;
		
		// check that local server install path can be accessed
		if (LocalServer.INSTANCE.checkInstallValid()) {
			progressBar.setVisible(true);
			server = LocalServer.INSTANCE;
			// start the local server
			server.addStatusChangedListener(status -> {
				if (server.getStatus() == LocalServerStatus.STARTED) {
					try {
						String gpServiceURL = new File(System.getProperty("data.dir"),
								"./samples-data/local_server/ClipRaster.gpkx").getAbsolutePath();
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
							gpTask = new GeoprocessingTask(localGPService.getUrl() + "/Clip Raster");
							progressBar.setVisible(false);
						}
					});
					localGPService.startAsync();
				} else if (server.getStatus() == LocalServerStatus.FAILED) {
					Alert dialog = new Alert(AlertType.ERROR);
					dialog.setHeaderText("Geoprocess Job Fail");
					dialog.setContentText("Error: " + "Local Geoprocessing Failed to load.");
					dialog.showAndWait();
				}
			});
			
			//server.startAsync();
		}
	}
	
	public void DoSlope(Polygon polygonGraphic) throws IOException {
		polygonGraphic = (Polygon)GeometryEngine.project(polygonGraphic, SpatialReferences.getWebMercator());
		String strExtent = String.format("%.3f %.3f %.3f %.3f", polygonGraphic.getExtent().getXMin(), 
				polygonGraphic.getExtent().getYMin(), polygonGraphic.getExtent().getXMax(), polygonGraphic.getExtent().getYMax());
		if (myRasterLayer != null)
			arcGISScene.getOperationalLayers().remove(myRasterLayer);
		
		// tracking progress of creating contour map
		progressBar.setVisible(true);
		// create parameter using interval set
		GeoprocessingParameters gpParameters = new GeoprocessingParameters(
				GeoprocessingParameters.ExecutionType.ASYNCHRONOUS_SUBMIT);

		final Map<String, GeoprocessingParameter> inputs = gpParameters.getInputs();
		inputs.put("in_raster", new GeoprocessingRaster(rasterURL, "Raster"));
		inputs.put("rectangle", new GeoprocessingString(strExtent));

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
		                myRasterLayer = new RasterLayer(myRasterFile);

		             // create rgb renderer
		                StretchParameters stretchParameters = new StandardDeviationStretchParameters(1.0);
		                StretchRenderer stretchRenderer = new StretchRenderer(stretchParameters, null, true, null);
		                myRasterLayer.setRasterRenderer(stretchRenderer);
		                
		                // Add the layer to the map
		                arcGISScene.getOperationalLayers().add(myRasterLayer);
						
					} catch (IOException e) {
						// handle exception
					}
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
}
