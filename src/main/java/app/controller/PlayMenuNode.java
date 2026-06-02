package app.controller;

/**
 * Marker-Interface für Elemente des "Spielen"-Menüs.
 * Dient der Typsicherheit, damit Service und Controller eine gemeinsame Sprache sprechen.
 */
public sealed interface PlayMenuNode permits PlayMenuItem {}