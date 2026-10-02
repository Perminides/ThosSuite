package app.learn.model;
public enum DeckCategory {
    ANKI_DECK ("anki"),
    REGION_DECK ("region");

	private final String skinKey;

	DeckCategory(String skinKey) {
		this.skinKey = skinKey;
	}

	/**
	 * Der Schlüssel, über den der Skin seine Staffelung auflöst — ein tragender Wert, kein
	 * Anzeigetext. Kein {@code toString()}: Wer die Konstante in eine Meldung schreibt, soll
	 * {@code ANKI_DECK} lesen und nicht {@code anki}.
	 */
	public String getSkinKey() {
		return skinKey;
	}
}