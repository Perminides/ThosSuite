package app.shared;

import java.io.File;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Window;


/**
 * Diverse JavaFx Bildmanipulationen für die Anzeige.
 * Manipulationen an Bild<b>dateien</b> findest Du in ImageUtils.</p>
 */
public class UiUtils {

    private static Window ownerWindow;

    /**
     * Setzt das Fenster, das allen Dialogen und Alerts als Owner dient. Wird einmal vom
     * {@code Controller} gesetzt, direkt nach {@code mainWindow.show()}.
     *
     * <p>Der Owner ist <b>Pflicht</b>, nicht Kosmetik: JavaFX bindet die Stylesheets der
     * Dialog-Scene per {@code Bindings.bindContent} an die Scene des Owners
     * ({@code HeavyweightDialog.updateStageBindings}). Ohne Owner bleibt ein Dialog ungestylt —
     * siehe die rohen Alerts in {@code ThosSuiteApp} und {@code DB}. Die Bindung ist live: ein
     * Skinwechsel wirkt auch auf bereits offene Dialoge.</p>
     *
     * <p>Liegt hier und nicht im {@code SkinService}, weil das Hauptfenster nichts mit Skinning zu
     * tun hat — es ist bloß der, der immer Parent ist. Etwas geschummelt ist es trotzdem.</p>
     */
    public static void setOwnerWindow(Window window) {
        ownerWindow = window;
    }

    public static Window getOwnerWindow() {
        return ownerWindow;
    }
    
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
	 * Ein Ton derselben Farbe, der sich sichtbar von ihr abhebt — für Flächen, die eine Variante ihrer
	 * selbst brauchen: Hover, Pressed, ein abgesetzter Track.
	 *
	 * <p><b>Die Richtung wählt die Methode, nicht der Aufrufer:</b> aufgehellt wird immer, außer das
	 * schösse über 1.0 hinaus — dann wird stattdessen abgedunkelt. Genau das macht sie skin-tauglich:
	 * auf heller Fläche wird der Hover dunkler, auf dunkler heller, ohne dass es jemand pro Skin
	 * entscheiden muss.</p>
	 *
	 * <p>Farbton und Sättigung bleiben unangetastet, es ändert sich allein die Helligkeit. Oberhalb
	 * von 50 kann der Abdunkel-Zweig auf 0 klemmen; das Ergebnis ist dann schlicht Schwarz.</p>
	 *
	 * <p><b>Nicht zum Dämpfen von Text geeignet.</b> Zurücktreten heißt sich dem Hintergrund annähern,
	 * und den kennt diese Methode nicht. Dafür {@code textColor.interpolate(hintergrund, 0.5)}.</p>
	 *
	 * @param c
	 * @param percent
	 *            an int value between 0 (no change) and 100 (maximal change will lead to white or black...)
	 * @return
	 */
	public static Color contrastingShade(Color c, int percent) {
	    if (percent < 0 || percent > 100) {
	        throw new RuntimeException("Das soll ein Prozentwert sein für die contrastingShade, du Witzbold :)");
	    }

	    double percentF = percent / 100.0;
	    double threshold = 1.0 - percentF;

	    double brightness = c.getBrightness();
	    double newBrightness;

	    if (brightness > threshold) {
	        newBrightness = Math.max(0.0, brightness - percentF); // abdunkeln
	    } else {
	        newBrightness = Math.min(1.0, brightness + percentF); // aufhellen
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
	
	/**
	 * Malt ein +-Zeichen unten rechts auf ein Bild.
	 */
	public static Image addPlusSign(Image source, int targetWidth) {
	    double scale = targetWidth / source.getWidth();
	    int w = targetWidth;
	    int h = (int)(source.getHeight() * scale);

	    javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(w, h);
	    javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
	    gc.drawImage(source, 0, 0, w, h);

	    // Badge-Icon laden und unten rechts draufmalen
	    String badgeFilename = "plus_" + targetWidth + ".png";
	    File badgeFile = Config.getPath("iconFolder").resolve(badgeFilename).toFile();
	    Image badge = new Image(badgeFile.toURI().toString());
	    gc.drawImage(badge, w - badge.getWidth() - 4, h - badge.getHeight() - 4);

	    javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
	    params.setFill(javafx.scene.paint.Color.TRANSPARENT);
	    return canvas.snapshot(params, null);
	}
	
}
