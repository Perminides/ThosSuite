package app.shared.ui;

import app.shared.model.LearnCallbacks;
import app.shared.ui.components.map.EmptyLearnMap;
import app.shared.ui.components.map.LearnMap;

/**
 *  Multiple Choice: keine Karte, kein Eingabefeld, leerer Hintergrund. 
 *  Wenn Du dich fragst, warum man der McLearnView mitgeben muss, dass es die MCSession ist:
 *  Naja, schau halt bei Welt und Hannover: Theoretisch könnte es ja mal ein zweites MC-Anki-SessionDeck geben.
 *  Was weiß ich, eins nur für Flaggen oder so. Dann wäre das hier supi einfach zu machen...
 *  Ja, wird nicht passieren, aber bitte, da geht Konsistenz mal vor!
 **/
public class McLearnView extends AnkiLearnView {

	public McLearnView(String deckId, String mapName, String kategorie, LearnCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		rebuild(); // muss die letzte Zeile sein, siehe AnkiLearnView
	}

	@Override protected LearnMap createMap()       { return new EmptyLearnMap(); }
	@Override protected boolean hasInputField()      { return false; }
	@Override public void disableMcPanel() {}; // MCPanel darf in einer MC-Session niemals deaktiviert werden. Wird allerdings vermutlich eh nie aufgerufen.
}
