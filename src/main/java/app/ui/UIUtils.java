package app.ui;

public class UIUtils {
    
    public static String toHex(javafx.scene.paint.Color c) {
        if (c == null) return "#000000"; // Fallback
        
        int r = (int) (Math.round(c.getRed() * 255));
        int g = (int) (Math.round(c.getGreen() * 255));
        int b = (int) (Math.round(c.getBlue() * 255));
        
        // Wenn die Farbe nicht voll deckend ist (Opacity < 1.0),
        // fügen wir den Alpha-Kanal hinten an (#RRGGBBAA)
        if (c.getOpacity() < 1.0) {
            int a = (int) (Math.round(c.getOpacity() * 255));
            return String.format("#%02X%02X%02X%02X", r, g, b, a);
        } else {
            // Sonst reicht der Standard-Hex (#RRGGBB)
            return String.format("#%02X%02X%02X", r, g, b);
        }
    }
	
}
