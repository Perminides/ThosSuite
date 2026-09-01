package app.shared.ui.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.shared.model.ShapeGeometry;
import app.shared.model.ShapeGeometry.Point;
import app.shared.model.SketchColor;
import app.shared.skin.SkinService;
import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;

/**
 * Eine schematische Skizze aus nummerierten Teilflächen, die sich Fläche für Fläche einfärben lässt.
 *
 * <p>Sie kennt <b>keinen Lern-Typ</b> und nichts von dem, was sie zeigt: die Flächen kommen als
 * {@link ShapeGeometry}-Liste herein, jede mit ihrer Nummer als id. Wer sie füllt und in welcher
 * Reihenfolge, entscheidet der Aufrufer.</p>
 *
 * <p>Sie <b>ist</b> der sichtbare Node — eine StackPane, die ihren Inhalt zentriert. Ihre Größe
 * bekommt sie übergeben; eine Lage nicht, denn sie wird in eine other Komponente gehängt und nicht
 * auf ein Spielfeld gesetzt. Paketprivat: Sie ist Innenleben von {@link SuiteImage}, kein Angebot.</p>
 *
 * <p><b>Eine Fläche hat drei Zustände.</b> Frisch ist sie umrandet und ungefüllt, markiert bekommt
 * sie zusätzlich eine Füllung, gefüllt trägt sie ihre Farbe und <b>keinen</b> Strich mehr: So
 * verschmelzen zwei benachbarte Flächen derselben Farbe am Ende nahtlos, statt eine Naht zu zeigen,
 * die es auf dem Original nicht gibt.</p>
 *
 * <p><b>Den ersten Zustand setzt der Skin ausdrücklich</b>, statt ihn vom JavaFX-Standard zu erben.
 * Der teilt seine Formen nämlich in zwei Familien: Flächen ({@code Polygon}, {@code Circle}) starten
 * schwarz gefüllt, Linien ({@code Path}) ungefüllt. Seit hier beide Arten nebeneinander liegen, muss
 * „noch nicht beantwortet" aus <i>einer</i> Regel kommen — sonst hinge das Aussehen daran, welche
 * Form eine Datei zufällig benutzt.</p>

 * <p>Eine Fläche kann ein Polygon sein oder ein Kreis. Der Unterschied endet beim Bauen des Nodes:
 * Style-Klasse, Zustände und Nummerierung sind für beide dieselben.</p>
 *
 * <p>Skaliert werden die <b>Koordinaten</b> beim Bauen ({@link ShapeGeometry#scaled}), nicht der
 * fertige Node über eine Transformation — sonst würde der Strich mitwachsen. Der Faktor gilt für
 * beide Achsen gleich: Eine Skizze wird eingepasst und mittig gesetzt, nie gestreckt.</p>
 *
 * <p>CSS (vom Skin gesetzt): Fläche = {@code .my-sketch-area}, {@code :marked} und je Farbe eine
 * Klasse aus {@link SketchColor#styleClass()}.</p>
 */
class SketchPane extends StackPane {

	private static final PseudoClass MARKED = PseudoClass.getPseudoClass("marked");

	private final Map<Integer, Shape> areas = new HashMap<>();
	private final Group contentGroup = new Group();
	private final double factor;
	private final double cellWidth;
	private final double cellHeight;

	/**
	 * @param geometries die Teilflächen, jede mit ihrer Nummer als id
	 * @param width     Breite des Feldes, in das die Skizze eingepasst wird
	 * @param height      Höhe des Feldes
	 */
	public SketchPane(List<ShapeGeometry> geometries, double width, double height) {
		double[] box = bounds(geometries);
		factor = scaleFactor(box, width, height);
		cellWidth = (box[2] - box[0]) / 3;
		cellHeight = (box[3] - box[1]) / 3;

		addAreas(geometries, 0, 1);

		getChildren().add(contentGroup); // StackPane zentriert.
		getStyleClass().add("my-sketch-pane");
		setPrefSize(width, height);
		setMinSize(width, height);
		setMaxSize(width, height);
	}

