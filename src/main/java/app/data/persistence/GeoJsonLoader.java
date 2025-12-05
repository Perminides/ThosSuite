package app.data.persistence;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.data.MapShape;

/**
 * Diese Klasse kennt nur das MapRepository!
 * Erzeugt aus einer geoJSON mit Multipolygons eine Liste von MapShapes
 */
class GeoJsonLoader {
	public List<MapShape> load(String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(filePath));
            
            List<MapShape> shapes = parseFeatures(root);
            return shapes;
            
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Laden von GeoJSON: " + filePath, e);
        }
    }
	
    private List<MapShape> parseFeatures(JsonNode root) {
        List<MapShape> shapes = new ArrayList<>();
        JsonNode features = root.get("features");
        
        for (JsonNode feature : features) {
            String id = feature.path("properties").path("id").asText(null);
            String capitalName = feature.path("properties").path("capitalName").asText(null);
            String regionName = feature.path("properties").path("regionName").asText(null);
            String fixedColorId = feature.path("properties").path("fixedColorId").asText(null);
            String deckId = feature.path("properties").path("deckId").asText(null);
            String altCapitalNames = feature.path("properties").path("altCapitalNames").asText(null);
            String altRegionNames = feature.path("properties").path("altRegionNames").asText(null);
            JsonNode geometry = feature.get("geometry");
            
            String type = geometry.get("type").asText();
            Shape shape = null;
            if ("MultiPolygon".equals(type)) {
            	shape = parseMultiPolygon(geometry);
            } else if ("Polygon".equals(type)) {
            	shape = parsePolygon(geometry);
            } else if("MultiLineString".equals(type)) {
            	shape = parseMultiLineString(geometry);
            	Shape blowUp = new BasicStroke(7f).createStrokedShape(shape); //!Später: Magic number
            	// !Später: Wir wissen immer noch nicht den Grund für die Artefakte! Also das ist Symptombekämpfung hier. Sollte das mal fehlschlagen, müssen wir an die Ursachen ran.
				Area corrected = new Area(blowUp); // Sonst gibt es unschöne Artefakte beim Zeichnen des Borders in Form von schwarzen Linien von einem Rand zum anderen an den Stellen der Vertices...
				shape = corrected;
            }
            shapes.add(new MapShape(shape, id, deckId, regionName, capitalName, altRegionNames, altCapitalNames, fixedColorId));
        }
        
        return shapes;
    }
    
    private static Path2D.Double parsePolygon(JsonNode geometry) {
        Path2D.Double path = new Path2D.Double();
        JsonNode coords = geometry.get("coordinates");
        
        for (JsonNode ring : coords) {
            if (ring.size() == 0) continue;
            
            JsonNode firstPoint = ring.get(0);
            path.moveTo(firstPoint.get(0).asDouble(),
                       -firstPoint.get(1).asDouble());
            
            for (int i = 1; i < ring.size(); i++) {
                JsonNode pt = ring.get(i);
                path.lineTo(pt.get(0).asDouble(),
                           -pt.get(1).asDouble());
            }
            path.closePath();
        }
        return path;
    }
    
	/**
	 * Erzeugt aus GeoJSON-Koordinaten (Polygon oder MultiPolygon) Java2D-Path2D-Objekte.
	 *
	 * GeoJSON: - Polygon: [ [outerRing], [hole1], [hole2], ... ] - MultiPolygon: [ polygon1, polygon2, ... ] polygonX = [ [outerRing], [hole1], ... ]
	 *
	 * Path2D.WIND_EVEN_ODD: - sorgt automatisch dafür, dass alle Ringe ab dem zweiten als „Loch“ ausgeschnitten werden.
	 */
    private Path2D parseMultiPolygon(JsonNode geometry) {
        Path2D path = new Path2D.Double();
        JsonNode coords = geometry.get("coordinates");
        
        for (JsonNode polygon : coords) {        // Jedes Polygon im MultiPolygon
            for (JsonNode ring : polygon) {       // Jeder Ring (outer + holes)
                
                if (ring.size() == 0) continue;
                
                // ring IST BEREITS das Punkt-Array!
                JsonNode firstPoint = ring.get(0);
                path.moveTo(firstPoint.get(0).asDouble(), 
                           -firstPoint.get(1).asDouble());
                
                for (int i = 1; i < ring.size(); i++) {
                    JsonNode pt = ring.get(i);
                    path.lineTo(pt.get(0).asDouble(), 
                               -pt.get(1).asDouble());
                }
                path.closePath();
            }
        }
        return path;
    }
    
	private static Path2D.Double parseMultiLineString(JsonNode geometry) {
		Path2D.Double path = new Path2D.Double();
		JsonNode lines = geometry.get("coordinates");
		for (JsonNode coords : lines) {
			if (coords.size() == 0)
				continue;
			JsonNode firstPoint = coords.get(0);
			path.moveTo(firstPoint.get(0).asDouble(), -firstPoint.get(1).asDouble());
			for (int i = 1; i < coords.size(); i++) {
				JsonNode pt = coords.get(i);
				path.lineTo(pt.get(0).asDouble(), -pt.get(1).asDouble());
			}
		}

		return path;
	}
}