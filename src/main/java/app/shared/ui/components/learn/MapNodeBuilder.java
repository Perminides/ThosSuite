package app.shared.ui.components.learn;

import java.util.ArrayList;
import java.util.List;

import app.shared.ui.components.learn.model.ShapeGeometry;
import app.shared.ui.components.learn.model.ShapeGeometry.Point;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 * Baut aus framework-freier {@link ShapeGeometry} den sichtbaren JavaFX-Node. Die <b>einzige</b> Stelle, an der
 * Karten-Nodes entstehen — vorher verstreut über GeoJsonLoader, ShapeMap-Konstruktor und GeoMap.createCircle.
 *
 * <p>Kennt keinen Lern-Typ. Alles, was er wissen muss, kommt als Geometrie (inkl. id und — bei Shape-Karten —
 * type) rein. Beide Karten-Seiten haben jetzt dieselbe Signatur: eine Geometrie rein, ein Node raus. Die frühere
 * id/styleClass-Asymmetrie auf der Shape-Seite ist weg.</p>
 *
 * <p>Struktur je Form:
 * <ul>
 *   <li>POLYGON + Shape-Karte → ein Path, {@code .my-map-shape} + Layer-Klasse (aus {@code type}), ggf. mouseTransparent.</li>
 *   <li>POLYGON + Bild-Karte → Group(first, second), {@code .my-image-map-shape}.</li>
 *   <li>LINE → Group(first, second), {@code .river .my-image-map-shape} (Linien sind stets Bild-Karten).</li>
 *   <li>CIRCLE → Group(first, second) aus zwei Kreisen, {@code .my-image-map-shape}.</li>
 * </ul>
 * CENTER erreicht den Builder nie — die Bild-Karte fängt ihn ab und zentriert nur.</p>
 */
public final class MapNodeBuilder {

	private MapNodeBuilder() {}

	/**
	 * Deutschland-Shape: immer ein Polygon → ein Path mit "my-map-shape" + Layer-Klasse. id und type kommen aus
	 * der Geometrie; zIndex/Layer-Klasse/interaktiv leitet {@link ShapeLayer} aus dem type ab.
	 */
	public static Node buildShapeMapNode(ShapeGeometry geometry) {
		ShapeLayer layer = ShapeLayer.fromJsonId(geometry.type());
		return buildShapePath(geometry, layer.styleClass(), layer.interactive());
	}

	/** Welt-Shape: Polygon/Linie → Group(first, second), Kreis → Marker. Kennt keine Layer-Klasse. id aus der Geometrie. */
	public static Node buildImageMapNode(ShapeGeometry geometry) {
		switch (geometry.kind()) {
			case CIRCLE:
				return buildMarker(geometry);
			case LINE:
				return buildImageGroup(geometry, true);
			case POLYGON:
				return buildImageGroup(geometry, false);
			default:
				throw new RuntimeException("Kein Bild-Karten-Node für: " + geometry.kind());
		}
	}

	// Shape-Karte (Deutschland): ein Path, "my-map-shape" + Layer-Klasse.
	private static Node buildShapePath(ShapeGeometry geometry, String layerStyleClass, boolean interactive) {
		String id = geometry.id();
		Path path = new Path(buildElements(geometry, true));
		path.setFillRule(FillRule.EVEN_ODD);
		path.getStyleClass().add("my-map-shape");
		path.getStyleClass().add(layerStyleClass);
		if (!interactive)
			path.setMouseTransparent(true); // nicht-interaktive Layer lassen Klicks durch
		path.setUserData(id);
		path.setId("pathId-" + id);
		return path;
	}

	// Bild-Karte (Welt): Group aus Fill-Path ("first") und Border-Path ("second"). Für Linien zusätzlich "river".
	private static Node buildImageGroup(ShapeGeometry geometry, boolean isLine) {
		String id = geometry.id();
		List<PathElement> elements = buildElements(geometry, !isLine);

		Path first = new Path();
		first.setFillRule(FillRule.EVEN_ODD);
		first.getElements().addAll(elements);
		first.getStyleClass().add("first");
		first.setId("pathId-" + id + (isLine ? "-river-first" : "-first"));

		Path second = new Path();
		second.setFillRule(FillRule.EVEN_ODD);
		second.getElements().addAll(elements);
		second.getStyleClass().add("second");
		second.setId("pathId-" + id + (isLine ? "-river-second" : "-second"));

		Group group = new Group(first, second);
		if (isLine)
			group.getStyleClass().add("river");
		group.getStyleClass().add("my-image-map-shape");
		group.setUserData(id);
		group.setId("groupId-" + id);
		return group;
	}

	// Sichtbarer Kreis / Falsch-Klick-Marker: zwei Kreise (Fill/Border) in einer Group, strukturgleich zu einer Bild-Karten-Shape.
	private static Node buildMarker(ShapeGeometry geometry) {
		String id = geometry.id();
		Circle fill = new Circle(geometry.centerX(), geometry.centerY(), geometry.radius());
		fill.getStyleClass().add("first");
		Circle border = new Circle(geometry.centerX(), geometry.centerY(), geometry.radius());
		border.getStyleClass().add("second");

		Group group = new Group(fill, border);
		group.getStyleClass().add("my-image-map-shape");
		group.setUserData(id);
		group.setId("groupId-" + id);
		return group;
	}

	// Punktfolgen → MoveTo/LineTo(/ClosePath). Y ist bereits invertiert (Bildschirm-Koordinaten).
	private static List<PathElement> buildElements(ShapeGeometry geometry, boolean closed) {
		List<PathElement> elements = new ArrayList<>();
		for (List<Point> ring : geometry.paths()) {
			if (ring.isEmpty())
				continue;
			Point first = ring.get(0);
			elements.add(new MoveTo(first.x(), first.y()));
			for (int i = 1; i < ring.size(); i++) {
				Point p = ring.get(i);
				elements.add(new LineTo(p.x(), p.y()));
			}
			if (closed)
				elements.add(new ClosePath());
		}
		return elements;
	}
}