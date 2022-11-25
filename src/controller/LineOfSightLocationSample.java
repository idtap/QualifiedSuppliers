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
package controller;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geoanalysis.LocationLineOfSight;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.AnalysisOverlay;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.SceneView;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LineOfSightLocationSample {

	private SceneView sceneView;

	public void start(Stage _stage_main) {

		try {

			// create stack pane and JavaFX app scene
			StackPane stackPane = new StackPane();
			Scene fxScene = new Scene(stackPane);
			fxScene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

			// set title, size, and add JavaFX scene to stage
			Stage stage = new Stage();
			stage.setTitle("通視分析");
			stage.setWidth(1024);
			stage.setHeight(768);
			stage.setScene(fxScene);
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(_stage_main);
			stage.show();

			stage.setOnCloseRequest(event -> {
				terminate();
			});
			
			TileCache tileCache_Raster = new TileCache("C:/test/World.tpk");
			ArcGISTiledLayer tiledLayer_Raster = new ArcGISTiledLayer(tileCache_Raster);
			Basemap basemap_Raster = new Basemap(tiledLayer_Raster);
			ArcGISScene arcgisScene = new ArcGISScene(basemap_Raster);

			sceneView = new SceneView();
			// set the map to the map view
			sceneView.setArcGISScene(arcgisScene);
			stackPane.getChildren().add(sceneView);

			// 高程離線圖資
			String rasterURL = new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
					.getAbsolutePath();
			ArrayList<String> list = new ArrayList<String>();
			list.add(rasterURL);

			Surface surface = new Surface();
			surface.getElevationSources().add(new RasterElevationSource(list));
			surface.setElevationExaggeration(1);
			arcgisScene.setBaseSurface(surface);

			// create an analysis overlay for the line of sight
			AnalysisOverlay analysisOverlay2 = new AnalysisOverlay();
			sceneView.getAnalysisOverlays().add(analysisOverlay2);

			// initialize a line of sight analysis and add it to the analysis overlay
			Point observerLocation = new Point(121, 23, 2000,
					SpatialReferences.getWgs84());
			Point targetLocation = new Point(121, 22.94, 1312,
					SpatialReferences.getWgs84());
			LocationLineOfSight lineOfSight = new LocationLineOfSight(observerLocation, targetLocation);
			analysisOverlay2.getAnalyses().add(lineOfSight);

			// initialize the viewpoint
			Camera camera = new Camera(new Point(121.02, 22.94, 3059, SpatialReferences.getWgs84()), 11, 62, 0);
			sceneView.setViewpointCamera(camera);

			// remove default mouse move handler
			sceneView.setOnMouseMoved(null);

			// update the target location when the mouse moves
			EventHandler<MouseEvent> mouseMoveEventHandler = event -> {
				Point2D point2D = new Point2D(event.getX(), event.getY());
				// get the scene location from the screen position
				ListenableFuture<Point> pointFuture = sceneView.screenToLocationAsync(point2D);
				pointFuture.addDoneListener(() -> {
					try {
						Point point = pointFuture.get();
						// update the target location
						lineOfSight.setTargetLocation(point);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
			};

			// mouse click to start/stop moving target location
			sceneView.setOnMouseClicked(event -> {
				if (event.isStillSincePress() && event.getButton() == MouseButton.PRIMARY) {
					if (sceneView.getOnMouseMoved() == null) {
						sceneView.setOnMouseMoved(mouseMoveEventHandler);
					} else {
						sceneView.setOnMouseMoved(null);
					}
				}
			});

		} catch (Exception e) {
			// on any error, display the stack trace.
			e.printStackTrace();
		}
	}

	/**
	 * Stops and releases all resources used in application.
	 */
	void terminate() {

		if (sceneView != null) {
			sceneView.dispose();
		}
	}

}
