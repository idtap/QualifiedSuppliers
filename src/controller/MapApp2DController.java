package controller;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ResourceBundle;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.GeodesicEllipseParameters;
import com.esri.arcgisruntime.geometry.GeodesicSectorParameters;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.PolygonBuilder;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.symbology.ColorUtil;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol.HorizontalAlignment;
import com.esri.arcgisruntime.symbology.TextSymbol.VerticalAlignment;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import main.MapManager;
import tool.StackProfile;
import tool.SurfaceDistanceCal;
import tool.SurfaceDistanceCal.CaculateFinishedEventHandler;

public class MapApp2DController implements Initializable {

	public enum ApState {
		normal, 點, 線, 面, 文字, 圓形, 矩形, 多邊形, 扇形
	}

	public ApState apState = ApState.normal;

	public static ObservableList<String> drawOptions = FXCollections.observableArrayList("點", "線", "面", "文字", "圓形",
			"矩形", "多邊形", "扇形");

	@FXML
	private ComboBox<String> cmbDraw;

	GraphicsOverlay tmpGraphicsOverlay = null; // 暫時繪點之圖層
	private Graphic selectedGraphic = null;
	public Stage _stage_main;
	GraphicJson gf = new GraphicJson("Overlay2D.json");

	String inputText = "";
	double dblRadius = 0.0;
	double begin = 0.0;
	double angle = 0.0;
	double dblWidth = 0.0;
	double dblHeight = 0.0;

