package app.shared.model;

/**
 * Anki-spezifische Sortierreihenfolgen für Lernkarten (z.B. nach Fehlerhäufigkeit
 * oder Datum der letzten Wiederholung).
 * <p>
 * Liegt in {@code shared} statt in {@code learn.anki}, obwohl der Typ inhaltlich
 * dorthin gehört. Grund: {@link Screen#sort(CardSortOrder)} lebt im neutralen
 * {@code Screen}-Vertrag, weil der Controller die Sortierung über eine
 * {@code Screen}-Referenz durchreicht (siehe {@code Controller.cardSortOrderSelected}).
 * Läge dieser Typ in {@code learn.anki}, müsste {@code shared} dorthin importieren –
 * verbotene Abhängigkeitsrichtung. Der Typ ist hier also bewusst eine Etage zu hoch
 * angesiedelt; ein akzeptierter Schönheitsfehler, kein struktureller Defekt.
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
