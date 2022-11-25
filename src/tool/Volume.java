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
import controller.mainController;
import controller.mainController.ApState;
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

public class Volume {

	private ProgressBar progressBar;
	public GeoprocessingTask gpTask;
	public GeoprocessingTask gpTask2;
	private LocalGeoprocessingService localGPService;
	private LocalGeoprocessingService localGPService2;
	public RasterLayer myRasterLayer;
	private String rasterURL;
	public static LocalServer server;
	private Stage _stage_main;
	public static double surfaceDistance = 0.0;
	private String tmpTif;
	
	public Volume(String rasterURL, ProgressBar progressBar, Stage _stage_main) {
		this.rasterURL = rasterURL;
		this.progressBar = progressBar;
		this._stage_main = _stage_main;
		
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
	
	public void Init() {
		
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
						localGPService2 = new LocalGeoprocessingService(gpServiceURL,
								ServiceType.ASYNCHRONOUS_SUBMIT);
					} catch (Exception e) {
						e.printStackTrace();
					}

					localGPService2.addStatusChangedListener(s -> {
						// create geoprocessing task once local geoprocessing service is started
						if (s.getNewStatus() == LocalServerStatus.STARTED) {
							// add `/Contour` to use contour geoprocessing tool
							gpTask2 = new GeoprocessingTask(localGPService2.getUrl() + "/Surface%20Volume");
							progressBar.setVisible(false);

						}
					});
					localGPService2.startAsync();
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

	private double runtimeArea = 0.0;
	private double rate = 0.0;
	
	public void DoVolume(Polygon polygonGraphic) throws IOException {
		
		polygonGraphic = (Polygon)GeometryEngine.project(polygonGraphic, SpatialReferences.getWebMercator());
		
		runtimeArea = -1 * GeometryEngine.area(polygonGraphic) / 1000000;
		
		String strExtent = String.format("%.3f %.3f %.3f %.3f", polygonGraphic.getExtent().getXMin(), 
				polygonGraphic.getExtent().getYMin(), polygonGraphic.getExtent().getXMax(), polygonGraphic.getExtent().getYMax());
		
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
				if (mainController.apState != mainController.ApState.Volume)
					return;
				
				Map<String, GeoprocessingParameter> result = gpJob.getResult().getOutputs();
				for (Object value : result.values()) {
					GeoprocessingRaster r = (GeoprocessingRaster) value;
					System.out.println(r.getUrl());
					
					File file = new File(r.getUrl());
					if (file.exists())
						file.delete();
					
					tmpTif = new File(System.getProperty("data.dir"), "./samples-data/test.tif")
							.getAbsolutePath();
					
					try (BufferedInputStream in = new BufferedInputStream(new URL(r.getUrl()).openStream());
							FileOutputStream fileOutputStream = new FileOutputStream(tmpTif)) {
						byte dataBuffer[] = new byte[1024];
						int bytesRead;
						while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
							fileOutputStream.write(dataBuffer, 0, bytesRead);
						}
						
						//SecondInit(tmpTif);
						
						DoVolume2(tmpTif);
					} catch (IOException e) {
						e.printStackTrace();
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

	private void DoVolume2(String tmpTif) {
		
			deleteFile(new File(LocalServer.INSTANCE.getTempDataPath()), "surfaceVolume.txt");
		
			// tracking progress of creating contour map
			progressBar.setVisible(true);
			// create parameter using interval set
			GeoprocessingParameters gpParameters = new GeoprocessingParameters(
					GeoprocessingParameters.ExecutionType.ASYNCHRONOUS_SUBMIT);

			final Map<String, GeoprocessingParameter> inputs = gpParameters.getInputs();
			inputs.put("in_surface", new GeoprocessingString(tmpTif));
			inputs.put("out_text_file", new GeoprocessingString("SF.txt"));
			
			// adds contour lines to map
			GeoprocessingJob gpJob = gpTask2.createJob(gpParameters);

			gpJob.addProgressChangedListener(() -> {
				progressBar.setProgress(((double) gpJob.getProgress()) / 100);
			});

			gpJob.addJobDoneListener(() -> {
				if (gpJob.getStatus() == Job.Status.SUCCEEDED) {
					if (mainController.apState != mainController.ApState.Volume)
						return;
					
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
			                
			                rate = runtimeArea / area2D;
			                area3D = area3D * rate;
			                volume = volume * rate;
			                
			                msg += "平面面積:" + String.format("%.4f",runtimeArea < 0 ? runtimeArea * -1 : runtimeArea) + "平方公里\n";
			                msg += "地表面積:" + String.format("%.4f",area3D < 0 ? area3D * -1 : area3D) + "平方公里\n";
			                msg += "體積:" + String.format("%.4f",volume < 0 ? volume * -1 : volume) + "立方公里\n";

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

				} else {
					Alert dialog = new Alert(AlertType.ERROR);
					dialog.setHeaderText("Geoprocess Job Fail");
					dialog.setContentText("Error: " + gpJob.getError().getAdditionalMessage());
					dialog.showAndWait();
				}
				progressBar.setVisible(false);
				mainController.apState = ApState.normal;
			});
			gpJob.start();
	}
	
	private File searchFile(File path, String search) {
	    if (path.isDirectory()) {
	        File[] arr = path.listFiles();
	        for (File f : arr) {
	            File found = searchFile(f, search);
	            if (found != null)
	                return found;
	        }
	    } else {
	        if (path.getName().equals(search)) {
	            return path;
	        }
	    }
	    return null;
	}
	
	private File deleteFile(File path, String search) {
		try {
		    if (path.isDirectory()) {
		        File[] arr = path.listFiles();
		        for (File f : arr) {
		            File found = deleteFile(f, search);
		            if (found != null)
		            	found.delete();
		        }
		    } else {
		        if (path.getName().equals(search)) {
		            return path;
		        }
		    }
		    return null;
		} catch (Exception e) {
			return null;
		}
		
	}
}
