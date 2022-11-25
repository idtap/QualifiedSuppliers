package controller;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.symbology.ColorUtil;
import com.esri.arcgisruntime.symbology.DictionarySymbolStyle;
import com.esri.arcgisruntime.symbology.Symbol;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tool.ComboxItem;
import tool.ComboxItemConverter;

public class sidcEditorController implements Initializable {
	public Stage _stage_main;
	private DictionarySymbolStyle dictionarySymbol;

	private String _SIC = "SPGP-----------";
	private static Map<String, String> _Affiliation = new HashMap<>();
	private static Map<String, String> _Type = new HashMap<>();
	private static Map<String, String> _Echelon = new HashMap<>();
	private static Map<String, String> _Country = new HashMap<>();
	private static Map<String, String> _Battle = new HashMap<>();
	private ArrayList<TextField> _txtList = new ArrayList();

	@FXML
	private ComboBox cboAffiliation;

	@FXML
	private ComboBox cboBattle;

	@FXML
	private ComboBox cboCountry;

	@FXML
	private ComboBox cboEchelon;

	@FXML
	private ComboBox cboType;

	@FXML
	private RadioButton radPlanned;

	@FXML
	private RadioButton radPresent;

	@FXML
	private TextField txt01;

	@FXML
	private TextField txt02;

	@FXML
	private TextField txt03;

	@FXML
	private TextField txt04;

	@FXML
	private TextField txt05;

	@FXML
	private TextField txt06;

	@FXML
	private TextField txt07;

	@FXML
	private TextField txt08;

	@FXML
	private TextField txt09;

	@FXML
	private TextField txt10;

	@FXML
	private TextField txt11;

	@FXML
	private TextField txt12;

	@FXML
	private TextField txt13;

	@FXML
	private TextField txt14;

	@FXML
	private TextField txt15;

	@FXML
	private ImageView imgViewSymbol;

	@FXML
	void cboAffiliation_SelectedIndexChanged(ActionEvent event) {
		if ((ComboxItem) cboAffiliation.getSelectionModel().getSelectedItem() == null)
			return;

		String value = ((ComboxItem) cboAffiliation.getSelectionModel().getSelectedItem()).getCode();
		SetSIC(1, value);
	}

	@FXML
	void cboBattle_SelectedIndexChanged(ActionEvent event) {
		if ((ComboxItem) cboBattle.getSelectionModel().getSelectedItem() == null)
			return;

		String value = ((ComboxItem) cboBattle.getSelectionModel().getSelectedItem()).getCode();
		SetSIC(14, value);
	}

	@FXML
	void cboCountry_SelectedIndexChanged(ActionEvent event) {
		if ((ComboxItem) cboCountry.getSelectionModel().getSelectedItem() == null)
			return;

		String value = ((ComboxItem) cboCountry.getSelectionModel().getSelectedItem()).getCode();
		SetSIC(12, value);
	}

	@FXML
	void cboEchelon_SelectedIndexChanged(ActionEvent event) {
		if ((ComboxItem) cboEchelon.getSelectionModel().getSelectedItem() == null)
			return;

		String value = ((ComboxItem) cboEchelon.getSelectionModel().getSelectedItem()).getCode();
		SetSIC(10, value);
	}

	@FXML
	void cboType_SelectedIndexChanged(ActionEvent event) {
		if ((ComboxItem) cboType.getSelectionModel().getSelectedItem() == null)
			return;

		String value = ((ComboxItem) cboType.getSelectionModel().getSelectedItem()).getCode();
		SetSIC(10, value);
	}

