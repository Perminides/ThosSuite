package app.data;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nur eine dünne Datenklasse, nichts wildes hier.
 * Wobei, die kann mittlwerweile auch small|300|400 in einen entsprechenden Shape übersetzen :)
 */
public class GeoMap {
	public enum CircleSizes {
		ULTRA_SMALL("utlra small", 4),
		SMALL("small", 10),
		MIDDLE_SMALL("middle small", 18),
		MIDDLE("middle", 25),
		MIDDLE_BIG("middle big", 37),
		BIG("big", 50),
		ULTRA_BIG("ultra big", 100);
		
		CircleSizes (String csvName, int i) {
			this.size = i;
			this.csvName = csvName;
		}
		
		private final int size;
		private final String csvName;
		
		public int getSize() {return size;}
		public String getName() {return csvName;}
		
	    private static CircleSizes fromCsvName(String name) {
	        for (CircleSizes cs : values()) {
	            if (cs.csvName.equals(name)) {
	                return cs;
	            }
	        }
	        return null; // Oder throw Exception
	    }
	};
	
    private final Map<String, MapShape> shapes; 
    private final MapType type;
    private final BufferedImage backgroundImage;
    private final BufferedImage overlayImage;
    private final BufferedImage inactiveBackgroundImage;
    private final BufferedImage inactiveOverlayImage;
    
    public GeoMap(List<MapShape> shapes, MapType type, BufferedImage backgroundImage, BufferedImage overlayImage, BufferedImage inactiveImage, BufferedImage inactiveOverlayImage) {
    	this.shapes = new HashMap<>();
    	for (MapShape shape : shapes)
    		this.shapes.put(shape.id(), shape);
    	this.type = type;
    	this.backgroundImage = backgroundImage;
    	this.overlayImage = overlayImage;
    	this.inactiveBackgroundImage = inactiveImage;
    	this.inactiveOverlayImage = inactiveOverlayImage;
    }

	public Collection<MapShape> getShapes() {
		return shapes.values();
	}
	
	public Collection<String> getIds() {
		return shapes.keySet();
	}
	
	public MapType getType() {
		return type;
	}

	/**
	 * 
	 * @param id
	 * @return Den angefragten Shape oder einen frischen Kreis der angefragten Größe und dem angefragten Mittelpunkt.
	 */
	public MapShape getShape(String id) {
		if (shapes.get(id) != null)
			return shapes.get(id);

		if (id.contains("|")) {
            String[] parts = id.split("\\|");
            String size = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            
            MapShape circleShape = createCircle(id, size, x, y);
            return circleShape;
		}
		throw new RuntimeException("Keine Idee, was ich mit dieser ID machen soll tbh..." + id);
	}

	public BufferedImage getBackgroundImage() {
		return backgroundImage;
	}
	
	public BufferedImage getOverlayImage() {
		return overlayImage;
	}
	
	public BufferedImage getInactiveImage() {
		return inactiveBackgroundImage;
	}
	
	public BufferedImage getInactiveOverlayImage() {
		return inactiveOverlayImage;
	}
	
	public MapShape createCircle(CircleSizes size, int x, int y) {
		return createCircle("", size.getName(), x, y);
	}
	
    private MapShape createCircle(String id, String sizeString, int x, int y) {
        CircleSizes circleSize = CircleSizes.fromCsvName(sizeString);
        int radius = circleSize != null ? circleSize.getSize() : 65; //!Später: Fallback weg, wenn alle Shapes erstellt sind bei Welt (und Hannover)
        Path2D circle;
        if (radius != 65)
        	circle = new Path2D.Float(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));
        else
        	circle = new Path2D.Float(new Rectangle2D.Float(x - radius/2, y - radius/2, radius, radius));
        return new MapShape(circle, id, null, null, null, null, null, null);
    }

	public void setShapes(List<MapShape> transformed) {
		// !Sofort. Das muss wieder final. Nur wegen Normalisiserung für Internet-Karten, das überzeugt mich nicht!
		this.shapes.clear();
		for (MapShape ms : transformed)
			this.shapes.put(ms.id(), ms);
	}
}