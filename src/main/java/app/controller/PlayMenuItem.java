package app.controller;

/**
 * Repräsentiert einen klickbaren Eintrag im "Spielen"-Menü.
 * @param label Der Text, der im Menü angezeigt wird.
 * @param payload Das Datenobjekt (z.B. DeckType oder Marker), das bei Klick an den Controller gesendet wird.
 */
public record PlayMenuItem(String label, Object payload) implements PlayMenuNode {}