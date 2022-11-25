package controller;
/*
 * Copyright 2019 Esri.
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

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;

import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GetElevationAtAPointSample {

	private SceneView sceneView;
	private GraphicsOverlay graphicsOverlay;

	public void Start(Stage _stage_main) {
		try {
			// create stack pane and application scene
			StackPane stackPane = new StackPane();
			Scene fxScene = new Scene(stackPane);
			fxScene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

			// set title, size, and add JavaFX scene to stage
			Stage stage = new Stage();
			stage.setTitle("高度測量");
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

			// create a point symbol and graphic to mark where elevation is being measured
			SimpleLineSymbol elevationLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFF0000, 3.0f);
			Graphic polylineGraphic = new Graphic();
			polylineGraphic.setSymbol(elevationLineSymbol);

			// create a text symbol and graphic to display the elevation of selected points
			TextSymbol elevationTextSymbol = new TextSymbol(20, null, 0xFFFF0000, TextSymbol.HorizontalAlignment.CENTER,
					TextSymbol.VerticalAlignment.BOTTOM);
			Graphic elevationTextGraphic = new Graphic();
			elevationTextGraphic.setSymbol(elevationTextSymbol);

			// create a graphics overlay
			graphicsOverlay = new GraphicsOverlay(GraphicsOverlay.RenderingMode.DYNAMIC);
			graphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
			sceneView.getGraphicsOverlays().add(graphicsOverlay);

			// add a camera and initial camera position
			Camera camera = new Camera(23, 121, 50000, 0, 10, 0);
			sceneView.setViewpointCamera(camera);

			// create listener to handle clicks
			sceneView.setOnMouseClicked(event -> {
				if (event.getButton() == MouseButton.PRIMARY && event.isStillSincePress()) {
					// get the clicked screenPoint
					Point2D screenPoint = new Point2D(event.getX(), event.getY());
					// convert the screen point to a point on the surface
					Point relativeSurfacePoint = sceneView.screenToBaseSurface(screenPoint);

					// check that the point is on the surface
					if (relativeSurfacePoint != null) {

						// clear any existing graphics from the graphics overlay
						graphicsOverlay.getGraphics().clear();

						// construct a polyline to use as a marker
						PolylineBuilder polylineBuilder = new PolylineBuilder(
								relativeSurfacePoint.getSpatialReference());
						Point baseOfPolyline = new Point(relativeSurfacePoint.getX(), relativeSurfacePoint.getY(), 0);
						polylineBuilder.addPoint(baseOfPolyline);
						Point topOfPolyline = new Point(baseOfPolyline.getX(), baseOfPolyline.getY(), 750);
						polylineBuilder.addPoint(topOfPolyline);
						Polyline markerPolyline = polylineBuilder.toGeometry();
						polylineGraphic.setGeometry(markerPolyline);
						graphicsOverlay.getGraphics().add(polylineGraphic);

						// get the surface elevation at the surface point
						ListenableFuture<Double> elevationFuture = arcgisScene.getBaseSurface()
								.getElevationAsync(relativeSurfacePoint);
						elevationFuture.addDoneListener(() -> {
							try {
								// get the surface elevation
								Double elevation = elevationFuture.get();

								// update the text in the elevation marker
								elevationTextSymbol.setText(Math.round(elevation) + " 公尺");
								elevationTextGraphic.setGeometry(topOfPolyline);
								graphicsOverlay.getGraphics().add(elevationTextGraphic);

							} catch (InterruptedException | ExecutionException e) {
								new Alert(Alert.AlertType.ERROR, e.getCause().getMessage()).show();
							}
						});
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
