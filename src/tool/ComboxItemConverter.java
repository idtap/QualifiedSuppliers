package tool;

import java.util.HashMap;
import java.util.Map;

import javafx.util.StringConverter;

public class ComboxItemConverter extends StringConverter<ComboxItem> {

    /** Cache of Products */
    private Map<String, ComboxItem> productMap = new HashMap<String, ComboxItem>();

    @Override
    public String toString(ComboxItem item) {
        productMap.put(item.getDisplay(), item);
        return item.getDisplay();
    }

    @Override
    public ComboxItem fromString(String display) {
        return productMap.get(display);
    }

}