package app.shared.ui.components;

import java.nio.file.Path;

import app.shared.skin.SkinService;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import javafx.util.Duration;

/**
 * Ein Miniaturbild, das beim Überfahren das Original in voller Größe zeigt.
 *
 * <p>Kennt kein Feature — es bekommt zwei Pfade und eine Höhe und macht daraus ein Bild mit
 * Vergrößerung. Genutzt wird es heute vom Tagebuch; die Filmkacheln haben denselben Bedarf,
 * dort liegen die Poster ebenfalls klein und groß nebeneinander.</p>
 *
 * <p>Die Höhe kommt von außen — sie hängt davon ab, wo das Bild eingebaut wird. Den Abstand, den
 * das Popup zum Bildschirmrand hält, holt sich der Baustein dagegen selbst beim Skin: der ist für
 * jede Verwendung derselbe.</p>
 */
public class SuiteThumbnail extends ImageView {

	private final double popupMargin = SkinService.get().popupMonitorMargin();
	private final Tooltip popup = new Tooltip();
	private final Path original;

	/**
	 * @param thumbnail Pfad des kleinen Bildes, das in der Fläche steht.
	 * @param original  Pfad des Originals, das beim Überfahren erscheint.
	 * @param height    Höhe des Miniaturbildes; die Breite folgt dem Seitenverhältnis.
	 */
	public SuiteThumbnail(Path thumbnail, Path original, double height) {
		super(new Image(thumbnail.toUri().toString(), -1, height, true, true));
		this.original = original;

		popup.setShowDelay(Duration.millis(300));
		popup.setShowDuration(Duration.INDEFINITE);
		popup.setHideDelay(Duration.ZERO);
		popup.setAutoFix(false);
		popup.setStyle("-fx-padding: 0;");

		setOnMouseEntered(e -> showEnlarged(e.getScreenX(), e.getScreenY()));
		setOnMouseExited(_ -> popup.hide());
	}

	/**
	 * Sucht die größte Fläche für das Originalbild: erst der bessere Halbraum links/rechts, dann
	 * der bessere oben/unten, dann gewinnt der mit der größeren Bildfläche.
	 */
	private void showEnlarged(double mouseX, double mouseY) {
		Rectangle2D screen = Screen.getPrimary().getVisualBounds();

		// Bild einmal laden
		Image image = new Image(original.toUri().toString());
		double naturalW = image.getWidth();
		double naturalH = image.getHeight();
		double aspectRatio = naturalW / naturalH;

		// Schritt 1: Gewinner links/rechts
		double leftW = mouseX - screen.getMinX() - 2 * popupMargin;
		double rightW = screen.getMaxX() - mouseX - 2 * popupMargin;
		double hemiH = screen.getHeight() - 2 * popupMargin;

		boolean useRight = rightW >= leftW;
		double hWinner_W = useRight ? rightW : leftW;

		double hLR_imgW, hLR_imgH;
		if (naturalW <= hWinner_W && naturalH <= hemiH) {
			hLR_imgW = naturalW; hLR_imgH = naturalH;
		} else if (naturalW / hWinner_W >= naturalH / hemiH) {
			hLR_imgW = hWinner_W; hLR_imgH = hWinner_W / aspectRatio;
		} else {
			hLR_imgH = hemiH; hLR_imgW = hemiH * aspectRatio;
		}
		double areaLR = hLR_imgW * hLR_imgH;

		// Schritt 2: Gewinner oben/unten
		double topH = mouseY - screen.getMinY() - 2 * popupMargin;
		double botH = screen.getMaxY() - mouseY - 2 * popupMargin;
		double hemiW = screen.getWidth() - 2 * popupMargin;

		boolean useBottom = botH >= topH;
		double hWinner_H = useBottom ? botH : topH;

		double hTB_imgW, hTB_imgH;
		if (naturalW <= hemiW && naturalH <= hWinner_H) {
			hTB_imgW = naturalW; hTB_imgH = naturalH;
		} else if (naturalW / hemiW >= naturalH / hWinner_H) {
			hTB_imgW = hemiW; hTB_imgH = hemiW / aspectRatio;
		} else {
			hTB_imgH = hWinner_H; hTB_imgW = hWinner_H * aspectRatio;
		}
		double areaTB = hTB_imgW * hTB_imgH;

		// Schritt 3: Finale Entscheidung
		double imgW, imgH, popupX, popupY;

		if (areaLR >= areaTB) {
			imgW = hLR_imgW; imgH = hLR_imgH;
			popupX = useRight ? mouseX + popupMargin : mouseX - popupMargin - imgW;
			popupY = mouseY - imgH / 2.0;
			popupY = Math.max(screen.getMinY() + popupMargin, popupY);
			popupY = Math.min(screen.getMaxY() - popupMargin - imgH, popupY);
		} else {
			imgW = hTB_imgW; imgH = hTB_imgH;
			popupY = useBottom ? mouseY + popupMargin : mouseY - popupMargin - imgH;
			popupX = mouseX - imgW / 2.0;
			popupX = Math.max(screen.getMinX() + popupMargin, popupX);
			popupX = Math.min(screen.getMaxX() - popupMargin - imgW, popupX);
		}

		// ImageView mit Originalbild, bei Bedarf via setFitWidth/setFitHeight skaliert
		ImageView largeImage = new ImageView(image);
		if (naturalW > imgW || naturalH > imgH) {
			largeImage.setFitWidth(imgW);
			largeImage.setFitHeight(imgH);
			largeImage.setPreserveRatio(true);
			largeImage.setSmooth(true);
		}

		popup.setGraphic(largeImage);
		popup.show(this, popupX, popupY);
	}
}
