package app.ui;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public class UIUtils {
    
    /**
     * Konvertiert eine JavaFX-{@link javafx.scene.paint.Color} in einen CSS-kompatiblen Hex-String.
     * <p>
     * Ist die Farbe voll deckend, wird das Format {@code #RRGGBB} zurückgegeben.
     * Bei einem Opacity-Wert kleiner als 1.0 wird der Alpha-Kanal angehängt: {@code #RRGGBBAA}.
     * </p>
     *
     * @param c die umzuwandelnde Farbe; {@code null} wird als {@code #000000} (Schwarz) behandelt
     * @return Hex-Farb-String im Format {@code #RRGGBB} oder {@code #RRGGBBAA}
     */
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
    
	/**
	 * Ersetzt die Farbe jedes Pixels mit tintColor, wobei der Alpha-Wert unangetastet bleibt.
	 * Ist also super, um monochromatische Bilder einzufärben :-)
	 * 
	 * @param source
	 * @param tintColor
	 * @return
	 */
	public static Image tintImage(Image source, Color tintColor) {
		int width = (int) source.getWidth();
		int height = (int) source.getHeight();

		WritableImage result = new WritableImage(width, height);
		PixelReader reader = source.getPixelReader();
		PixelWriter writer = result.getPixelWriter();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color pixelColor = reader.getColor(x, y);
				Color newColor = new Color(tintColor.getRed(), tintColor.getGreen(), tintColor.getBlue(), pixelColor.getOpacity() // Alpha bleibt unverändert
				);
				writer.setColor(x, y, newColor);
			}
		}

		return result;
	}
	
	/**
	 * 
	 * @param c
	 * @param intensity:
	 *            an int value between 0 (no change) and 100 (maximal change will lead to white or black...)
	 * @return
	 */
	public static Color adjustBrightness(Color c, int intensity) {	    
	    if (intensity < 0 || intensity > 100) {
	        throw new RuntimeException("Das soll ein Prozentwert sein für die adjustBrightness, du Witzbold :)");
	    }

	    double intensityF = intensity / 100.0;
	    double threshold = 1.0 - intensityF;

	    double brightness = c.getBrightness();
	    double newBrightness;

	    if (brightness > threshold) {
	        newBrightness = Math.max(0.0, brightness - intensityF); // abdunkeln
	    } else {
	        newBrightness = Math.min(1.0, brightness + intensityF); // aufhellen
	    }

	    // Neue Farbe erstellen via HSB-Factory
	    // Wichtig: c.getOpacity() übernimmt den Alpha-Wert (0.0 - 1.0)
	    return Color.hsb(c.getHue(), c.getSaturation(), newBrightness, c.getOpacity());
	}
	
	public static void inactivateEscPress (Alert alert) {
		alert.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
		    if (event.getCode() == KeyCode.ESCAPE) {
		        event.consume();
		    }
		});
	}
}
