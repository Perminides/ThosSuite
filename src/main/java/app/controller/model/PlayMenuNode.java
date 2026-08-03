package app.controller.model;

/**
 * Ein Eintrag im "Spielen"-Menü. Es gibt genau zwei Sorten, und die sealed-Liste sagt welche:
 * ein einzelnes Anki-Deck ({@link AnkiPlayItem}) oder den Sammeleintrag für die Regionen
 * ({@link RegionPlayItem}), hinter dem die Deck-Auswahl erst im Dialog passiert.
 *
 * <p>Region-Decks tauchen hier also <b>nicht</b> einzeln auf — deshalb reicht der Deck-Typ allein
 * nicht als Unterscheidung, und deshalb sind es zwei Records statt einem mit Payload.</p>
 */
public sealed interface PlayMenuNode permits AnkiPlayItem, RegionPlayItem {

	/** Der Text, der im Menü angezeigt wird. */
	String label();
}
