package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.milSymbolCode;
import main.milSymbolTreeView;

public class milSymbolPickerController implements Initializable {

	public Stage _stage_main;
	private milSymbolTreeView milSymbolTreeView;
	private sidcEditorController sidcEditor;

	private Stage dialogStage;
	private boolean _applyClicked = false;
	private String selectedShape = "";

	private Hashtable<String, String> favoriteMap = new Hashtable<>();

	@FXML
	private Button btnOpen;

	@FXML
	private Button btnOK;

	@FXML
	private Label txtFilepath;

	@FXML
	private AnchorPane milTreeView;

	@FXML
	private AnchorPane sidcPane;

	@FXML
	private TextField filterField;

	@FXML
	private ListView listFavorite;

	@FXML
	void handkebtnOKClicked(ActionEvent event) {
		_applyClicked = true;
		dialogStage.close();
	}

	@FXML
	void handkebtnCancellicked(ActionEvent event) {
		_applyClicked = false;
		dialogStage.close();
	}

	@FXML
	void handleKeywordSearch(ActionEvent event) {

	}

	public void setDialogStage(Stage dialogStage) {
		this.dialogStage = dialogStage;
	}

	/** 是否為套用更新 */
	public boolean isApplyClicked() {
		return _applyClicked;
	}

	public String getDrawSymbolID() {
		return sidcEditor.getSymbolID();
	}

	public String getSelectedShape() {
		return selectedShape;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		setUserComponents();
		createFilteredTree();

		// 常用清單點選項目
		listFavorite.setOnMouseClicked(event -> {
			String selectedItem = listFavorite.getSelectionModel().getSelectedItem().toString();
			sidcEditor.SetSIC(favoriteMap.get(selectedItem));
		});

		// 常用清單加入子選單
		listFavorite.setCellFactory(lv -> {

			ListCell<String> cell = new ListCell<>();

			ContextMenu contextMenu = new ContextMenu();

			MenuItem deleteItem = new MenuItem();
			deleteItem.textProperty().bind(Bindings.format("刪除 \"%s\"", cell.itemProperty()));
			deleteItem.setOnAction(event -> { 
				removeFavoriteSymbolID(cell.getItem());				
			});
			contextMenu.getItems().addAll(deleteItem);

			cell.textProperty().bind(cell.itemProperty());

			cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
				if (isNowEmpty) {
					cell.setContextMenu(null);
				} else {
					cell.setContextMenu(contextMenu);
				}
			});
			return cell;
		});

		readFavoriteJson();
	}

	private String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	/** 讀取外部json檔案得到JSONObject */
	public JSONObject readJsonFromPath(String path) throws IOException, JSONException {
		FileInputStream is = new FileInputStream(path);
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
	 * 讀取json檔案得到常用清單並顯示
	 */
	private void readFavoriteJson() {

		try {
			JSONObject result = readJsonFromPath("cfg/FEfavorite.json");
			Iterator<?> keys = result.keys();

			while (keys.hasNext()) {
				String key = (String) keys.next();
				String value = result.getString(key);
				favoriteMap.put(key, value);

				listFavorite.getItems().add(key);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void setUserComponents() {
		try {
			// 樹狀結構
			milSymbolTreeView = new milSymbolTreeView();
			milSymbolTreeView.SymbolSelectedEvent.addListener("setSidcEditorSymbolID",
					(SIC, Shape) -> setSidcEditorSymbolID(SIC, Shape));
			milSymbolTreeView.SymbolAddFavoriteEvent.addListener("addFavoriteSymbolID",
					(key, attr) -> addFavoriteSymbolID(key, attr));
			AnchorPane.setTopAnchor(milSymbolTreeView.treeView, 0.0);
			AnchorPane.setBottomAnchor(milSymbolTreeView.treeView, 0.0);
			AnchorPane.setLeftAnchor(milSymbolTreeView.treeView, 0.0);
			AnchorPane.setRightAnchor(milSymbolTreeView.treeView, 0.0);
			milTreeView.getChildren().addAll(milSymbolTreeView.treeView);

			// 右側SIC設定
			sidcEditor = new sidcEditorController(); // AntennaVoList);
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sidcEditor.fxml"));
			loader.setController(sidcEditor);
			AnchorPane root = (AnchorPane) loader.load();
			sidcEditor._stage_main = _stage_main;

			Scene scene = new Scene(root);
			AnchorPane.setTopAnchor(root, 0.0);
			AnchorPane.setBottomAnchor(root, 0.0);
			AnchorPane.setLeftAnchor(root, 0.0);
			AnchorPane.setRightAnchor(root, 0.0);
			sidcPane.getChildren().addAll(root);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** 新增至常用清單,並寫入json檔案 */

	private void removeFavoriteSymbolID(String key) {
		favoriteMap.remove(key);
		listFavorite.getItems().remove(key);
		
		try (OutputStreamWriter oos = new OutputStreamWriter(new FileOutputStream("cfg/FEfavorite.json"), Charset.forName("UTF-8"))) {
			JSONObject cacheObject = new JSONObject(favoriteMap);
			cacheObject.write(oos);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	/** 加入項目至常用清單,並存入Json檔 */
	private void addFavoriteSymbolID(String key, milSymbolCode attr) {
		if (favoriteMap.containsKey(key)) {
			return;
		}
		
		favoriteMap.put(key, attr.getCode());
		listFavorite.getItems().add(key);

		try (OutputStreamWriter oos = new OutputStreamWriter(new FileOutputStream("cfg/a.json"), Charset.forName("UTF-8"))) {
			JSONObject cacheObject = new JSONObject(favoriteMap);
			cacheObject.write(oos);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** 設定目前選擇項目,顯示右邊15碼資料 */
	public void setSidcEditorSymbolID(String SIC, String Shape) {
		sidcEditor.SetSIC(SIC);
		this.selectedShape = Shape;
	}

	
	private void createFilteredTree() {
		FilterableTreeItem<String> root = milSymbolTreeView.rootXML;
		root.predicateProperty().bind(Bindings.createObjectBinding(() -> {
			if (filterField.getText() == null || filterField.getText().isEmpty())
				return null;
			return TreeItemPredicate.create(actor -> actor.toString().contains(filterField.getText()));
		}, filterField.textProperty()));
	}

}
