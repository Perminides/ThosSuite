package app.shared.model;

import java.util.Set;

/** Rohergebnis des Anki-Dialogs: Zahlen als Text (Parsen macht das Feature), gewählte Labels. */
public record AnkiDialogState(String minText, String maxText, String maxCardsText, Set<String> selectedLabels) {}