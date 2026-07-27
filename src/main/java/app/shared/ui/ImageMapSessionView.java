package app.shared.ui;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import app.shared.model.SessionCallbacks;
import app.shared.model.ShapeGeometry;
import app.shared.ui.components.ImageSessionMap;
import app.shared.ui.components.SessionMap;

/** Welt und Hannover: Bild-Karte, Eingabefeld, leerer Hintergrund. */
public class ImageMapSessionView extends AnkiSessionView {

	private final Function<Set<String>, List<ShapeGeometry>> geometrieFuer;

	public ImageMapSessionView(String deckId, String mapName, String kategorie,
			Function<Set<String>, List<ShapeGeometry>> geometrieFuer, SessionCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		this.geometrieFuer = geometrieFuer;
		rebuild(); // muss die letzte Zeile sein, siehe AnkiSessionView
	}

	@Override
	protected SessionMap createMap() {
		return new ImageSessionMap(deckId(), mapName(), geometrieFuer, callbacks().mapElementClicked());
	}

	@Override protected boolean hasInputField()      { return true; }
	@Override protected boolean usesDeckBackground() { return false; }
}
