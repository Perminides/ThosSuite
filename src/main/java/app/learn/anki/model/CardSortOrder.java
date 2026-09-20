package app.learn.anki.model;

/**
 * Anki-spezifische Sortierreihenfolgen für Lernkarten (z.B. nach Fehlerhäufigkeit
 * oder Datum der letzten Wiederholung).
 */

public enum CardSortOrder {
	RANDOM("Zufällig"),
	BY_LEVEL_DESC("Höchstes Level zuerst"),
	BY_LEVEL_ASC("Niedrigstes Level zuerst"), 
	BY_WRONG_COUNT_DESC("Schwere zuerst"),
	BY_WRONG_COUNT_ASC("Leichte zuerst");

	private final String displayName;

	CardSortOrder(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