	/**
	 * Hängt die Flächen einer weiteren Struktur an, verschoben in eines von neun Rasterfeldern
	 * (zeilenweise 0…8). Die bisherigen Flächen behalten Nummer, Markierung und Füllung — das ist
	 * der Unterschied zu einer neu gebauten Skizze, und der einzige Grund, warum es diese Methode
	 * gibt.
	 *
	 * <p>Das Raster sind schlicht die <b>Drittel der zuerst geladenen Skizze</b>. Damit ist „setze in
	 * Feld 4" eine Aussage über eine Skizze und keine über das, was sie zeigt; die Komponente lernt
	 * nichts über ihren Inhalt dazu. Die angehängte Struktur nummeriert für sich ab 0 und liegt in
	 * ihrem eigenen Feld 0 — verschoben wird um ganze Felder.</p>
	 *
	 * <p>Der Maßstab wird <b>nicht</b> neu gerechnet: Er stammt aus dem ersten Laden und bleibt
	 * gültig. Sonst schrumpfte eine Struktur, die versehentlich über den Rand ragt, nachträglich
	 * alles bereits Gezeichnete.</p>
	 */
	public void append(List<ShapeGeometry> geometries, int cell, double size, double offsetX,
			double offsetY) {
		int basis = areas.size();
		addAreas(geometries, basis, size);
		// cell < 0: Leinwand-Modus — die Silhouette liegt in ihren eigenen Leinwand-Koordinaten
		// (Gösch, Dreieck), oben auf, ohne Feld-Zentrierung. Wie der Hintergrund im Konstruktor.
		if (cell >= 0)
			for (int nummer = basis; nummer < areas.size(); nummer++)
				place(shapeFor(nummer), cell, offsetX, offsetY);
	}

	/**
	 * Setzt eine bereits vorhandene Fläche in ein anderes Rasterfeld. Farbe und Markierung bleiben,
	 * es wird nichts neu gebaut.
	 *
	 * <p>Trägt ein Element mehrere Flächen (ein zweigeteilter Kreis etwa), braucht jede ihren
	 * eigenen Aufruf — der Schritt kennt Flächen, keine Elemente.</p>
	 */
	public void move(int area, int cell) {
		place(shapeFor(area), cell, 0, 0);
	}

	/**
	 * Die Lage ist eine <b>Verschiebung des Knotens</b>, nicht eingebackene Koordinaten: Nur so ist
	 * Setzen und späteres Umsetzen derselbe Vorgang. Anders als eine Skalierung zieht eine
	 * Translation die Strichbreite nicht mit — der Einwand aus {@link ShapeGeometry#scaled} trifft
	 * hier also nicht zu.
	 */
	private void place(Shape shape, int cell, double offsetX, double offsetY) {
		if (cell < 0 || cell > 8)
			throw new RuntimeException("Rasterfeld liegt außerhalb von 0..8: " + cell);
		// Auf die Feldmitte, nicht auf die Feldecke: Elementdateien sind um ihren Nullpunkt
		// zentriert, damit sie beim Verkleinern stehen bleiben statt zur Ecke zu wandern.
		// Der Versatz rechnet in Dateikoordinaten (y nach oben positiv) und skaliert mit.
		shape.setTranslateX(cellWidth * factor * (cell % 3 + 0.5) + offsetX * factor);
		shape.setTranslateY(cellHeight * factor * (cell / 3 + 0.5) - offsetY * factor);
	}

	private void addAreas(List<ShapeGeometry> geometries, int basis, double size) {
		for (ShapeGeometry geometry : geometries) {
			Shape area = build(geometry.scaled(factor * size));
			areas.put(basis + Integer.parseInt(geometry.id()), area);
			contentGroup.getChildren().add(area);
		}
	}

	/** Polygon oder Kreis — beide tragen dieselbe Style-Klasse und kennen dieselben drei Zustände. */
	private static Shape build(ShapeGeometry geometry) {
		Shape shape = geometry.kind() == ShapeGeometry.Kind.CIRCLE
				? buildCircle(geometry)
				: buildPath(geometry);
		shape.getStyleClass().add("my-sketch-area");
		shape.setMouseTransparent(true); // Die Skizze zeigt nur an, sie nimmt keine Klicks.
		return shape;
	}

	/**
	 * Ein Kreis — oder, wenn ein zweiter herausgeschnitten wird, eine Sichel aus echten Bögen.
	 * {@code subtract} liefert einen frischen Knoten ohne die Stilklassen der beiden Kreise; die
	 * setzt {@link #build} ohnehin danach.
	 */
	private static Shape buildCircle(ShapeGeometry geometry) {
		Circle circle = new Circle(geometry.centerX(), geometry.centerY(), geometry.radius());
		ShapeGeometry.Cutout cutout = geometry.cutout();
		if (cutout == null)
			return circle;
		return Shape.subtract(circle, new Circle(cutout.x(), cutout.y(), cutout.radius()));
	}

