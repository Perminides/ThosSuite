package app.learn.repository;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import app.shared.model.ShapeGeometry.Point;

/**
 * Helperklasse. Bundlet Methoden für GeoJsonLoader und SketchFileSource
 */
public class GeoJsonGeometry {

	static List<List<Point>> parsePolygon(JsonNode geometry) {
		List<List<Point>> rings = new ArrayList<>();
		for (JsonNode ring : geometry.get("coordinates"))
			rings.add(parsePoints(ring));
		return rings;
	}

	static List<List<Point>> parseMultiPolygon(JsonNode geometry) {
		List<List<Point>> rings = new ArrayList<>();
		for (JsonNode polygon : geometry.get("coordinates"))
			for (JsonNode ring : polygon)
				rings.add(parsePoints(ring));
		return rings;
	}

	static List<List<Point>> parseMultiLineString(JsonNode geometry) {
		List<List<Point>> lines = new ArrayList<>();
		for (JsonNode coords : geometry.get("coordinates")) {
			if (coords.size() == 0)
				continue;
			lines.add(parsePoints(coords));
		}
		return lines;
	}

	// Y-Invertierung wie zuvor (der Screen wächst nach unten, die geoJSON-Y nach oben).
	static private List<Point> parsePoints(JsonNode ring) {
		List<Point> points = new ArrayList<>();
		for (JsonNode pt : ring)
			points.add(new Point(pt.get(0).asDouble(), -pt.get(1).asDouble()));
		return points;
	}
	
}
