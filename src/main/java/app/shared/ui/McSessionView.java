package app.shared.ui;

import app.shared.model.SessionCallbacks;
import app.shared.ui.components.NoSessionMap;
import app.shared.ui.components.SessionMap;

/** Multiple Choice: keine Karte, kein Eingabefeld, leerer Hintergrund. */
public class McSessionView extends AnkiSessionView {

	public McSessionView(String deckId, String mapName, String kategorie, SessionCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		rebuild(); // muss die letzte Zeile sein, siehe AnkiSessionView
	}

	@Override protected SessionMap createMap()       { return new NoSessionMap(); }
	@Override protected boolean hasInputField()      { return false; }
	@Override protected boolean usesDeckBackground() { return true; }
	@Override public void disableMcPanel() {}; // MCPanel darf in einer MC-Session niemals deaktiviert werden. Wird allerdings vermutlich eh nie aufgerufen.
}
