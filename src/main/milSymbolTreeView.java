package main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import main.milSymbolCheckboxTreeView.SymbolSelectedEventHandler;
import tool.Event;

//this.item.setExpanded(true);

public class milSymbolTreeView {

	@FunctionalInterface
	public interface SymbolSelectedEventHandler {
		void invoke(String SIC, String Shape);
	}

	/** 威脅情資更新通知 */
	public static Event<SymbolSelectedEventHandler> SymbolSelectedEvent = new Event<SymbolSelectedEventHandler>();

	private void RaiseSymbolSelectedEvent(String SIC, String Shape) {
		// invoke all listeners:
		for (SymbolSelectedEventHandler listener : SymbolSelectedEvent.listeners()) {
			listener.invoke(SIC, Shape);
		}
	}

	public TreeView<String> treeView;

	public HashMap<String, milSymbolCode> milSymbol = new HashMap<String, milSymbolCode>();

	public void handleSelectedEvent(TreeItem<String> selectedItem) {
		if (selectedItem == null || !selectedItem.isLeaf())
			return;

		String key = (selectedItem.getParent().getParent() != null ? selectedItem.getParent().getParent().getValue()
				: "") + selectedItem.getParent().getValue() + selectedItem.getValue();
		if (milSymbol.containsKey(key)) {
			milSymbolCode attr = milSymbol.get(key);
			RaiseSymbolSelectedEvent(attr.getCode(), attr.getShape());
		}
	}

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

	ArrayList<String> codesLimit = null;

	@SuppressWarnings("unchecked")
	private void drawTreeView(File file) {
		try {
			
			TreeItem<String> rootXML = readData(file);
			rootXML.setExpanded(true);

			if (treeView == null)
				treeView = new TreeView<>(rootXML);
			else
				treeView.setRoot(rootXML);
			treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
				@Override
				public void changed(@SuppressWarnings("rawtypes") ObservableValue observable, Object oldValue,
						Object newValue) {

					TreeItem<String> selectedItem = (TreeItem<String>) newValue;
					handleSelectedEvent(selectedItem);
				}
			});

			treeView.setShowRoot(true);
			treeView.refresh();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public milSymbolTreeView() {
		
		File file = new File(System.getProperty("data.dir"),
				"./samples-data/xml/FE.xml");

		drawTreeView(file);
	}

	private class TreeItemCreationContentHandler extends DefaultHandler {

		private TreeItem<String> item = new TreeItem<String>();

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			// finish this node by going back to the parent
			this.item = (TreeItem<String>) this.item.getParent();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			// start a new node and use it as the current item
			String cname = attributes.getValue("text");
			TreeItem<String> item = new TreeItem<String>(cname);
			this.item.getChildren().add(item);
			this.item = item;

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
				this.item.getChildren().add(new TreeItem<String>(s));
			}
		}

	}

}
