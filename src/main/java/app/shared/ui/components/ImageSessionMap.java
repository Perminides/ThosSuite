package app.shared.ui.components;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import app.shared.model.ShapeGeometry;
import app.shared.skin.SkinService;
import javafx.scene.Node;

/**
 * {@link SessionMap} auf Basis einer {@link ImageMapPane} — Welt, Hannover und Verwandte.
 *
 * <p>Die Bild-Karte adressiert über Geometrien, nicht über Ids. Die Übersetzung
 * {@code ids → Geometrien} kommt als Funktion herein, weil sie im Feature wohnt
 * ({@code GeoMap.geometryFor}) — diese Klasse kennt weder {@code GeoMap} noch {@code Deck}.</p>
 *
 * <p>Die vier Bildpfade und die Overlay-Maße holt sie sich selbst beim Skin: das ist abwärts und
 * spart dem Feature die Auflösung.</p>
 */
public class ImageSessionMap implements SessionMap {

	private final ImageMapPane pane;
	private final Function<Set<String>, List<ShapeGeometry>> geometrien;

	public ImageSessionMap(String deckId, String mapName,
			Function<Set<String>, List<ShapeGeometry>> geometrien,
			Consumer<String> onClicked) {
		this.geometrien = geometrien;

		var skin = SkinService.get();
		this.pane = new ImageMapPane(
				skin.getMapImagePath(mapName),
				skin.getMapOverlayImagePath(mapName),
				skin.getMapInactiveImagePath(mapName),
				skin.getMapInactiveOverlayImagePath(mapName),
				skin.getOverlayContentBounds(deckId));

		// TODO: Aus ImageMapSessionPane übernommen — „Ich verstehe nicht, was hier passiert. Muss
		//   diese Zeile wirklich hin?" Unverändert mitgezogen, damit der Umzug nichts ändert.
		pane.setViewportClip(skin.applyImageMapLayout(pane, deckId));
		pane.center(); // jetzt steht die Größe
		pane.setListener(onClicked);
	}

	@Override public void reset()                        { pane.resetMarkers(); }
	@Override public void setActive(boolean active)      { pane.setActive(active); }
	@Override public void markCorrect(Set<String> ids)   { pane.addToCorrect(geometrien.apply(ids)); }
	@Override public void mark(Set<String> ids)          { pane.setMarked(geometrien.apply(ids)); }
	@Override public void markInQuestion(Set<String> ids){ pane.setToCheckShapes(geometrien.apply(ids)); }

	/**
	 * Die Bild-Karte kennt die falsche Form nicht per id — sie merkt sich den letzten Klick selbst.
	 *
	 * <p>TODO: Aus der alten Pane übernommen, inklusive Marker: „EM 2021 — alle Länder grün, welches
	 *   war falsch?" Das Zurücksetzen davor löscht die bisherigen Markierungen mit.</p>
	 */
	@Override public void markIncorrect(String id) {
		pane.resetMarkers();
		pane.markLastClickAsIncorrect();
	}

	@Override public Node getView() { return pane.getView(); }
}
