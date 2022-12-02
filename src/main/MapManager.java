package main;

import java.io.File;
import java.util.ArrayList;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISSceneLayer;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.PointCloudLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties.SurfacePlacement;
import com.esri.arcgisruntime.symbology.DictionaryRenderer;
import com.esri.arcgisruntime.symbology.DictionarySymbolStyle;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

public class MapManager {

	public static MapView mapView;
	private static ArcGISMap arcgisMap;
	public static GraphicsOverlay graphicsOverlay2D;

	public static DictionarySymbolStyle dictionarySymbol2D;
	public static GraphicsOverlay graphicsOverlay_military_2Dsymbol;

	public static SceneView sceneView;
	public static ArcGISScene arcgisScene;

	public static GraphicsOverlay graphicsOverlayDraw3D;
	public static GraphicsOverlay graphicsOverlay3D;

	public static DictionarySymbolStyle dictionarySymbol3D;
	public static GraphicsOverlay graphicsOverlay_military_3Dsymbol;

	public static SimpleMarkerSymbol redCircleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,
			0xFFFF0000, 10);

	private static String mapPath = "C:\\test\\map";

	public static void InitMap2D() {
		try {
			// license with a license key
			// ArcGISRuntimeEnvironment.setLicense("runtimestandard,1000,rud000528559,none,1JHJH7E3CZ4HH6JRP132");

			File stylxFile = new File(ArcGISRuntimeEnvironment.getResourcesDirectory() + "/symbols/mil2525c.stylx");
			dictionarySymbol2D = DictionarySymbolStyle.createFromFile(stylxFile.getAbsolutePath());
			dictionarySymbol2D.loadAsync();

			// ** 2D **
			mapView = new MapView();
			mapView.setAttributionTextVisible(false);

			TileCache tileCache_Raster = new TileCache(mapPath + "/raster-L11.tpk");
			ArcGISTiledLayer tiledLayer_Raster = new ArcGISTiledLayer(tileCache_Raster);
			Basemap basemap_Raster = new Basemap(tiledLayer_Raster);
			arcgisMap = new ArcGISMap(basemap_Raster);

			TileCache twCache_Raster = new TileCache(mapPath + "/台灣區域衛星影像.tpk");
			ArcGISTiledLayer twLayer_Raster = new ArcGISTiledLayer(twCache_Raster);
			arcgisMap.getOperationalLayers().add(twLayer_Raster);

			mapView.setMap(arcgisMap);

			// create a graphics overlay for the graphics
			graphicsOverlay2D = new GraphicsOverlay();
			mapView.getGraphicsOverlays().add(graphicsOverlay2D);

			graphicsOverlay_military_2Dsymbol = new GraphicsOverlay();
			// create symbol dictionary from specification
			@SuppressWarnings("deprecation")
			DictionaryRenderer renderer2D = new DictionaryRenderer(dictionarySymbol2D);
			graphicsOverlay_military_2Dsymbol.setRenderer(renderer2D);
			mapView.getGraphicsOverlays().add(graphicsOverlay_military_2Dsymbol);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void InitMap3D() {
		try {
			File stylxFile = new File(ArcGISRuntimeEnvironment.getResourcesDirectory() + "/symbols/mil2525c.stylx");
			dictionarySymbol3D = DictionarySymbolStyle.createFromFile(stylxFile.getAbsolutePath());
			dictionarySymbol3D.loadAsync();

			// ** 3D **
			arcgisScene = new ArcGISScene();

			// set the map to be displayed in this view
			sceneView = new SceneView();
			sceneView.setAttributionTextVisible(false);
			sceneView.setArcGISScene(arcgisScene);

			// create a graphics overlay for the graphics
			graphicsOverlayDraw3D = new GraphicsOverlay();
			sceneView.getGraphicsOverlays().add(graphicsOverlayDraw3D);

			graphicsOverlay3D = new GraphicsOverlay();
			graphicsOverlay3D.getSceneProperties().setSurfacePlacement(SurfacePlacement.ABSOLUTE);
			sceneView.getGraphicsOverlays().add(graphicsOverlay3D);

			graphicsOverlay_military_3Dsymbol = new GraphicsOverlay();
			graphicsOverlay_military_3Dsymbol.getSceneProperties()
					.setSurfacePlacement(SurfacePlacement.DRAPED_BILLBOARDED);
			// create symbol dictionary from specification
			@SuppressWarnings("deprecation")
			DictionaryRenderer renderer3D = new DictionaryRenderer(dictionarySymbol3D);
			graphicsOverlay_military_3Dsymbol.setRenderer(renderer3D);
			graphicsOverlay_military_3Dsymbol.getSceneProperties().setSurfacePlacement(SurfacePlacement.ABSOLUTE);
			sceneView.getGraphicsOverlays().add(graphicsOverlay_military_3Dsymbol);

			TileCache tileCache_Raster3D = new TileCache("C:\\TEST\\Map3D\\basemap\\全球亮灰階底圖.tpkx");
			ArcGISTiledLayer tiledLayer_Raster3D = new ArcGISTiledLayer(tileCache_Raster3D);
			Basemap basemap_Raster3D = new Basemap(tiledLayer_Raster3D);
			
//			TileCache tileCache_Raster3D = new TileCache(mapPath + "/raster-L11.tpk");
//			ArcGISTiledLayer tiledLayer_Raster3D = new ArcGISTiledLayer(tileCache_Raster3D);
//			Basemap basemap_Raster3D = new Basemap(tiledLayer_Raster3D);
//
//			TileCache twCache_Raster3D = new TileCache(mapPath + "/台灣區域衛星影像.tpk");
//			ArcGISTiledLayer twLayer_Raster3D = new ArcGISTiledLayer(twCache_Raster3D);
//			basemap_Raster3D.getBaseLayers().add(twLayer_Raster3D);

			arcgisScene.setBasemap(basemap_Raster3D);

			setDefaultCamera();

			String url1 = new File(System.getProperty("data.dir"), "samples-data/raster-file/TaiwanDem_new_P.tif")
					.getAbsolutePath();
			String url2 = new File(System.getProperty("data.dir"), "samples-data/raster-file/Shasta_Elevation.tif")
					.getAbsolutePath();
			String url3 = new File(System.getProperty("data.dir"), "samples-data/dsm/DSM_n5600w15915P_1984.tif")
					.getAbsolutePath();

			ArrayList<String> list = new ArrayList<String>();
			list.add(url1);
			list.add(url2);
			list.add(url3);

			Surface surface = new Surface();
			surface.getElevationSources().add(new RasterElevationSource(list));
			surface.setElevationExaggeration(1);
			arcgisScene.setBaseSurface(surface);
			

			File pointCloudSLPK = new File(mapPath + "/taipei_Model_white.slpk");
			ArcGISSceneLayer pointCloudLayer = new ArcGISSceneLayer(pointCloudSLPK.getAbsolutePath());
			arcgisScene.getOperationalLayers().add(pointCloudLayer);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** 取得MapView */
	public static SceneView getSceneView() {
		return sceneView;
	}

	public static SpatialReference getSpatialReference() {
		return SpatialReferences.getWgs84();
//		if (sceneView != null)
//			return sceneView.getSpatialReference();
//		else
//			return mapView.getSpatialReference();
	}

	/** 設定原始Camera */
	public static void setDefaultCamera() {
		// 移動場景視角
		Point p = new Point( 120.500, 22.601, 99989);
		Camera camera = new Camera(p, 0, 0, 0);
		Viewpoint vp = new Viewpoint(p, 10000, camera);
		MapManager.getSceneView().setViewpointAsync(vp, 2.0f);
	}
}
