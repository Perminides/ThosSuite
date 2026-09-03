package app.shared.ui.components;

import java.nio.file.Path;

import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

/**
 * Der Hintergrund eines Spielfelds: ein Bild, zentriert, ohne Wiederholung, skaliert bis es passt
 * und die Fläche füllt.
 *
 * <p>Welche Datei es ist, entscheidet der Skin. <em>Wie</em> sie sitzt, ist für jedes Spielfeld
 * gleich und steht deshalb hier — es ist keine Skin-Frage.</p>
 *
 * <p><b>Warum eine Fabrik und keine Unterklasse:</b> {@code javafx.scene.layout.Background} und
 * {@code BackgroundImage} sind beide {@code final} — die ganze Wertfamilie des layout-Pakets ist
 * versiegelt. Erben geht also nicht, und Verhüllen brächte nichts: {@code Region.setBackground}
 * nimmt ausschließlich ein {@code Background}, der Typ müsste an jeder Aufrufstelle wieder
 * ausgepackt werden. Also stellt diese Klasse den fertigen Wert her, und der Aufrufer setzt ihn.</p>
 */
public final class SuiteBackground {

	private SuiteBackground() {
	}

	/** Der fertige Hintergrund zu dieser Bilddatei. */
	public static Background of(Path wallpaper) {
		try {
			BackgroundImage image = new BackgroundImage(
					new Image(wallpaper.toUri().toString()),
					BackgroundRepeat.NO_REPEAT,
					BackgroundRepeat.NO_REPEAT,
					BackgroundPosition.CENTER,
					new BackgroundSize(
							BackgroundSize.AUTO,
							BackgroundSize.AUTO,
							false,
							false,
							true,  // contain: skaliert zum Reinpassen, ohne die Proportionen zu ändern
							true   // cover:   füllt alles aus, notfalls gestreckt
					));
			return new Background(image);
		} catch (Exception e) {
			throw new RuntimeException("Konnte Hintergrundbild nicht laden: " + wallpaper, e);
		}
	}
}
