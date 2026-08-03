package app.controller.model;

import app.learn.model.Deck;

/**
 * Der Menü-Eintrag für ein einzelnes Anki-Deck.
 *
 * @param label Der Text, der im Menü angezeigt wird.
 * @param deck  Das Deck, das gespielt werden soll.
 */
public record AnkiPlayItem(String label, Deck deck) implements PlayMenuNode {}
