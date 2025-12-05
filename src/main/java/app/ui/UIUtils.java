package app.ui;

import java.awt.Color;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;

public class UIUtils {

    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
    
    public static void setFlatStyleBackgroundColor(JComponent component, Color color) {
    	updateFlatLafStyle(component, Map.of("background", UIUtils.toHex(color)));
    }
    
    public static void removeFlatStyleHoverBackgroundChange(JComponent comp) {
    	copyStyleValue(comp, "background", "hoverBackground");
    }
    
    public static void copyStyleValue(JComponent component, String sourceKey, String targetKey) {
        String currentStyle = (String) component.getClientProperty("FlatLaf.style");
        if (currentStyle == null) return;
        
        // Finde Wert von sourceKey
        Pattern pattern = Pattern.compile(sourceKey + ":([^;]+)");
        Matcher matcher = pattern.matcher(currentStyle);
        
        if (matcher.find()) {
            String sourceValue = matcher.group(1);
            updateFlatLafStyle(component, Map.of(targetKey, sourceValue));
        }
    }
    
    /**
     * 
     * Erweitere updateFlatLafStyle (fügt hinzu ODER ändert)
     * 
     * Also dieses FlatLaf kommt schon mit einer Menge Einschränkungen. Wenn man das Design eines Elementes verändern will,
     * kann man nicht einfach setBackground aufrufen, sondern muss alle Style-Properties neu übergeben. Was für ein
     * wahnwitziges Handling...
     * 
     * @param component
     * @param updates
     */
    public static void updateFlatLafStyle(JComponent component, Map<String, String> updates) {
        String currentStyle = (String) component.getClientProperty("FlatLaf.style");
        if (currentStyle == null) currentStyle = "";
        
        String newStyle = currentStyle;
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String pattern = key + ":[^;]+;?";
            
            if (newStyle.matches(".*" + pattern + ".*")) {
                // Key existiert → ersetzen
                newStyle = newStyle.replaceAll(pattern, key + ":" + value + ";");
            } else {
                // Key existiert nicht → anhängen
                newStyle += key + ":" + value + ";";
            }
        }
        
        component.putClientProperty("FlatLaf.style", newStyle);
    }
	
}
