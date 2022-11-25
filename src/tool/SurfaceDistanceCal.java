package tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService;
import com.esri.arcgisruntime.localserver.LocalGeoprocessingService.ServiceType;
import com.esri.arcgisruntime.localserver.LocalServer;
import com.esri.arcgisruntime.localserver.LocalServerStatus;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingFeatures;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingJob;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingMultiValue;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameter;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingParameters;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingRaster;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingString;
import com.esri.arcgisruntime.tasks.geoprocessing.GeoprocessingTask;

import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;

public class SurfaceDistanceCal {
	@FunctionalInterface
	public interface CaculateFinishedEventHandler {
		void invoke(String distance);
	}

	/** 威脅情資更新通知 */
	public static Event<CaculateFinishedEventHandler> CaculateFinishedEvent = new Event<CaculateFinishedEventHandler>();

	private void RaiseCaculateFinishedEvent(String distance) {
		// invoke all listeners:
		for (CaculateFinishedEventHandler listener : CaculateFinishedEvent.listeners()) {
			listener.invoke(distance);
		}
	}
	
	private ProgressBar progressBar;
	private GeoprocessingTask gpTask;
	private LocalGeoprocessingService localGPService;
	private String rasterURL;
	private Polyline polyline;
	private static LocalServer server;
	private static double totalDist = 0.0;
	
	public SurfaceDistanceCal(String rasterURL, Polyline polyline, ProgressBar progressBar) {
		this.rasterURL = rasterURL;
		this.polyline = polyline;
		this.progressBar = progressBar;
		
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
							try {
								CalcuteDistance();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
//							progressBar.setVisible(false);
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
			
			server.startAsync();
		}
	}
	
	private void CalcuteDistance() throws IOException {

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

				ArrayList<Feature> lsFeatures = new ArrayList<Feature>();
				features.getFeatures().forEach((e) -> {
					lsFeatures.add(e);
				});
				
				int i = 0;
				double distance = 0.0;
				while (i+1 < lsFeatures.size()) {
					Feature f1 = lsFeatures.get(i);
					Feature f2 = lsFeatures.get(i + 1);
					
					//底
					double w = ((double) f2.getAttributes().get("FIRST_DIST") -
							(double) f1.getAttributes().get("FIRST_DIST")) * 1000; 
					
					//公尺
					//高
					double h =  Math.abs((double) f2.getAttributes().get("FIRST_Z") -
								(double) f1.getAttributes().get("FIRST_Z"));
					
					System.out.println("w:" + w + ",h:" + h);
					
					double d1 = Math.sqrt(Math.pow(w, 2) + Math.pow(h, 2));
					distance += d1;
					i++;
				}

				RaiseCaculateFinishedEvent(String.format("%.2f", distance/ 1000.0));
				//System.out.println("地表距離:" + String.format("%.2f", distance/ 1000.0));
				if (server != null)
					server.stopAsync();
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
			for (Point p : polyline.getParts().getPartsAsPoints()) {
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

}
