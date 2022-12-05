package main;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TreeItem;

public class milSymbolCode {

	/** ����W�� */
    private final SimpleStringProperty name;
    /** 2525B 15�X */
    private final SimpleStringProperty code;
    /** �Ϊ�(POINT�BLINE�BAREA) */
    private final SimpleStringProperty shape;

    public milSymbolCode(String name, String code,String shape) {
        this.name = new SimpleStringProperty(name);
        this.code = new SimpleStringProperty(code);
        this.shape = new SimpleStringProperty(shape);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String fName) {
        name.set(fName);
    }

    public String getCode() {
        return code.get();
    }

    public void setCode(String value) {
    	code.set(value);
    }

	public String getShape() {
		return shape.get();
	}
}