package app.controller.model;

/**
 * Der eine Sammeleintrag für die Regionen. Trägt kein Deck: welche Decks gespielt werden,
 * entscheidet erst der Konfigurations-Dialog.
 *
 * @param label Der Text, der im Menü angezeigt wird.
 */
public record RegionPlayItem(String label) implements PlayMenuNode {}
