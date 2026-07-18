package app.shared.model;

import java.util.List;

/**
 * <p>Die Definition eines Shapes samt seiner id. Das was man braucht, um einen Node daraus zu bauen: den Path
 * oder Radius + Location, die id, die der Node als userData trägt, und — für Shape-Karten — den {@code type}.
 * Hieraus baut der {@link MapNodeBuilder} bei Bedarf den sichtbaren Node.</p>
 *
 * <p>Vier Formen: POLYGON (geschlossene Ringe, z.B. Länder oder Landkreise), LINE (offene Züge, z.B. Flüsse),
 * CIRCLE (sichtbarer Kreis — Städte als Punkte, oder der Falsch-Klick-Marker) und CENTER (ein reiner
 * Zentrier-Anker: kein sichtbares Shape, die Bild-Karte zentriert nur auf x/y und baut keinen Node).</p>
 *
 * <p>Die Punkte liegen bereits in Bildschirm-Koordinaten (Y beim Laden invertiert) vor. Der Builder plottet
 * sie direkt, ohne weitere Umrechnung.</p>
 *
 * <h3>Zur Rolle von {@code type} — bewusst hier, mit einer bekannten Naht (bitte vor dem Verschieben lesen)</h3>
 *
 * <p>{@code type} ist der rohe GeoJSON-Schlüssel ("0".."3") einer <b>Shape-Karten</b>-Geometrie. Er trägt
 * <b>zwei</b> voneinander unabhängige Bedeutungen, die heute nur zufällig aus derselben Quelle fließen:</p>
 * <ul>
 *   <li><b>Darstellung</b> (Node-Bau, shared-Seite): zIndex, CSS-Layer-Klasse, "Klicks registrieren?". Diese
 *       Ableitung macht {@code ShapeLayer} (package-private neben dem {@link MapNodeBuilder}).</li>
 *   <li><b>Lernstoff</b> (Domäne, learn-Seite): "ist dieses Shape zu lernen / wird es abgefragt?". Diese
 *       Ableitung macht {@code MapShape.isPlayable()} in learn — bewusst getrennt und ehrlich benannt, statt
 *       wie früher unter dem UI-Namen {@code isInteractive} mitzureisen.</li>
 * </ul>
 *
 * <p><b>Warum {@code type} trotzdem hier steht und nicht in einem eigenen Transport-Objekt versteckt wird:</b>
 * Nur die Shape-Karte braucht {@code type}; die Bild-Karten-Fabriken ({@link #polygon}, {@link #line},
 * {@link #circle}, {@link #center}) lassen ihn null. Diese Asymmetrie ist der ehrliche Hinweis, dass hier
 * <i>zwei Welten</i> in einem Typ zusammenliegen — die Shape-Karten-Geometrie und die Bild-Karten-Geometrie.
 * Sie sind es womöglich <b>nicht</b>: verschiedene Builder-Methoden ({@code buildShapeMapNode} vs.
 * {@code buildImageMapNode}), {@code type} nur auf einer Seite, Kreise aus verschiedenen Quellen. Eine
 * Aufspaltung in zwei Typen wäre denkbar — ist aber ein großer Umbau, der die fertige Bild-Karten-Seite wieder
 * anfasst, und wurde bewusst zurückgestellt (YAGNI). Würde getrennt, bekäme der Shape-Teil den {@code type}
 * ohnehin. Solange nicht getrennt wird, wohnt er sichtbar hier, statt in einem Nebenobjekt vorgespiegelt zu
 * werden, dessen einziger Zweck das Verstecken wäre (drei "ein Shape"-Typen — MapShape/ShapeGeometry/Transport
 * — wären schlechter auffindbar als diese eine markierte Naht). <b>Das ist die Naht. Nicht überrascht sein.</b></p>
 */
public final class ShapeGeometry {

	public enum Kind { POLYGON, LINE, CIRCLE, CENTER }

	public record Point(double x, double y) {}

	private final String id;
	private final Kind kind;
	private final List<List<Point>> paths;   // POLYGON: Ringe; LINE: Linienzüge; CIRCLE/CENTER: leer
	private final double centerX;
	private final double centerY;
	private final double radius;
	private final String type;               // nur Shape-Karten (roher GeoJSON-Schlüssel); Bild-Karten: null. Siehe Klassen-Doc.

	private ShapeGeometry(String id, Kind kind, List<List<Point>> paths, double centerX, double centerY, double radius, String type) {
		this.id = id;
		this.kind = kind;
		this.paths = paths;
		this.centerX = centerX;
		this.centerY = centerY;
		this.radius = radius;
		this.type = type;
	}

	/** Shape-Karte: Polygon mit Layer-{@code type} (Darstellung + Lernstoff leiten sich daraus ab, s. Klassen-Doc). */
	public static ShapeGeometry shapePolygon(String id, List<List<Point>> rings, String type) {
		return new ShapeGeometry(id, Kind.POLYGON, rings, 0, 0, 0, type);
	}

	/** Bild-Karte: Polygon ohne type. */
	public static ShapeGeometry polygon(String id, List<List<Point>> rings) {
		return new ShapeGeometry(id, Kind.POLYGON, rings, 0, 0, 0, null);
	}

	public static ShapeGeometry line(String id, List<List<Point>> lines) {
		return new ShapeGeometry(id, Kind.LINE, lines, 0, 0, 0, null);
	}

	public static ShapeGeometry circle(String id, double centerX, double centerY, double radius) {
		return new ShapeGeometry(id, Kind.CIRCLE, List.of(), centerX, centerY, radius, null);
	}

	/** Zentrier-Anker: kein sichtbares Shape. Die Bild-Karte zentriert nur auf x/y, baut keinen Node. */
	public static ShapeGeometry center(String id, double x, double y) {
		return new ShapeGeometry(id, Kind.CENTER, List.of(), x, y, 0, null);
	}

	public String id() { return id; }
	public Kind kind() { return kind; }
	public List<List<Point>> paths() { return paths; }
	public double centerX() { return centerX; }
	public double centerY() { return centerY; }
	public double radius() { return radius; }

	/** Der rohe Layer-Schlüssel — nur bei Shape-Karten gesetzt, sonst null. Siehe Klassen-Doc. */
	public String type() { return type; }
}