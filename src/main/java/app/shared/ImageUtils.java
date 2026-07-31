package app.shared;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

/**
 * Rechnen auf Bildern: skalieren, umwandeln, ausmessen.
 *
 * <p>Alles hier arbeitet auf {@link BufferedImage} — dem Bildtyp aus Java2D, nicht dem von JavaFX.
 * Es kommt nichts auf den Bildschirm; diese Methoden dienen dem, was auf die Platte geschrieben
 * wird. Genau darin liegt der Unterschied zu {@link UiUtils}, wo alles einen JavaFX-Typ nimmt oder
 * liefert.</p>
 *
 * <p>Zwei Skalierverfahren, weil der Lern-Bildimport sie nebeneinanderstellt und den Benutzer
 * wählen lässt: {@link #scaleSmooth} über die Bibliothek imgscalr, {@link #scaleStepwise} als
 * Handarbeit über wiederholtes Halbieren.</p>
 */
public class ImageUtils {

	/**
	 * Skaliert über imgscalr in das angegebene Rechteck; das Seitenverhältnis bleibt erhalten
	 * ({@code Mode.AUTOMATIC}), das Bild wird also einbeschrieben und nicht verzerrt.
	 *
	 * @param methode Qualitätsstufe von imgscalr — je höher, desto langsamer und schöner.
	 * @param ops     Optionale Nachbearbeitung, etwa {@code Scalr.OP_ANTIALIAS}. Achtung: die kann
	 *                ein Bild mit Alpha-Kanal zurückgeben, auch wenn die Quelle keinen hatte —
	 *                siehe {@link #toRgb(BufferedImage)}.
	 */
	public static BufferedImage scaleSmooth(BufferedImage img, int breite, int hoehe, Scalr.Method methode, BufferedImageOp... ops) {
		return Scalr.resize(img, methode, Scalr.Mode.AUTOMATIC, breite, hoehe, ops);
	}

	/**
	 * Skaliert von Hand: erst in Zweierschritten grob heran (Nearest Neighbor, schnell), dann
	 * einmal genau auf Maß (bilinear).
	 *
	 * <p>Quelle des Verfahrens: <a href="https://www.locked.de/fast-image-scaling-in-java/">
	 * locked.de</a>. Wenn die Ergebnisse nicht gut sind, probiere es halt mal mit JavaXT z. B.</p>
	 */
	public static BufferedImage scaleStepwise(BufferedImage img, int breite, int hoehe) {
		Dimension ziel = new Dimension(breite, hoehe);
		return scaleExact(scaleByHalf(img, ziel), ziel);
	}

	/**
	 * Wandelt ein Bild in reines RGB ohne Alpha-Kanal.
	 *
	 * <p>Nötig vor dem Schreiben als JPEG: der JPEG-Schreiber von ImageIO verträgt nur drei Kanäle
	 * und liefert bei einem Bild mit Alpha-Kanal entweder eine Ausnahme oder die berüchtigten rosa
	 * eingefärbten Bilder. So ein Alpha-Kanal entsteht auch bei einer JPEG-Quelle, sobald beim
	 * Skalieren {@code Scalr.OP_ANTIALIAS} im Spiel war.</p>
	 *
	 * <p>Das Weiß ist dabei nur die Farbe, die vormals durchsichtige Pixel bekommen. Bei einer
	 * JPEG-Quelle gibt es keine, es ist also nie zu sehen — es geht um die Umwandlung, nicht um
	 * einen weißen Hintergrund.</p>
	 */
	public static BufferedImage toRgb(BufferedImage img) {
		BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgb.createGraphics();
		g.drawImage(img, 0, 0, Color.WHITE, null);
		g.dispose();
		return rgb;
	}

	/** Breite und Höhe eines Bildes aus den Rohdaten, als {@code int[]{breite, höhe}}. */
	public static int[] dimensions(byte[] imageData) {
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
			return new int[]{img.getWidth(), img.getHeight()};
		} catch (Exception e) {
			throw new RuntimeException("Abmessungen eines Bildes konnten nicht gelesen werden", e);
		}
	}

	// --- Handarbeit: die beiden Stufen von scaleStepwise ---

	private static BufferedImage scaleByHalf(BufferedImage img, Dimension d) {
		int w = img.getWidth();
		int h = img.getHeight();
		float factor = getBinFactor(w, h, d);

		// make new size
		w *= factor;
		h *= factor;
		BufferedImage scaled = new BufferedImage(w, h, img.getType());
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(img, 0, 0, w, h, null);
		g.dispose();
		return scaled;
	}

	private static BufferedImage scaleExact(BufferedImage img, Dimension d) {
		float factor = getFactor(img.getWidth(), img.getHeight(), d);

		// create the image
		int w = Math.round(img.getWidth() * factor);
		int h = Math.round(img.getHeight() * factor);
		BufferedImage scaled = new BufferedImage(w, h, img.getType());

		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, w, h, null);
		g.dispose();
		return scaled;
	}

	private static float getBinFactor(int width, int height, Dimension dim) {
		float factor = 1;
		float target = getFactor(width, height, dim);
		if (target <= 1) {
			while (factor / 2 > target) { factor /= 2; }
		} else {
			while (factor * 2 < target) { factor *= 2; }
		}
		return factor;
	}

	private static float getFactor(int width, int height, Dimension dim) {
		float sx = dim.width / (float) width;
		float sy = dim.height / (float) height;
		return Math.min(sx, sy);
	}
}
