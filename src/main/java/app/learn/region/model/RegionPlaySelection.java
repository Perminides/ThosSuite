package app.learn.region.model;

import java.util.Set;

import app.learn.model.MapShape;

/**
 * Alles, was eine freie Regions-Session zum Start braucht — das Ergebnis von
 * {@code RegionPlaySetup.show(…)}.
 *
 * @param spec    was gespielt wird (primäres Deck, Modus, zusätzliche Decks)
 * @param regions die Regionen dazu
 */
public record RegionPlaySelection(SessionSpec spec, Set<MapShape> regions) {}
