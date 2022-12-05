package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import tool.Event;

//this.item.setExpanded(true);

public class milSymbolCheckboxTreeView {

	@FunctionalInterface
	public interface SymbolSelectedEventHandler {
		void invoke(String SIC);
	}

	/** 威脅情資更新通知 */
	public static Event<SymbolSelectedEventHandler> SymbolSelectedEvent = new Event<SymbolSelectedEventHandler>();

	private void RaiseSymbolSelectedEvent(String SIC) {
		// invoke all listeners:
		for (SymbolSelectedEventHandler listener : SymbolSelectedEvent.listeners()) {
			listener.invoke(SIC);
		}
	}

	public TreeView<String> treeView;

	/**  */
	public HashMap<String, milSymbolCode> milSymbol = new HashMap<String, milSymbolCode>();

	/** 當使用者點選TreeView Item */
	public void handleSelectedEvent(TreeItem<String> selectedItem) {
		if (!selectedItem.isLeaf())
			return;

		String key = (selectedItem.getParent().getParent() != null ? selectedItem.getParent().getParent().getValue()
				: "") + selectedItem.getParent().getValue() + selectedItem.getValue();
		if (milSymbol.containsKey(key)) {
			milSymbolCode attr = milSymbol.get(key);
			String SIC = attr.getCode();
			RaiseSymbolSelectedEvent(SIC);
		}
	}

	/** 讀取FX.XML(2525B清單檔) */
	public TreeItem<String> readData(File file) throws SAXException, ParserConfigurationException, IOException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		TreeItemCreationContentHandler contentHandler = new TreeItemCreationContentHandler();

		// parse file using the content handler to create a TreeItem representation
		reader.setContentHandler(contentHandler);
		reader.parse(file.toURI().toString());

		// use first child as root (the TreeItem initially created does not contain data
		// from the file)
		TreeItem<String> item = contentHandler.item.getChildren().get(0);
		contentHandler.item.getChildren().clear();
		return item;
	}

	public void setAllChecked(ArrayList<String> codes) {
		findItemsAndChecked((CheckBoxTreeItem<String>) treeView.getRoot(), codes);
	}

	private void findItemsAndChecked(CheckBoxTreeItem<String> item, ArrayList<String> codes) {
		if (item.isLeaf()) {
			String key = (item.getParent().getParent() != null ? item.getParent().getParent().getValue() : "")
					+ item.getParent().getValue() + item.getValue();
			if (milSymbol.containsKey(key)) {
				milSymbolCode attr = milSymbol.get(key);
				if (codes.contains(attr.getCode()))
					item.setSelected(true);
				else
					item.setSelected(false);
			}
		}
		for (TreeItem<String> child : item.getChildren()) {
			findItemsAndChecked((CheckBoxTreeItem<String>) child, codes);
		}
	}

	public void writeCheckItemToXMLfile(String file) {
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		XMLStreamWriter xsw = null;
		try {
			xsw =  xof.createXMLStreamWriter(new FileOutputStream(file), "UTF-8");
			xsw.writeStartDocument("UTF-8", "1.0");
			checkedItemsToXml((CheckBoxTreeItem<String>) treeView.getRoot(), xsw);

		} catch (Exception e) {
			System.err.println("Unable to write the file: " + e.getMessage());
		} finally {
			try {
				if (xsw != null) {
					xsw.close();
				}
			} catch (Exception e) {
				System.err.println("Unable to close the file: " + e.getMessage());
			}
		}
	}

	private void checkedItemsToXml(CheckBoxTreeItem<String> item, XMLStreamWriter xsw) {

		try {
			if (item.isIndeterminate() || item.isSelected()) {
				if (item.getValue().equals("ForceElement"))
					xsw.writeStartElement("TreeView");
				else
					xsw.writeStartElement("TreeNode");
				xsw.writeAttribute("text", item.getValue());
				if (item.isSelected() && item.isLeaf()) {
					String key =(item.getParent().getParent() != null ? item.getParent().getParent().getValue() : "") + item.getParent().getValue() + item.getValue();
					if (milSymbol.containsKey(key)) {
						milSymbolCode attr = milSymbol.get(key);
						xsw.writeAttribute("name", attr.getCode());
						xsw.writeAttribute("shape", attr.getShape());
					}
				}
			}
			
			for (TreeItem<String> child : item.getChildren()) {
				checkedItemsToXml((CheckBoxTreeItem<String>) child, xsw);
			}

			if (item.isIndeterminate() || item.isSelected())
				xsw.writeEndElement();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void drawTreeView() {
		try {
			String path = getClass().getResource("/xml/FE.xml").getPath();
			TreeItem<String> rootXML = readData(new File(path));
			rootXML.setExpanded(true);

			treeView = new TreeView<>(rootXML);
			treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
				@Override
				public void changed(@SuppressWarnings("rawtypes") ObservableValue observable, Object oldValue,
						Object newValue) {

					TreeItem<String> selectedItem = (TreeItem<String>) newValue;
					handleSelectedEvent(selectedItem);
				}
			});

			treeView.setEditable(true);
			treeView.setRoot(rootXML);
			treeView.setShowRoot(true);

			// set the cell factory(需設定否則看不到checkbox)
			treeView.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public milSymbolCheckboxTreeView() {
		drawTreeView();
	}

	private class TreeItemCreationContentHandler extends DefaultHandler {

		private CheckBoxTreeItem<String> item = new CheckBoxTreeItem<String>();

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			// finish this node by going back to the parent
			this.item = (CheckBoxTreeItem<String>) this.item.getParent();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			// start a new node and use it as the current item
			String cname = attributes.getValue("text");
			CheckBoxTreeItem<String> item = new CheckBoxTreeItem<String>(cname);
			this.item.getChildren().add(item);
			this.item = item;

			item.selectedProperty().addListener((obs, oldVal, newVal) -> {
				handleSelectedEvent((TreeItem<String>) item);
			});

			if (attributes.getLength() > 1) {
				String key = (item.getParent().getParent() != null ? item.getParent().getParent().getValue() : "")
						+ item.getParent().getValue() + cname;
				milSymbol.put(key, new milSymbolCode(cname, attributes.getValue("name"), attributes.getValue("shape")));
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			String s = String.valueOf(ch, start, length).trim();
			if (!s.isEmpty()) {
				// add text content as new child
				this.item.getChildren().add(new CheckBoxTreeItem<String>(s));
			}
		}

	}

}
