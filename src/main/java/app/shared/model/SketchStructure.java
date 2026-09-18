package app.shared.model;

import java.util.List;

/**
 * Eine Hintergrund-Skizze, wie sie in ihrer Strukturdatei steht: die Teilflächen und die Leinwand,
 * auf der sie liegen.
 *
 * <p>Die Leinwand ist fast immer die Box der Flächen selbst — ein Hintergrund füllt sie. Nur eine
 * Flagge, die kein Rechteck ist, lässt Platz frei; dann gibt die Datei ihre Leinwand im
 * GeoJSON-Feld {@code bbox} an, damit Raster und Elementgrößen dieselben bleiben wie überall.</p>
 *
 * @param areas  die Teilflächen, jede mit ihrer Nummer als id
 * @param canvas die Leinwand aus {@code bbox}, oder {@code null}: dann ist es die Box der Flächen
 */
public record SketchStructure(List<ShapeGeometry> areas, Canvas canvas) {

	/** Eine Box in Bildschirm-Koordinaten, Y also schon invertiert wie bei den Flächen. */
	public record Canvas(double minX, double minY, double maxX, double maxY) {}
}
