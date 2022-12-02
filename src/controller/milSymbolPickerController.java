package controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import main.milSymbolTreeView;

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
	private TextField filterField;

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
    
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		setUserComponents();
		createFilteredTree();
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

    private void createFilteredTree() {
        FilterableTreeItem<String> root = milSymbolTreeView.rootXML;
        root.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            if (filterField.getText() == null || filterField.getText().isEmpty())
                return null;
            return TreeItemPredicate.create(actor -> actor.toString().contains(filterField.getText()));
        }, filterField.textProperty()));
    }
	
}
