package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import javafx.application.Platform;
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

import main.milSymbolCheckboxTreeView;
import main.perferCode;

public class milSymbolEditorController implements Initializable {

	public Stage _stage_main;
	private milSymbolCheckboxTreeView milSymbolTreeView;
	private sidcEditorController sidcEditor;

	@FXML
	private Button btnOpen;

	@FXML
	private Button btnSave1;

	@FXML
	private Button btnSaveAs;

	@FXML
	private Label txtFilepath;

	@FXML
	private AnchorPane milTreeView;

	@FXML
	private AnchorPane sidcPane;

	@FXML
	void handlebtnOpenClicked(ActionEvent event) {
		try {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File("."));
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

			File selectedFile = fileChooser.showOpenDialog(_stage_main);
			if (selectedFile == null)
				return;

			ArrayList<String> ret = perferCode.parse(selectedFile.getAbsolutePath());
			milSymbolTreeView.setAllChecked(ret);
			txtFilepath.setText(selectedFile.getAbsolutePath());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	void handlebtnSaveAsClicked(ActionEvent event) {
		SaveAsPrefers();
	}

	@FXML
	void handlebtnSaveClicked(ActionEvent event) {
		SavePrefers(txtFilepath.getText());
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		setUserComponents();
	}

	private void SavePrefers(String file) {
		if (file.equals(""))
			SaveAsPrefers();
		else
			milSymbolTreeView.writeCheckItemToXMLfile(file);
	}

	private void SaveAsPrefers() {
		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File("."));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

		File selectedFile = fileChooser.showSaveDialog(_stage_main);
		if (selectedFile == null)
			return;

		milSymbolTreeView.writeCheckItemToXMLfile(selectedFile.getAbsolutePath());
		txtFilepath.setText(selectedFile.getAbsolutePath());
	}

	private void setUserComponents() {
		try {
			// ¾ðª¬µ²ºc
			milSymbolTreeView = new milSymbolCheckboxTreeView();
			milSymbolTreeView.SymbolSelectedEvent.addListener("setSidcEditorSymbolID", (SIC) -> setSidcEditorSymbolID(SIC));
			AnchorPane.setTopAnchor(milSymbolTreeView.treeView, 0.0);
			AnchorPane.setBottomAnchor(milSymbolTreeView.treeView, 0.0);
			AnchorPane.setLeftAnchor(milSymbolTreeView.treeView, 0.0);
			AnchorPane.setRightAnchor(milSymbolTreeView.treeView, 0.0);
			milTreeView.getChildren().addAll(milSymbolTreeView.treeView);
			
			
			
			// ¥k°¼SIC³]©w
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

	public void setSidcEditorSymbolID(String SIC) {
		sidcEditor.SetSIC(SIC);
	}
}