	private void initData() {
		_Affiliation.put("待查證", "P");
		_Affiliation.put("不明", "U");
		_Affiliation.put("擬友", "A");
		_Affiliation.put("我(友)軍", "F");
		_Affiliation.put("中立", "N");
		_Affiliation.put("擬敵", "S");
		_Affiliation.put("敵軍", "H");
		_Affiliation.put("演習待查證", "G");
		_Affiliation.put("演習不明", "W");
		_Affiliation.put("演習擬友", "M");
		_Affiliation.put("演習我(友)軍", "D");
		_Affiliation.put("演習中立", "L");
		_Affiliation.put("演習擬敵", "J");
		_Affiliation.put("演習敵軍", "K");

		_Type.put("無編制編組記號", "-");
		_Type.put("藍軍-指揮部", "A");
		_Type.put("藍軍-特遣部隊", "E");
		_Type.put("藍軍-特遣部隊-指揮部", "B");
		_Type.put("敵軍-虛設", "F");
		_Type.put("敵軍-建制部隊-虛設", "C");
		_Type.put("敵軍-特遣部隊", "G");
		_Type.put("敵軍-指揮部-虛設", "D");

		_Echelon.put("無編制", "--");
		_Echelon.put("伍級", "-A");
		_Echelon.put("班級", "-B");
		_Echelon.put("組級", "-C");
		_Echelon.put("排級", "-D");
		_Echelon.put("連級", "-E");
		_Echelon.put("營級", "-F");
		_Echelon.put("團或群級", "-G");
		_Echelon.put("旅級", "-H");
		_Echelon.put("師級", "-I");
		_Echelon.put("軍級", "-J");
		_Echelon.put("軍團(作戰區)級", "-K");
		_Echelon.put("集團軍級", "-L");
		_Echelon.put("戰區級", "-M");
		_Echelon.put("輪型機動或有限越野", "MO");
		_Echelon.put("越野", "MP");
		_Echelon.put("機動履帶", "MQ");
		_Echelon.put("輪型及履帶機動", "MR");
		_Echelon.put("機動牽引", "MS");
		_Echelon.put("機動鐵軌", "MT");
		_Echelon.put("機動雪車", "MU");
		_Echelon.put("機動雪橇", "MV");
		_Echelon.put("機動馱獸", "MW");
		_Echelon.put("機動駁船", "MX");
		_Echelon.put("機動兩棲", "MY");

		_Country.put("未知", "--");
		_Country.put("澳大利亞", "AU");
		_Country.put("中國大陸", "CN");
		_Country.put("日本", "JP");
		_Country.put("北韓", "KP");
		_Country.put("南韓", "KR");
		_Country.put("菲律賓", "PH");
		_Country.put("俄羅斯", "RU");
		_Country.put("中華民國(台灣)", "TW");
		_Country.put("美國", "US");
		_Country.put("越南", "VN");
		_Country.put("新加坡", "SG");
		_Country.put("待查証", "XX");

		_Battle.put("未指定", "-");
		_Battle.put("空中部隊", "A");
		_Battle.put("電戰部隊", "E");
		_Battle.put("民兵部隊", "C");
		_Battle.put("地面部隊", "G");
		_Battle.put("海上部隊", "N");
		_Battle.put("戰略部隊", "S");
	}

