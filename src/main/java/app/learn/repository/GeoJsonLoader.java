package app.learn.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.learn.model.MapShape;
import app.shared.model.ShapeGeometry;
import app.shared.model.ShapeGeometry.Point;

/**
 * Diese Klasse kennt nur das MapRepository!
 * Erzeugt aus einer geoJSON mit Multipolygons eine Liste von MapShapes.
 *
 * <p>Baut keine JavaFX-Nodes mehr — nur noch framework-freie {@link ShapeGeometry}. Die Y-Invertierung
 * (Bildschirm-Koordinaten) bleibt hier als reine Arithmetik; den Path/Group-Bau macht der MapNodeBuilder.</p>
 */
class GeoJsonLoader {

	public List<MapShape> load(java.nio.file.Path filePath, boolean isShapeMap) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(filePath.toFile());
			return parseFeatures(root, isShapeMap);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Laden von GeoJSON: " + filePath, e);
		}
	}

	private List<MapShape> parseFeatures(JsonNode root, boolean isShapeMap) {
		List<MapShape> shapes = new ArrayList<>();
		JsonNode features = root.get("features");

		for (JsonNode feature : features) {
			String id = feature.path("properties").path("id").asText(null);
			String capitalName = feature.path("properties").path("capitalName").asText(null);
			String regionName = feature.path("properties").path("regionName").asText(null);
			String shapeType = feature.path("properties").path("type").asText(null);
			String deckId = feature.path("properties").path("deckId").asText(null);
			String altCapitalNames = feature.path("properties").path("altCapitalNames").asText(null);
			String altRegionNames = feature.path("properties").path("altRegionNames").asText(null);

			JsonNode geometry = feature.get("geometry");
			String geometryType = geometry.get("type").asText();

			ShapeGeometry geo;
			if (isShapeMap) {
				geo = ShapeGeometry.shapePolygon(id, parseMultiPolygon(geometry), shapeType);
			} else  if ("MultiPolygon".equals(geometryType)) {
				geo = ShapeGeometry.polygon(id, parseMultiPolygon(geometry));
			} else if ("Polygon".equals(geometryType)) {
				geo = ShapeGeometry.polygon(id, parsePolygon(geometry));
			} else if ("MultiLineString".equals(geometryType)) {
				geo = ShapeGeometry.line(id, parseMultiLineString(geometry));
			} else {
				throw new RuntimeException("Unerwarteter Typ: " + geometryType);
			}

			shapes.add(new MapShape(geo, deckId, regionName, capitalName,
					altRegionNames, altCapitalNames));
		}
		return shapes;
	}

	private List<List<Point>> parsePolygon(JsonNode geometry) {
		List<List<Point>> rings = new ArrayList<>();
		for (JsonNode ring : geometry.get("coordinates"))
			rings.add(parsePoints(ring));
		return rings;
	}

	private List<List<Point>> parseMultiPolygon(JsonNode geometry) {
		List<List<Point>> rings = new ArrayList<>();
		for (JsonNode polygon : geometry.get("coordinates"))
			for (JsonNode ring : polygon)
				rings.add(parsePoints(ring));
		return rings;
	}

	private List<List<Point>> parseMultiLineString(JsonNode geometry) {
		List<List<Point>> lines = new ArrayList<>();
		for (JsonNode coords : geometry.get("coordinates")) {
			if (coords.size() == 0)
				continue;
			lines.add(parsePoints(coords));
		}
		return lines;
	}

	// Y-Invertierung wie zuvor (der Screen wächst nach unten, die geoJSON-Y nach oben).
	private List<Point> parsePoints(JsonNode ring) {
		List<Point> points = new ArrayList<>();
		for (JsonNode pt : ring)
			points.add(new Point(pt.get(0).asDouble(), -pt.get(1).asDouble()));
		return points;
	}
}