package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.milSymbolTreeView;
import main.perferCode;
import tool.Event;

public class milSymbolPickerController implements Initializable {

	public Stage _stage_main;
	private milSymbolTreeView milSymbolTreeView;
	private sidcEditorController sidcEditor;
	
    private Stage dialogStage;
	private boolean _applyClicked = false;
	private String selectedShape = "";
    
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
    void handkebtnOKClicked(ActionEvent event) {
		_applyClicked = true;
		dialogStage.close();
    }
    
    @FXML
    void handkebtnCancellicked(ActionEvent event) {
		_applyClicked = false;
		dialogStage.close();
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
    
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		setUserComponents();
	}

	private void setUserComponents() {
		try {
			// 樹狀結構
			milSymbolTreeView = new milSymbolTreeView();
			milSymbolTreeView.SymbolSelectedEvent.addListener("setSidcEditorSymbolID", (SIC, Shape) -> setSidcEditorSymbolID(SIC, Shape));
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

	public void setSidcEditorSymbolID(String SIC, String Shape) {
		sidcEditor.SetSIC(SIC);
		this.selectedShape = Shape;
	}

}