	@SuppressWarnings("unchecked")
	private void initComponent() {
		_txtList.add(txt01);
		_txtList.add(txt02);
		_txtList.add(txt03);
		_txtList.add(txt04);
		_txtList.add(txt05);
		_txtList.add(txt06);
		_txtList.add(txt07);
		_txtList.add(txt08);
		_txtList.add(txt09);
		_txtList.add(txt10);
		_txtList.add(txt11);
		_txtList.add(txt12);
		_txtList.add(txt13);
		_txtList.add(txt14);
		_txtList.add(txt15);

		ToggleGroup tg = new ToggleGroup();
		radPlanned.setToggleGroup(tg);
		radPresent.setToggleGroup(tg);

		tg.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> changed, Toggle oldVal, Toggle newVal) {
				// Cast new to RadioButton.
				RadioButton rb = (RadioButton) newVal;
				if (radPlanned.isSelected()) {
					SetSIC(3, "A");
				} else if (radPresent.isSelected()) {
					SetSIC(3, "P");
				}
			}
		});

		cboAffiliation.setConverter(new ComboxItemConverter());
		for (String p : _Affiliation.keySet()) {
			ComboxItem c = new ComboxItem(_Affiliation.get(p), p);
			cboAffiliation.getItems().add(c);
		}

		cboType.setConverter(new ComboxItemConverter());
		for (String p : _Type.keySet()) {
			ComboxItem c = new ComboxItem(_Type.get(p), p);
			cboType.getItems().add(c);
		}

		cboEchelon.setConverter(new ComboxItemConverter());
		for (String p : _Echelon.keySet()) {
			ComboxItem c = new ComboxItem(_Echelon.get(p), p);
			cboEchelon.getItems().add(c);
		}

		cboCountry.setConverter(new ComboxItemConverter());
		for (String p : _Country.keySet()) {
			ComboxItem c = new ComboxItem(_Country.get(p), p);
			cboCountry.getItems().add(c);
		}

		cboBattle.setConverter(new ComboxItemConverter());
		for (String p : _Battle.keySet()) {
			ComboxItem c = new ComboxItem(_Battle.get(p), p);
			cboBattle.getItems().add(c);
		}

	}

	public void SetSIC(int Index, String Code) {
		for (int i = 0; i < Code.length(); i++) {
			TextField pTextBox = _txtList.get(Index + i);
			pTextBox.setText(Code.substring(i, i + 1));
		}

		 SetSICAsync(getSymbolID());
	}

	public String getSymbolID() {
		String SIC = "";
		for (TextField pTextBox : _txtList) {
			SIC += pTextBox.getText();
		}
		return SIC;
	}

	void SetSymbolID(String value) {
		int i = -1;
		String Code;
		for (TextField pTextBox : _txtList) {
			i++;
			Code = value.substring(i, i + 1);

			if ((i + 1) == 15 && (Code.equals("*") || Code.equals("X")))
				Code = "-";

			if (Code.equals("*"))
				continue;
			if (!pTextBox.getText().equals(Code)) {
//				pTextBox.setStyle("-fx-border-color: black;-fx-background-color: Lime;");
				pTextBox.setText(Code);
			}
//			else {
//				pTextBox.setStyle("-fx-border-color: black;-fx-background-color: white;");
//			}
		}
	}

	public void SetSICAsync(String sic) {

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sidc", sic);

		try {
			ListenableFuture<Symbol> symbolFuture = dictionarySymbol.getSymbolAsync(attributes);
			symbolFuture.addDoneListener(() -> {
				try {
					Symbol symbol = symbolFuture.get();
					if (symbol == null) {
						return;
					}

					ListenableFuture<Image> imgFuture = symbol
							.createSwatchAsync(ColorUtil.colorToArgb(Color.TRANSPARENT));
					imgFuture.addDoneListener(() -> {
						try {
							Image img = imgFuture.get();
							imgViewSymbol.setFitHeight(150);
							imgViewSymbol.setFitWidth(150);
							imgViewSymbol.setImage(img);
						} catch (ExecutionException | InterruptedException e) {
							e.printStackTrace();
						}
					});

				} catch (ExecutionException | InterruptedException e) {
					e.printStackTrace();
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void SetModifiersSymbolID(String SIC) {
//		cboType.getSelectionModel().clearSelection();
//		cboEchelon.getSelectionModel().clearSelection();
//		cboCountry.getSelectionModel().clearSelection();
//		cboBattle.getSelectionModel().clearSelection();

		String Code;

		Code = SIC.substring(1, 2);
		if (!Code.equals("*")) {
			if (_Affiliation.containsValue(Code)) {
				for (int i = 0; i < cboAffiliation.getItems().size(); i++) {
					ComboxItem c = (ComboxItem) cboAffiliation.getItems().get(i);
					if (c.getCode().equals(Code)) {
						cboAffiliation.getSelectionModel().select(i);
						break;
					}
				}
			}
		}

		Code = SIC.substring(3, 4);
		if (Code.equals("P")) {
			radPresent.setSelected(true);
		} else if (Code.equals("A")) {
			radPlanned.setSelected(true);
		} else {
			radPresent.setSelected(true);
		}

		Code = SIC.substring(10, 12);
		if (!Code.equals("**")) {
			if (_Echelon.containsValue(Code)) {
				for (int i = 0; i < cboEchelon.getItems().size(); i++) {
					ComboxItem c = (ComboxItem) cboEchelon.getItems().get(i);
					if (c.getCode().equals(Code)) {
						cboType.getSelectionModel().select(0);
						cboEchelon.getSelectionModel().select(i);
						break;
					}
				}
			} else {
				String Code10 = Code.substring(0, 1);
				Code = "-" + Code10;
				if (_Echelon.containsValue(Code)) {
					for (int i = 0; i < cboEchelon.getItems().size(); i++) {
						ComboxItem c = (ComboxItem) cboEchelon.getItems().get(i);
						if (c.getCode().equals(Code)) {
							cboEchelon.getSelectionModel().select(i);
							break;
						}
					}
				}

				if (_Type.containsValue(Code10)) {
					for (int i = 0; i < cboType.getItems().size(); i++) {
						ComboxItem c = (ComboxItem) cboType.getItems().get(i);
						if (c.getCode().equals(Code10)) {
							cboType.getSelectionModel().select(i);
							break;
						}
					}
				}
			}
		}

		Code = SIC.substring(12, 14);
		if (!Code.equals("**")) {
			if (_Country.containsValue(Code)) {
				for (int i = 0; i < cboCountry.getItems().size(); i++) {
					ComboxItem c = (ComboxItem) cboCountry.getItems().get(i);
					if (c.getCode().equals(Code)) {
						cboCountry.getSelectionModel().select(i);
						break;
					}
				}
			}
		}

		Code = SIC.substring(14, 15);
		if (Code.equals("*") || Code.equals("X"))
			Code = "-";

		if (_Battle.containsValue(Code)) {
			for (int i = 0; i < cboBattle.getItems().size(); i++) {
				ComboxItem c = (ComboxItem) cboBattle.getItems().get(i);
				if (c.getCode().equals(Code)) {
					cboBattle.getSelectionModel().select(i);
					break;
				}
			}
		}

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		initData();
		initComponent();
		initDictionarySymbol();

		SetSymbolID(_SIC);
		SetModifiersSymbolID(_SIC);
		SetSICAsync(_SIC);
	}

	public void SetSIC(String SIC) {
		SetSymbolID(SIC);
		SetModifiersSymbolID(SIC);
		SetSICAsync(SIC);

	}

	private void initDictionarySymbol() {
		File stylxFile = new File(ArcGISRuntimeEnvironment.getResourcesDirectory() + "/symbols/mil2525c.stylx");
		dictionarySymbol = DictionarySymbolStyle.createFromFile(stylxFile.getAbsolutePath());
		dictionarySymbol.loadAsync();
	}

}
