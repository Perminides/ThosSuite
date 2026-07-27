package app.shared.model;

/**
 * Die Skin-Werte, die die Tagebuch-Oberfläche braucht.
 *
 * @param viewerContentWidth Breite der Kartenspalte und der ScrollPane
 * @param tooltipMargin      Rand, den das vergrößerte Bild beim MouseOver zum Bildschirmrand hält
 */
public record DiaryStyle(double viewerContentWidth, double tooltipMargin) {}
