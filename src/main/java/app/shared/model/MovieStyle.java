package app.shared.model;

import javafx.scene.text.Font;

/**
 * Die Skin-Werte, die die Film-Oberfläche braucht.
 *
 * @param posterWidth   Breite des Posterbildes; bestimmt auch die Größe der Rating-Zahl
 * @param font          Grundlage für sämtliche Abstände in Kachel und Viewer
 * @param tooltipMargin Rand, den das Kommentar-Popup zum Bildschirmrand hält
 */
public record MovieStyle(int posterWidth, Font font, double tooltipMargin) {}
