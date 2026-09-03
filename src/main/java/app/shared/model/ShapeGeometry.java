package app.shared.model;

import java.util.ArrayList;
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

	/**
	 * Ein Kreis, der aus einem anderen Kreis herausgeschnitten wird — so entsteht eine Sichel aus
	 * echten Bögen statt aus einer Näherung. Nur Skizzen benutzen das; Karten lassen es null.
	 */
	public record Cutout(double x, double y, double radius) {}

	private final String id;
	private final Kind kind;
	private final List<List<Point>> paths;   // POLYGON: Ringe; LINE: Linienzüge; CIRCLE/CENTER: leer
	private final double centerX;
	private final double centerY;
	private final double radius;
	private final Cutout cutout;             // nur Skizzen: der herausgeschnittene Kreis, sonst null.
	private final String type;               // nur Shape-Karten (roher GeoJSON-Schlüssel); Bild-Karten: null. Siehe Klassen-Doc.

	private ShapeGeometry(String id, Kind kind, List<List<Point>> paths, double centerX, double centerY, double radius, String type) {
		this(id, kind, paths, centerX, centerY, radius, type, null);
	}

	private ShapeGeometry(String id, Kind kind, List<List<Point>> paths, double centerX, double centerY,
			double radius, String type, Cutout cutout) {
		this.id = id;
		this.kind = kind;
		this.paths = paths;
		this.centerX = centerX;
		this.centerY = centerY;
		this.radius = radius;
		this.type = type;
		this.cutout = cutout;
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

	/** Kreis mit einem herausgeschnittenen zweiten Kreis — die Sichel. */
	public static ShapeGeometry circle(String id, double centerX, double centerY, double radius, Cutout cutout) {
		return new ShapeGeometry(id, Kind.CIRCLE, List.of(), centerX, centerY, radius, null, cutout);
	}

	/** Zentrier-Anker: kein sichtbares Shape. Die Bild-Karte zentriert nur auf x/y, baut keinen Node. */
	public static ShapeGeometry center(String id, double x, double y) {
		return new ShapeGeometry(id, Kind.CENTER, List.of(), x, y, 0, null);
	}

	/**
	 * Dieselbe Form in einem anderen Maßstab — alle Punkte, Mittelpunkte und Radien mit {@code factor}
	 * multipliziert. Gibt eine <b>neue</b> Instanz zurück; das Original bleibt unangetastet, denn die
	 * Geometrien liegen gecacht im {@code MapService} und werden zwischen Skins geteilt.
	 *
	 * <p>Damit skaliert die Shape-Karte ihre Formen einmal beim Bauen, statt eine {@code Scale} an den
	 * fertigen Node zu hängen. Der Unterschied ist nicht kosmetisch: eine Transformation trifft
	 * <em>alles</em>, was der Node zeichnet — Strichbreiten und Effekte eingeschlossen. Skalierte
	 * Koordinaten treffen nur die Form, und 15 px Schatten bleiben 15 px, egal wie stark die Karte
	 * gestaucht wird.</p>
	 */
	public ShapeGeometry scaled(double factor) {
		List<List<Point>> scaled = new ArrayList<>(paths.size());
		for (List<Point> ring : paths) {
			List<Point> neu = new ArrayList<>(ring.size());
			for (Point p : ring)
				neu.add(new Point(p.x() * factor, p.y() * factor));
			scaled.add(neu);
		}
		Cutout skaliert = cutout == null ? null
				: new Cutout(cutout.x() * factor, cutout.y() * factor, cutout.radius() * factor);
		return new ShapeGeometry(id, kind, scaled, centerX * factor, centerY * factor, radius * factor, type,
				skaliert);
	}

	public String id() { return id; }
	public Kind kind() { return kind; }
	public List<List<Point>> paths() { return paths; }
	public double centerX() { return centerX; }
	public double centerY() { return centerY; }
	public double radius() { return radius; }
	public Cutout cutout() { return cutout; }

	/** Der rohe Layer-Schlüssel — nur bei Shape-Karten gesetzt, sonst null. Siehe Klassen-Doc. */
	public String type() { return type; }
}