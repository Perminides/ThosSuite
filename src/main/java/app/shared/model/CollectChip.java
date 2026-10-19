package app.shared.model;

/**
 * Ein gesammeltes Wort eines Collect-Steps und wie es gerade dasteht. Dummes Eingabe-DTO für die
 * Chip-Zeile der Lernansicht: Der Progress entscheidet den Zustand, die Anzeige nur sein Aussehen.
 */
public record CollectChip(String text, State state) {

	public enum State {
		/** Gewählt, noch nicht gewertet — lässt sich wieder entfernen. */
		CHOSEN,
		CORRECT,
		/** Gewählt, gehört aber nicht dazu. */
		WRONG,
		/** Gehört dazu, wurde aber nicht gewählt. */
		MISSING,
		/** Gewählt und toleriert: weder richtig noch falsch, deshalb ohne Farbe. */
		TOLERATED
	}
}