	/** Hebt eine Fläche hervor; eine zuvor hervorgehobene verliert die Markierung. */
	public void mark(int area) {
		for (Shape shape : areas.values())
			shape.pseudoClassStateChanged(MARKED, false);
		shapeFor(area).pseudoClassStateChanged(MARKED, true);
	}

	/** Färbt eine Fläche. Eine markierte verliert dabei ihre Markierung — gefüllt sticht markiert. */
	public void fill(int area, SketchColor color) {
		Shape shape = shapeFor(area);
		shape.pseudoClassStateChanged(MARKED, false);
		// Exklusiv: eine Fläche trägt genau eine Farbe, sonst entschiede die Reihenfolge im Stylesheet.
		for (SketchColor other : SketchColor.values())
			shape.getStyleClass().remove(other.styleClass());
		shape.getStyleClass().add(color.styleClass());
	}

	private Shape shapeFor(int number) {
		Shape shape = areas.get(number);
		if (shape == null)
			throw new RuntimeException("Die Skizze hat keine Fläche " + number + ", sondern " + areas.size());
		return shape;
	}

	/**
	 * Der Maßstab, mit dem die Skizze in ihr Feld passt — der kleinere der beiden Faktoren, damit sie
	 * in beide Richtungen hineinpasst.
	 *
	 * <p>Die Skizze wird bewusst eine Spur <b>größer</b> gerechnet als ihr Feld, damit der Clip des
	 * Bilderrahmens die äußere Kontur vollständig wegschneidet. Sonst zeichnete sie ein eckiges
	 * Rechteck in einen Rahmen mit runden Ecken. Übrig bleiben die Trennlinien im Inneren, und die
	 * Füllungen folgen der Rundung.</p>
	 *
	 * <p>Das trifft nur die Kanten, die auch wirklich anstoßen: Passt die Skizze in der Höhe nicht
	 * aus, bleibt oben und unten Luft — dort liegt die Kontur mitten im Feld, ist die echte Kante der
	 * Skizze und wird gezeichnet.</p>
	 */
	private static double scaleFactor(double[] box, double width, double height) {
		double geoWidth = box[2] - box[0];
		double geoHeight = box[3] - box[1];
		if (geoWidth <= 0 || geoHeight <= 0)
			throw new IllegalStateException("Skizze ohne Ausdehnung — keine Punkte in den Flächen?");

		// Eine halbe Strichbreite genügte rechnerisch; eine ganze je Seite hält auch das Antialiasing
		// der Kante vom Clip fern.
		double overhang = 2 * SkinService.get().sketchStrokeWidth();
		return Math.min((width + overhang) / geoWidth, (height + overhang) / geoHeight);
	}

	/** Die umschließende Box aller Flächen: {@code minX, minY, maxX, maxY}. */
	private static double[] bounds(List<ShapeGeometry> geometries) {
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;

		for (ShapeGeometry geometry : geometries) {
			if (geometry.kind() == ShapeGeometry.Kind.CIRCLE) {
				minX = Math.min(minX, geometry.centerX() - geometry.radius());
				maxX = Math.max(maxX, geometry.centerX() + geometry.radius());
				minY = Math.min(minY, geometry.centerY() - geometry.radius());
				maxY = Math.max(maxY, geometry.centerY() + geometry.radius());
			}
			for (List<Point> ring : geometry.paths())
				for (Point p : ring) {
					minX = Math.min(minX, p.x());
					maxX = Math.max(maxX, p.x());
					minY = Math.min(minY, p.y());
					maxY = Math.max(maxY, p.y());
				}
		}

		return new double[] { minX, minY, maxX, maxY };
	}

	/**
	 * Eine Fläche als {@code Path}, bewusst nicht als {@code Polygon}: Ein frischer Path hat keine
	 * Füllung, ein Polygon startet schwarz. „Noch nicht beantwortet" ist hier genau dieser Zustand.
	 */
	private static Path buildPath(ShapeGeometry geometry) {
		List<PathElement> elements = new ArrayList<>();
		for (List<Point> ring : geometry.paths()) {
			if (ring.isEmpty())
				continue;
			Point first = ring.get(0);
			elements.add(new MoveTo(first.x(), first.y()));
			for (int i = 1; i < ring.size(); i++)
				elements.add(new LineTo(ring.get(i).x(), ring.get(i).y()));
			elements.add(new ClosePath());
		}

		Path path = new Path(elements);
		path.setFillRule(FillRule.EVEN_ODD);
		return path;
	}
}