	// create a new point collection for polyline
	PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());

	double lengthGeodetic = 0.0;

	@FXML
	private Button btnUpdateName;

	@FXML
	private StackPane stackPane;

	@FXML
	private TextField txtName;
	
	@FXML
	private ProgressBar progressBar;

    @FXML
    void handle2DClicked(ActionEvent event) {
    	setViewPoint();
    }
	
	private void Init() {
		MapManager.InitMap2D();
		
		// add the map view to stack pane
		stackPane.getChildren().addAll(MapManager.mapView);

		tmpGraphicsOverlay = new GraphicsOverlay();
		MapManager.mapView.getGraphicsOverlays().add(0, tmpGraphicsOverlay);

		InitMapViewEvent();
		setViewPoint();

		cmbDraw.getItems().addAll(drawOptions);
		cmbDraw.valueProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue ov, String oldValue, String newValue) {
//				Alert alertPoint = new Alert(Alert.AlertType.INFORMATION, "請於圖台上標註單點圖形繪製位置!!");
//				Alert alertPoint2 = new Alert(Alert.AlertType.INFORMATION, "請於圖台上標註多點圖形繪製形狀!!");

				switch (newValue) {
				case "點":
					apState = ApState.點;
//					alertPoint.showAndWait();
					break;
				case "線":
					apState = ApState.線;
//					alertPoint2.showAndWait();
					break;
				case "面":
					apState = ApState.面;
//					alertPoint2.showAndWait();
					break;
				case "文字":
					TextInputDialog dialog = new TextInputDialog("繪製文字");
					dialog.setTitle("文字圖形");
					dialog.setHeaderText("請輸入欲繪出之文字:");
					dialog.setContentText("文字:");
					Optional<String> result = dialog.showAndWait();
					result.ifPresent(name -> {
						inputText = name;
						apState = ApState.文字;
//						alertPoint.showAndWait();
					});
					break;
				case "圓形":
					TextInputDialog dialog2 = new TextInputDialog("3");
					dialog2.setTitle("圓形圖形");
					dialog2.setHeaderText("請輸入圓形半徑(公里):");
					dialog2.setContentText("半徑:");
					Optional<String> result2 = dialog2.showAndWait();
					result2.ifPresent(value -> {
						dblRadius = Double.parseDouble(value);
						apState = ApState.圓形;
//						alertPoint.showAndWait();
					});
					break;
				case "矩形":
					// Create the custom dialog.
					Dialog<Pair<String, String>> dialog4 = new Dialog<>();
					dialog4.setTitle("繪製矩形");
					dialog4.setHeaderText("繪製矩形");

					// Set the button types.
					ButtonType OKButtonType = new ButtonType("確定", ButtonData.OK_DONE);
					dialog4.getDialogPane().getButtonTypes().addAll(OKButtonType, ButtonType.CANCEL);

					// Create the username and password labels and fields.
					GridPane grid = new GridPane();
					grid.setHgap(10);
					grid.setVgap(10);
					grid.setPadding(new Insets(20, 150, 10, 10));

					TextField txtWidth = new TextField();
					txtWidth.setText("5");
					TextField txtHeight = new TextField();
					txtHeight.setText("7");

					grid.add(new Label("長度(公里):"), 0, 0);
					grid.add(txtWidth, 1, 0);
					grid.add(new Label("高度(公里):"), 0, 1);
					grid.add(txtHeight, 1, 1);

					dialog4.getDialogPane().setContent(grid);

					// Request focus on the username field by default.
					Platform.runLater(() -> txtWidth.requestFocus());

					// Convert the result to a username-password-pair when the login button is
					// clicked.
					dialog4.setResultConverter(dialogButton -> {
						if (dialogButton == OKButtonType) {
							return new Pair<>(txtWidth.getText(), txtHeight.getText());
						}
						return null;
					});

					Optional<Pair<String, String>> result4 = dialog4.showAndWait();

					result4.ifPresent(params -> {
						System.out.println("width=" + params.getKey() + ", height=" + params.getValue());

						dblWidth = Double.parseDouble(params.getKey());
						dblHeight = Double.parseDouble(params.getValue());
						apState = ApState.矩形;
//						alertPoint.showAndWait();
					});
					break;
				case "多邊形":
					apState = ApState.多邊形;
//					alertPoint2.showAndWait();
					break;
				case "扇形":
					// Create the custom dialog.
					Dialog<Pair<String, String>> dialog3 = new Dialog<>();
					dialog3.setTitle("繪製扇形");
					dialog3.setHeaderText("繪製扇形");

					// Set the button types.
					ButtonType OKButtonType2 = new ButtonType("確定", ButtonData.OK_DONE);
					dialog3.getDialogPane().getButtonTypes().addAll(OKButtonType2, ButtonType.CANCEL);

					// Create the username and password labels and fields.
					GridPane grid2 = new GridPane();
					grid2.setHgap(10);
					grid2.setVgap(10);
					grid2.setPadding(new Insets(20, 150, 10, 10));

					TextField txtRadius = new TextField();
					txtRadius.setText("5");
					TextField angleRange = new TextField();
					angleRange.setText("0,90");

					grid2.add(new Label("半徑(公里):"), 0, 0);
					grid2.add(txtRadius, 1, 0);
					grid2.add(new Label("起始角,弧度:"), 0, 1);
					grid2.add(angleRange, 1, 1);

					dialog3.getDialogPane().setContent(grid2);

					// Request focus on the username field by default.
					Platform.runLater(() -> txtRadius.requestFocus());

					// Convert the result to a username-password-pair when the login button is
					// clicked.
					dialog3.setResultConverter(dialogButton -> {
						if (dialogButton == OKButtonType2) {
							return new Pair<>(txtRadius.getText(), angleRange.getText());
						}
						return null;
					});

					Optional<Pair<String, String>> result3 = dialog3.showAndWait();

					result3.ifPresent(params -> {
						System.out.println("半徑=" + params.getKey() + ", 角度起訖=" + params.getValue());

						dblRadius = Double.parseDouble(params.getKey());
						String[] vals = params.getValue().split(",");
						begin = -1 * Double.parseDouble(vals[0]) + 90;
						angle = Double.parseDouble(vals[1]);

						apState = ApState.扇形;
//						alertPoint.showAndWait();
					});

					break;
				}
			};
		});

		gf.OpenJsonFileImportGraphics(MapManager.graphicsOverlay2D);

		// 儲存2D GraphicOverlay的Graphics到Json檔
		// gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);

		// Military Overlay
		//AddMilitarySymbolGraphic();
		//AddPathLine();

	}

	private void AddPathLine() {

		// add start Point
		Point start = new Point(115.19, 22.671, SpatialReferences.getWgs84());

		// create destination Point
		Point destination = new Point(126.507, 22.671, SpatialReferences.getWgs84());

		// create a graphic representing the geodesic path between the two locations
		Graphic path = new Graphic();
		path.setSymbol(new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, 0xFFFF0000, 2));
		MapManager.graphicsOverlay2D.getGraphics().add(path);

		// create a straight line path between the start and end locations
		PointCollection points = new PointCollection(Arrays.asList(start, destination), SpatialReferences.getWgs84());
		Polyline polyline = new Polyline(points);
		// densify the path as a geodesic curve and show it with the path graphic
		Geometry pathGeometry = GeometryEngine.densifyGeodetic(polyline, 1, new LinearUnit(LinearUnitId.KILOMETERS),
				GeodeticCurveType.GEODESIC);
		path.setGeometry(pathGeometry);

		path.getAttributes().put("key", "path");
		path.getAttributes().put("name", "地表直線");

		// calculate the path distance
		lengthGeodetic = GeometryEngine.lengthGeodetic(pathGeometry, new LinearUnit(LinearUnitId.KILOMETERS),
				GeodeticCurveType.GEODESIC);
	}

	@FXML
	void handlebtnUpdateName(ActionEvent event) {
		if (selectedGraphic.getSymbol().getClass().equals(TextSymbol.class))
			((TextSymbol) selectedGraphic.getSymbol()).setText(txtName.getText());
		else
			selectedGraphic.getAttributes().put("name", txtName.getText());

		gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
	}

	@FXML
	void handleIMGClicked(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File(System.getProperty("data.dir"), "./samples-data"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("IMG | BMP | JPG | SHP | SID | TIFF",
				"*.img;*.jpg;*.bmp;*.shp;*.sid;*.tif"));

		File selectedFile = fileChooser.showOpenDialog(_stage_main);
		if (selectedFile == null)
			return;

		doPath(selectedFile);
	}

	private void doPath(File selectedFile) {
		if (selectedFile.getName().endsWith(".shp")) {
			handleShapefileClicked(selectedFile.getAbsolutePath());
		} else if (selectedFile.getName().endsWith(".bmp")) {
			handleImgJpgClicked(new File(System.getProperty("data.dir"), "./samples-data/png/SanFrancisco.png")
					.getAbsolutePath());
		} else
			handleImgJpgClicked(selectedFile.getAbsolutePath());
	}

	void handleImgJpgClicked(String path) {
		String rasterURL = path;
		Raster raster = new Raster(rasterURL);
		RasterLayer myRasterLayer = new RasterLayer(raster);

		myRasterLayer.addDoneLoadingListener(() -> {
			if (myRasterLayer.getLoadStatus() == LoadStatus.LOADED) {
				MapManager.mapView.getMap().getOperationalLayers().add(myRasterLayer);
				MapManager.mapView.setViewpointGeometryAsync(myRasterLayer.getFullExtent(), 50);
			}
		});
		raster.loadAsync();
	}

	void handleShapefileClicked(String path) {
		ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(path);
		FeatureLayer featureLayer = new FeatureLayer(shapefileFeatureTable);

		featureLayer.addDoneLoadingListener(() -> {
			if (featureLayer.getLoadStatus() == LoadStatus.LOADED) {
				// Add the feature layer to the map
				MapManager.mapView.getMap().getOperationalLayers().add(featureLayer);
				MapManager.mapView.setViewpointGeometryAsync(featureLayer.getFullExtent(), 50);
			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR, featureLayer.getLoadError().getMessage());
				alert.show();
			}
		});

		shapefileFeatureTable.loadAsync();
	}

	private Point QueryLocation(Point p) {
		// Create the custom dialog.
		Dialog<Pair<String, String>> dialog4 = new Dialog<>();
		dialog4.setTitle("座標設定");
		dialog4.setHeaderText("請設定座標位置");

		// Set the button types.
		ButtonType OKButtonType = new ButtonType("確定", ButtonData.OK_DONE);
		dialog4.getDialogPane().getButtonTypes().addAll(OKButtonType, ButtonType.CANCEL);

		// Create the username and password labels and fields.
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 40, 10, 10));

		TextField txtLon = new TextField();
		txtLon.setText(String.format("%.3f",p.getX()));
		TextField txtLat = new TextField();
		txtLat.setText(String.format("%.3f",p.getY()));

		grid.add(new Label("經度:"), 0, 0);
		grid.add(txtLon, 1, 0);
		grid.add(new Label("緯度:"), 0, 1);
		grid.add(txtLat, 1, 1);

		dialog4.getDialogPane().setContent(grid);

		// Request focus on the username field by default.
		Platform.runLater(() -> txtLon.requestFocus());

		// Convert the result to a username-password-pair when the login button is
		// clicked.
		dialog4.setResultConverter(dialogButton -> {
			if (dialogButton == OKButtonType) {
				return new Pair<>(txtLon.getText(), txtLat.getText());
			}
			return null;
		});

		Optional<Pair<String, String>> result4 = dialog4.showAndWait();
		try {
			
			String lon = result4.get().getKey();
			String lat = result4.get().getValue();
			return new Point(Double.parseDouble(lon), Double.parseDouble(lat), p.getSpatialReference());
		} catch (Exception ex) {
			return null;
		}
	}
	
	private void InitMapViewEvent() {
		MapManager.mapView.setOnMouseClicked(e -> {
			if (e.isStillSincePress()) {
				if (e.getButton() == MouseButton.PRIMARY) {
					Point2D screenPoint = new Point2D(e.getX(), e.getY());
					switch (apState) {
					case 點:
						Point mapPoint1 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint2 = (Point) GeometryEngine.project(mapPoint1, SpatialReferences.getWgs84());
						Point mapAns1 = QueryLocation(mapPoint2);
						if (mapAns1 == null)
							return;
						AddPointGraphics(mapAns1);
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						apState = ApState.normal;
						break;
					case 線:
						Point mapPoint8 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint9 = (Point) GeometryEngine.project(mapPoint8, SpatialReferences.getWgs84());
						Point mapAns2 = QueryLocation(mapPoint9);
						if (mapAns2 == null)
							return;
						AddTmpPointGraphic(mapAns2);
						break;
					case 面:
						Point mapPoint3 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint4 = (Point) GeometryEngine.project(mapPoint3, SpatialReferences.getWgs84());
						Point mapAns3 = QueryLocation(mapPoint4);
						if (mapAns3 == null)
							return;
						AddTmpPointGraphic(mapAns3);
						break;
					case 文字:
						Point mapPoint5 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint6 = (Point) GeometryEngine.project(mapPoint5, SpatialReferences.getWgs84());
						Point mapAns6 = QueryLocation(mapPoint6);
						if (mapAns6 == null)
							return;						
						AddTextGraphics(mapAns6);
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						apState = ApState.normal;
						break;
					case 圓形:
						Point mapPoint11 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint12 = (Point) GeometryEngine.project(mapPoint11, SpatialReferences.getWgs84());
						Point mapAns12 = QueryLocation(mapPoint12);
						if (mapAns12 == null)
							return;	
						AddCircle(mapAns12);
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						apState = ApState.normal;
						break;
					case 矩形:
						Point mapPoint21 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint22 = (Point) GeometryEngine.project(mapPoint21, SpatialReferences.getWgs84());
						Point mapAns22 = QueryLocation(mapPoint22);
						if (mapAns22 == null)
							return;	
						AddRectangle(mapAns22, dblWidth, dblHeight);
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						apState = ApState.normal;
						break;
					case 多邊形:
						Point mapPoint23 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint24 = (Point) GeometryEngine.project(mapPoint23, SpatialReferences.getWgs84());
						Point mapAns4 = QueryLocation(mapPoint24);
						if (mapAns4 == null)
							return;
						AddTmpPointGraphic(mapAns4);
						break;
					case 扇形:
						Point mapPoint13 = MapManager.mapView.screenToLocation(screenPoint);
						Point mapPoint14 = (Point) GeometryEngine.project(mapPoint13, SpatialReferences.getWgs84());
						Point mapAns14 = QueryLocation(mapPoint14);
						if (mapAns14 == null)
							return;	
						AddSector(mapAns14, dblRadius, begin, angle);
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						apState = ApState.normal;
						break;
					default:
						// 先辨別3D graphicsOverlay圖層
						ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics = MapManager.mapView
								.identifyGraphicsOverlayAsync(MapManager.graphicsOverlay2D, screenPoint, 10, false);

						identifyGraphics.addDoneListener(() -> Platform.runLater(() -> {
							try {
								IdentifyGraphicsOverlayResult result = identifyGraphics.get();
								List<Graphic> graphics = result.getGraphics();
								if (!graphics.isEmpty()) {
									Graphic graphic = graphics.get(0);
									if (graphic.getAttributes().containsKey("name")) {
										txtName.setText(graphic.getAttributes().get("name").toString());
										selectedGraphic = graphic;
									} else {
										if (graphic.getSymbol().getClass().equals(TextSymbol.class)) {
											txtName.setText(((TextSymbol) graphic.getSymbol()).getText());
											selectedGraphic = graphic;
										}
									}

									if (graphic.getSymbol().getClass() == SimpleLineSymbol.class) {
										if (graphic.getAttributes().containsKey("key")) {
											Alert dialog = new Alert(AlertType.INFORMATION);
											dialog.setHeaderText("地表直線距離");
											dialog.setContentText(lengthGeodetic + "公里");
											dialog.showAndWait();
										} else {
											Polyline polyline = (Polyline) graphic.getGeometry();
											double distance = GeometryEngine.length(polyline) * 100;

											Alert dialog = new Alert(AlertType.INFORMATION);
											dialog.setHeaderText("平面直線距離:");
											dialog.setContentText(distance + "公里");
											dialog.showAndWait();

										}
									}
									
//									if (graphic.getSymbol().getClass() == SimpleFillSymbol.class) {
//										Polygon polygon = (Polygon) graphic.getGeometry();
//										double area = GeometryEngine.area(polygon) * -10000;
//										
//										Alert dialog = new Alert(AlertType.INFORMATION);
//										dialog.setHeaderText("平面面積:");
//										dialog.setContentText(String.format("%.2f", area) + "平方公里");
//										dialog.showAndWait();
//									}
								} else {
									// 後辨別兵棋符號圖層
									ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics2 = MapManager.mapView
											.identifyGraphicsOverlayAsync(MapManager.graphicsOverlay_military_2Dsymbol,
													screenPoint, 10, false);

									identifyGraphics2.addDoneListener(() -> Platform.runLater(() -> {
										try {
											IdentifyGraphicsOverlayResult result2 = identifyGraphics2.get();
											List<Graphic> graphics2 = result2.getGraphics();
											if (!graphics2.isEmpty()) {
												Graphic graphic = graphics2.get(0);
												if (graphic.getAttributes().containsKey("name")) {
													txtName.setText(graphic.getAttributes().get("name").toString());
													selectedGraphic = graphic;
												}
											}
										} catch (Exception e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
									}));
								}

							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}));
						break;
					}
				}
			}
			
			if (e.getButton() == MouseButton.SECONDARY) {
				Point2D screenPoint = new Point2D(e.getX(), e.getY());
				switch (apState) {
				case 線:
					Point mapPoint8 = MapManager.mapView.screenToLocation(screenPoint);
					Point mapPoint9 = (Point) GeometryEngine.project(mapPoint8, SpatialReferences.getWgs84());
					Point mapAns2 = QueryLocation(mapPoint9);
					if (mapAns2 == null)
						return;
					AddTmpPointGraphic(mapAns2);
						
					if (pointCollection.size() >= 2) {
						AddLineGraphics();
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						tmpGraphicsOverlay.getGraphics().clear();
						apState = ApState.normal;
					}
					break;
				case 面:
					Point mapPoint3 = MapManager.mapView.screenToLocation(screenPoint);
					Point mapPoint4 = (Point) GeometryEngine.project(mapPoint3, SpatialReferences.getWgs84());
					Point mapAns3 = QueryLocation(mapPoint4);
					if (mapAns3 == null)
						return;
					AddTmpPointGraphic(mapAns3);
					if (pointCollection.size() >= 3) {
						AddPolygonGraphics();
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						tmpGraphicsOverlay.getGraphics().clear();
						apState = ApState.normal;
					}
					break;		
				case 多邊形:
					Point mapPoint23 = MapManager.mapView.screenToLocation(screenPoint);
					Point mapPoint24 = (Point) GeometryEngine.project(mapPoint23, SpatialReferences.getWgs84());
					Point mapAns4 = QueryLocation(mapPoint24);
					if (mapAns4 == null)
						return;
					AddTmpPointGraphic(mapAns4);

					if (pointCollection.size() >= 3) {
						AddPolygonGraphics();
						gf.SaveGraphicsToJsonFile(MapManager.graphicsOverlay2D, null);
						tmpGraphicsOverlay.getGraphics().clear();
						apState = ApState.normal;
					}
					break;					
				}
			}
		});
	}

	private void AddTmpPointGraphic(Point p1) {
		Point buoy1Loc = new Point(p1.getX(), p1.getY(), MapManager.getSpatialReference());
		pointCollection.add(buoy1Loc);

		// create a red (0xFFFF0000) circle simple marker symbol
		SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
				ColorUtil.colorToArgb(Color.RED), 3);

		// create graphics and add to graphics overlay
		Graphic buoyGraphic1 = new Graphic(buoy1Loc, redCircleSymbol);

		tmpGraphicsOverlay.getGraphics().add(buoyGraphic1);
	}

	private void AddPointGraphics(Point p1) {
		Point buoy1Loc = new Point(p1.getX(), p1.getY(), MapManager.getSpatialReference());
		// create a red (0xFFFF0000) circle simple marker symbol
		SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFFF0000, 10);

		// create graphics and add to graphics overlay
		Graphic buoyGraphic1 = new Graphic(buoy1Loc, redCircleSymbol);
		buoyGraphic1.getAttributes().put("name", "任意點");

		MapManager.graphicsOverlay2D.getGraphics().add(buoyGraphic1);

	}

	/** 畫圓形 */
	public static Polygon CreateCircleWithEngine(Point pointCenter, double dblRadius) {
		if (dblRadius == 0)
			return null;

		GeodesicEllipseParameters parms = new GeodesicEllipseParameters();
		parms.setCenter(pointCenter);
		parms.setSemiAxis1Length(dblRadius); // X
		parms.setSemiAxis2Length(dblRadius); // Y
		parms.setLinearUnit(new LinearUnit(LinearUnitId.KILOMETERS));
		parms.setMaxPointCount(3600);
		parms.setMaxSegmentLength(1);
		Geometry gg = GeometryEngine.ellipseGeodesic(parms);
		return (Polygon) gg;
	}

	public static Polygon CreateSectorWithEngine(Point pointCenter, double dblRadius, double begin, double angle) {
		if (dblRadius == 0)
			return null;

		GeodesicSectorParameters parms = new GeodesicSectorParameters(pointCenter, dblRadius, dblRadius, angle, begin);
		parms.setMaxPointCount(3600);
		parms.setLinearUnit(new LinearUnit(LinearUnitId.KILOMETERS));

		parms.setMaxSegmentLength(1);
		Geometry gg = GeometryEngine.sectorGeodesic(parms);
		return (Polygon) gg;

	}

	private static Polygon DrawRectangle(Point pCenter, double Width, double Height) {
		Point p = (Point) GeometryEngine.project(pCenter, SpatialReferences.getWebMercator());

		PolygonBuilder polylineBuilder = new PolygonBuilder(SpatialReferences.getWebMercator());

		double pLeftX = 0.0, pRightX = 0.0, pTopY = 0.0, pBottomY = 0.0;
		pLeftX = p.getX() - (Width / 2);
		pRightX = p.getX() + (Width / 2);
		pTopY = p.getY() - (Height / 2);
		pBottomY = p.getY() + (Height / 2);

		polylineBuilder.addPoint(new Point(pLeftX, pTopY));
		polylineBuilder.addPoint(new Point(pRightX, pTopY));
		polylineBuilder.addPoint(new Point(pRightX, pBottomY));
		polylineBuilder.addPoint(new Point(pLeftX, pBottomY));

		return (Polygon) GeometryEngine.project(polylineBuilder.toGeometry(), SpatialReferences.getWgs84());
	}

	private void AddCircle(Point p1) {
		Point p = new Point(p1.getX(), p1.getY(), SpatialReferences.getWgs84());
		Polygon polygon = CreateCircleWithEngine(p, dblRadius); // (2公里)

		// define the polygon outline
		SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH,
				ColorUtil.colorToArgb(Color.RED), 4);

		// define the fill symbol
		SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.DIAGONAL_CROSS,
				ColorUtil.colorToArgb(Color.YELLOW), outlineSymbol);

		// create the graphic
		Graphic nestingGraphic = new Graphic(polygon, fillSymbol);
		nestingGraphic.getAttributes().put("name", "圓形");

		// add the polygon graphic
		MapManager.graphicsOverlay2D.getGraphics().add(nestingGraphic);

		dblRadius = 0.0;
	}

	private void AddSector(Point p1, double dblRadius, double begin, double angle) {

		Polygon polygon = CreateSectorWithEngine(p1, dblRadius, begin, angle); // (2公里, 0度, 60度)

		// define the polygon outline
		SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH,
				ColorUtil.colorToArgb(Color.RED), 4);

		// define the fill symbol
		SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.DIAGONAL_CROSS,
				ColorUtil.colorToArgb(Color.YELLOW), outlineSymbol);

		// create the graphic
		Graphic nestingGraphic = new Graphic(polygon, fillSymbol);
		nestingGraphic.getAttributes().put("name", "任意扇形");

		// add the polygon graphic
		MapManager.graphicsOverlay2D.getGraphics().add(nestingGraphic);
	}

	private void AddRectangle(Point p1, double Width, double Height) {
		Polygon polygon = DrawRectangle(p1, Width * 1000, Height * 1000);

		// define the polygon outline
		SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH,
				ColorUtil.colorToArgb(Color.RED), 4);

		// define the fill symbol
		SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.DIAGONAL_CROSS,
				ColorUtil.colorToArgb(Color.YELLOW), outlineSymbol);

		// create the graphic
		Graphic nestingGraphic = new Graphic(polygon, fillSymbol);
		nestingGraphic.getAttributes().put("name", "任意矩形");

		// add the polygon graphic
		MapManager.graphicsOverlay2D.getGraphics().add(nestingGraphic);
	}

	private void setViewPoint() {
		Point buoy1Loc = new Point(120.500, 22.551, SpatialReferences.getWgs84());
		Viewpoint viewpoint = new Viewpoint(buoy1Loc, 250000);
		MapManager.mapView.setViewpoint(viewpoint);
	}

	Graphic lineGraphic = null;

	private void AddLineGraphics() {
		// create the polyline from the point collection
		Polyline polyline = new Polyline(pointCollection);

		// create a purple (0xFF800080) simple line symbol
		SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF800080, 4);

		// create the graphic with polyline and symbol
		lineGraphic = new Graphic(polyline, lineSymbol);
		lineGraphic.getAttributes().put("name", "任意線");

		// add graphic to the graphics overlay
		MapManager.graphicsOverlay2D.getGraphics().add(lineGraphic);
		pointCollection.clear();
	}

	private void AddPolygonGraphics() {
		// create the polyline from the point collection
		Polygon polygon = new Polygon(pointCollection);
		// create a green (0xFF005000) simple line symbol
		SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, 0xFF005000, 1);
		// create a green (0xFF005000) mesh simple fill symbol
		SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.CROSS, 0xFF005000, outlineSymbol);

		// create the graphic with polyline and symbol
		Graphic graphic = new Graphic(polygon, fillSymbol);
		graphic.getAttributes().put("name", "任意面");

		// add graphic to the graphics overlay
		MapManager.graphicsOverlay2D.getGraphics().add(graphic);
		pointCollection.clear();
	}

	private void AddTextGraphics(Point p1) {
		// create two points
		Point bassPoint = new Point(p1.getX(), p1.getY(), SpatialReferences.getWgs84());

		final int BLUE = 0xFF0000E6;
		// create two text symbols
		TextSymbol bassRockTextSymbol = new TextSymbol(10, inputText, BLUE, HorizontalAlignment.LEFT,
				VerticalAlignment.BOTTOM);

		// create two graphics from the points and symbols
		Graphic bassRockGraphic = new Graphic(bassPoint, bassRockTextSymbol);
		bassRockGraphic.getAttributes().put("location", String.format("%f,%f", bassPoint.getX(), bassPoint.getY()));

		// add graphics to the graphics overlay
		MapManager.graphicsOverlay2D.getGraphics().add(bassRockGraphic);

		inputText = "";
	}

	private void AddMilitarySymbolGraphic() {
		// 2525C
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("_type", "position_report");
		attributes.put("_action", "update");
		attributes.put("sidc", "SSGPUC-----BTW-");
		attributes.put("name", "自訂2525C符號");

		Point p = new Point(120.6, 22.4, SpatialReferences.getWgs84());

		Graphic millitaryGraphic = new Graphic(p, attributes);
		MapManager.graphicsOverlay_military_2Dsymbol.getGraphics().add(millitaryGraphic);
	}

	/**
	 * Stops and releases all resources used in application.
	 */
	public void terminate() {

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub
		Init();
	}
}
