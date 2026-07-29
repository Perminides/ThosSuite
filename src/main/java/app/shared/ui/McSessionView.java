package app.shared.ui;

import app.shared.model.SessionCallbacks;
import app.shared.ui.components.map.NoSessionMap;
import app.shared.ui.components.map.SessionMap;

/**
 *  Multiple Choice: keine Karte, kein Eingabefeld, leerer Hintergrund. 
 *  Wenn Du dich fragst, warum man der McSessionView mitgeben muss, dass es die MCSession ist:
 *  Naja, schau halt bei Welt und Hannover: Theoretisch könnte es ja mal ein zweites MC-Anki-SessionDeck geben.
 *  Was weiß ich, eins nur für Flaggen oder so. Dann wäre das hier supi einfach zu machen...
 *  Ja, wird nicht passieren, aber bitte, da geht Konsistenz mal vor!
 **/
public class McSessionView extends AnkiSessionView {

	public McSessionView(String deckId, String mapName, String kategorie, SessionCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		rebuild(); // muss die letzte Zeile sein, siehe AnkiSessionView
	}

	@Override protected SessionMap createMap()       { return new NoSessionMap(); }
	@Override protected boolean hasInputField()      { return false; }
	@Override public void disableMcPanel() {}; // MCPanel darf in einer MC-Session niemals deaktiviert werden. Wird allerdings vermutlich eh nie aufgerufen.
}
