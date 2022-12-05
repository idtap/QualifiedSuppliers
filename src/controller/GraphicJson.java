package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.ColorUtil;
import com.esri.arcgisruntime.symbology.ModelSceneSymbol;
import com.esri.arcgisruntime.symbology.SceneSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSceneSymbol;
import com.esri.arcgisruntime.symbology.Symbol;

import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;

class GraphicJson {
	
	GraphicJson(String value) {
		strOpenFile = value;
	}
	
	private String strOpenFile = "";
	
	public String getOpenFilePath() {
		return strOpenFile;
	}
	
	public void setOpenFilePath(String value) {
		strOpenFile = value;
	}
	
	private String graphicToJson(String strGeometry, String strSymbol, String strAttributes) {
		JSONObject obj = new JSONObject();
		obj.put("geometry", strGeometry);
		obj.put("symbol", strSymbol);
		obj.put("Attributes", strAttributes);
		return obj.toJSONString();
	}
	
	public Graphic jsonToGraphic(String Value) {
		JSONObject obj = (JSONObject)JSONValue.parse(Value);
		Geometry g = Geometry.fromJson(obj.get("geometry").toString());
		Symbol s = ( obj.get("symbol").equals("") ? null : Symbol.fromJson(obj.get("symbol").toString()));
		JSONObject attr = (JSONObject)JSONValue.parse(obj.get("Attributes").toString());
		
		if (attr.containsKey("kind")) {

			s = new SimpleMarkerSceneSymbol(SimpleMarkerSceneSymbol.Style.valueOf(attr.get("kind").toString()),
					 ColorUtil.colorToArgb(Color.PURPLE), 300, 300, 300, SceneSymbol.AnchorPosition.CENTER);
		}

		Graphic graphic = new Graphic(g, s);
		attr.forEach((key, val) -> {
			graphic.getAttributes().put((String) key, val);
		});
		
		return graphic;
	}	
	
	public Graphic jsonToGraphicForMilitarySymbol(String Value) {
		JSONObject obj = (JSONObject)JSONValue.parse(Value);
		Geometry g = Geometry.fromJson(obj.get("geometry").toString());
		JSONObject attr = (JSONObject)JSONValue.parse(obj.get("Attributes").toString());
		
		Map<String, Object> attributes = new HashMap<>();
		attr.forEach((key, val) -> {
			attributes.put((String) key, val);
		});

		Graphic graphic = new Graphic(g, attributes);
		return graphic;
	}	
	
	public void SaveGraphicsToJsonFile(GraphicsOverlay graphicsOverlay2D, Window win) {
		
		HashMap<String, String> hashMap = new HashMap<String, String>();
		int key = 1;
		if (!graphicsOverlay2D.getGraphics().isEmpty()) {
			for (Graphic g : graphicsOverlay2D.getGraphics()) {
				JSONObject attrObject = new JSONObject(g.getAttributes());
				String jsonObject = "";
				if (null == g.getSymbol()) {
					//2525B Graphic 沒有Symbol
					jsonObject = graphicToJson(g.getGeometry().toJson(), "", attrObject.toJSONString());
				}
				else if (g.getSymbol().getClass().equals(SimpleMarkerSceneSymbol.class)) {
					//3D Marker Graphic(如:球體) 沒有Symbol
					jsonObject = graphicToJson(g.getGeometry().toJson(), "", attrObject.toJSONString());
				}
				else if (g.getSymbol().getClass().equals(ModelSceneSymbol.class))
			    	continue;
				else
					jsonObject = graphicToJson(g.getGeometry().toJson(), g.getSymbol().toJson(), attrObject.toJSONString());
				
				hashMap.put(String.valueOf(key), jsonObject);
				key++;
			}
			
			String strAll = JSONObject.toJSONString(hashMap);
			
			if (strOpenFile.equals("")) 
				SaveAsFile(strAll, win);
			else
				SaveFile(strOpenFile, strAll);
		}
	}
	
	public void OpenJsonFileImportGraphics(GraphicsOverlay graphicsOverlay) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(strOpenFile));
			HashMap<String, String> hashMap = (HashMap<String, String>) JSONValue.parse(br);
			hashMap.forEach((key, value) -> {
				Graphic g = jsonToGraphic(value);
				graphicsOverlay.getGraphics().add(g);
			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        finally {
        	try {
				br.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
	}
	
	public void OpenJsonFileImportGraphicsForMilitarySymbol(GraphicsOverlay graphicsOverlay) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(strOpenFile));
			HashMap<String, String> hashMap = (HashMap<String, String>) JSONValue.parse(br);
			hashMap.forEach((key, value) -> {
				Graphic g = jsonToGraphicForMilitarySymbol(value);
				graphicsOverlay.getGraphics().add(g);
			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        finally {
        	try {
				br.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
	}
	
	
	private void SaveFile(String filePath, String value) {
		FileWriter file = null;
		try {
			file = new FileWriter(strOpenFile);
			file.write(value);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        finally {
        	try {
				file.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
	}
	
	private void SaveAsFile(String value, Window win) {
		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File("."));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

		File selectedFile = fileChooser.showSaveDialog(win);
		if (selectedFile == null)
			return;
		
		strOpenFile = selectedFile.getAbsolutePath();
		SaveFile(strOpenFile, value);
	}
}
