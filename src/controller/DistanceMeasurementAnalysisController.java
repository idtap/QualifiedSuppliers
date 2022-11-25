package controller;
/*
 * Copyright 2018 Esri.
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.esri.arcgisruntime.UnitSystem;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geoanalysis.LocationDistanceMeasurement;
import com.esri.arcgisruntime.geometry.Distance;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.AnalysisOverlay;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.SceneView;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class DistanceMeasurementAnalysisController {

  @FXML private SceneView sceneView;
  @FXML private Label directDistanceLabel;
  @FXML private Label verticalDistanceLabel;
  @FXML private Label horizontalDistanceLabel;
  @FXML private ComboBox<UnitSystem> unitSystemComboBox;

  private LocationDistanceMeasurement distanceMeasurement;

  public void initialize() {

	TileCache tileCache_Raster = new TileCache("C:/test/World.tpk");
	ArcGISTiledLayer tiledLayer_Raster = new ArcGISTiledLayer(tileCache_Raster);
	Basemap basemap_Raster = new Basemap(tiledLayer_Raster);
	ArcGISScene arcgisScene = new ArcGISScene(basemap_Raster);

	// set the map to the map view
	sceneView.setArcGISScene(arcgisScene);

	// 高程離線圖資
	String rasterURL = new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
			.getAbsolutePath();
	ArrayList<String> list = new ArrayList<String>();
	list.add(rasterURL);
  
	Surface surface = new Surface();
	surface.getElevationSources().add(new RasterElevationSource(list));
	surface.setElevationExaggeration(1);
	arcgisScene.setBaseSurface(surface); 
	  
	  
    // create an analysis overlay and add it to the scene view
    AnalysisOverlay analysisOverlay = new AnalysisOverlay();
    sceneView.getAnalysisOverlays().add(analysisOverlay);

    // initialize a distance measurement and add it to the analysis overlay
    Point start = new Point( 121.0, 23.0, 24.772694, SpatialReferences.getWgs84());
    Point end = new Point(121.0, 23.05, 58.501115, SpatialReferences.getWgs84());
    distanceMeasurement = new LocationDistanceMeasurement(start, end);
    analysisOverlay.getAnalyses().add(distanceMeasurement);

    // zoom to the initial measurement
	Camera camera = new Camera(start, 200.0, 0.0, 45.0, 0.0);
	sceneView.setViewpointCamera(camera); 
    

    // show the distances in the UI when the measurement changes
    DecimalFormat decimalFormat = new DecimalFormat("#.##");
	distanceMeasurement.addMeasurementChangedListener(e -> {
      Distance directDistance = e.getDirectDistance();
      Distance verticalDistance = e.getVerticalDistance();
      Distance horizontalDistance = e.getHorizontalDistance();
      directDistanceLabel.setText(decimalFormat.format(directDistance.getValue()) + " " + directDistance.getUnit()
          .getAbbreviation());
      verticalDistanceLabel.setText(decimalFormat.format(verticalDistance.getValue()) + " " + verticalDistance
          .getUnit().getAbbreviation());
      horizontalDistanceLabel.setText(decimalFormat.format(horizontalDistance.getValue()) + " " + horizontalDistance
          .getUnit().getAbbreviation());
    });

    // add the unit system options to the UI initialized with the measurement's default value (METRIC)
    unitSystemComboBox.getItems().addAll(UnitSystem.values());
    unitSystemComboBox.setValue(distanceMeasurement.getUnitSystem());

    // remove the default mouse move handler
    sceneView.setOnMouseMoved(null);

    // create a handler to update the measurement's end location when the mouse moves
    EventHandler<MouseEvent> mouseMoveEventHandler = event -> {
      Point2D point2D = new Point2D(event.getX(), event.getY());
      // get the scene location from the screen position
      ListenableFuture<Point> pointFuture = sceneView.screenToLocationAsync(point2D);
      pointFuture.addDoneListener(() -> {
        try {
          // update the end location
          Point point = pointFuture.get();
          distanceMeasurement.setEndLocation(point);
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      });
    };

    // mouse click to start/stop moving a measurement
    sceneView.setOnMouseClicked(event -> {
      if (event.isStillSincePress() && event.getButton() == MouseButton.PRIMARY) {
        // get the clicked location
        Point2D point2D = new Point2D(event.getX(), event.getY());
        ListenableFuture<Point> pointFuture = sceneView.screenToLocationAsync(point2D);
        pointFuture.addDoneListener(() -> {
          try {
            Point point = pointFuture.get();
            if (sceneView.getOnMouseMoved() == null) {
              // reset the measurement at the clicked location and start listening for mouse movement
              sceneView.setOnMouseMoved(mouseMoveEventHandler);
              distanceMeasurement.setStartLocation(point);
              distanceMeasurement.setEndLocation(point);
            } else {
              // set the measurement end location and stop listening for mouse movement
              sceneView.setOnMouseMoved(null);
              distanceMeasurement.setEndLocation(point);
            }
          } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
          }
        });
      }
    });
  }

  /**
   * Update the measurement's unit system when the combo box option is changed.
   */
  @FXML
  private void changeUnitSystem() {

    distanceMeasurement.setUnitSystem(unitSystemComboBox.getValue());
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
