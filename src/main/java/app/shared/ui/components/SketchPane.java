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
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

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
 * <p><b>Eine Fläche hat drei Zustände.</b> Frisch ist sie umrandet und ungefüllt — ein
 * {@code Path} bringt genau das von sich aus mit, und der Skin setzt nur den Strich. Markiert
 * bekommt sie zusätzlich eine Füllung. Gefüllt trägt sie ihre Farbe und <b>keinen</b> Strich mehr:
 * So verschmelzen zwei benachbarte Flächen derselben Farbe am Ende nahtlos, statt eine Naht zu
 * zeigen, die es auf dem Original nicht gibt.</p>
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

	private final Map<Integer, Path> areas = new HashMap<>();

	/**
	 * @param geometries die Teilflächen, jede mit ihrer Nummer als id
	 * @param width     Breite des Feldes, in das die Skizze eingepasst wird
	 * @param height      Höhe des Feldes
	 */
	public SketchPane(List<ShapeGeometry> geometries, double width, double height) {
		double factor = scaleFactor(geometries, width, height);

		Group contentGroup = new Group();
		for (ShapeGeometry geometry : geometries) {
			Path area = buildPath(geometry.scaled(factor));
			areas.put(Integer.parseInt(geometry.id()), area);
			contentGroup.getChildren().add(area);
		}

		getChildren().add(contentGroup); // StackPane zentriert.
		getStyleClass().add("my-sketch-pane");
		setPrefSize(width, height);
		setMinSize(width, height);
		setMaxSize(width, height);
	}

	/** Hebt eine Fläche hervor; eine zuvor hervorgehobene verliert die Markierung. */
	public void mark(int area) {
		for (Path path : areas.values())
			path.pseudoClassStateChanged(MARKED, false);
		pathFor(area).pseudoClassStateChanged(MARKED, true);
	}

	/** Färbt eine Fläche. Eine markierte verliert dabei ihre Markierung — gefüllt sticht markiert. */
	public void fill(int area, SketchColor color) {
		Path path = pathFor(area);
		path.pseudoClassStateChanged(MARKED, false);
		// Exklusiv: eine Fläche trägt genau eine Farbe, sonst entschiede die Reihenfolge im Stylesheet.
		for (SketchColor other : SketchColor.values())
			path.getStyleClass().remove(other.styleClass());
		path.getStyleClass().add(color.styleClass());
	}

	private Path pathFor(int number) {
		Path path = areas.get(number);
		if (path == null)
			throw new RuntimeException("Die Skizze hat keine Fläche " + number + ", sondern " + areas.size());
		return path;
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
	private static double scaleFactor(List<ShapeGeometry> geometries, double width, double height) {
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;

		for (ShapeGeometry geometry : geometries)
			for (List<Point> ring : geometry.paths())
				for (Point p : ring) {
					minX = Math.min(minX, p.x());
					maxX = Math.max(maxX, p.x());
					minY = Math.min(minY, p.y());
					maxY = Math.max(maxY, p.y());
				}

		double geoWidth = maxX - minX;
		double geoHeight = maxY - minY;
		if (geoWidth <= 0 || geoHeight <= 0)
			throw new IllegalStateException("Skizze ohne Ausdehnung — keine Punkte in den Flächen?");

		// Eine halbe Strichbreite genügte rechnerisch; eine ganze je Seite hält auch das Antialiasing
		// der Kante vom Clip fern.
		double overhang = 2 * SkinService.get().sketchStrokeWidth();
		return Math.min((width + overhang) / geoWidth, (height + overhang) / geoHeight);
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
		path.getStyleClass().add("my-sketch-area");
		path.setMouseTransparent(true); // Die Skizze zeigt nur an, sie nimmt keine Klicks.
		return path;
	}
}
