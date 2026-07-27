package app.shared.ui;

import java.util.List;

import app.shared.model.SessionCallbacks;
import app.shared.model.ShapeGeometry;
import app.shared.ui.components.SessionMap;
import app.shared.ui.components.ShapeSessionMap;

/** Deutschland: Shape-Karte, Eingabefeld, eigenes Hintergrundbild des Decks. */
public class GermanySessionView extends AnkiSessionView {

	private final List<ShapeGeometry> geometrien;

	public GermanySessionView(String deckId, String mapName, String kategorie,
			List<ShapeGeometry> geometrien, SessionCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		this.geometrien = geometrien;
		rebuild(); // muss die letzte Zeile sein, siehe AnkiSessionView
	}

	@Override
	protected SessionMap createMap() {
		return new ShapeSessionMap(geometrien, mapName(), kategorie(), callbacks().mapElementClicked());
	}

	@Override protected boolean hasInputField()      { return true; }
	@Override protected boolean usesDeckBackground() { return false; }
}
