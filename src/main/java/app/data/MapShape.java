package app.data;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.Node;

/**
 * Enthält den javafx.scene.Node und die Attribute aus der geojson. Der Node hat die "map-shape" oder "image-map-shape" CSS-Klasse gesetzt.
 * Und die entsprechende ShapeLayer-enum.
 */
public record MapShape(String id, String deckId, String regionName, String capitalName, Set<String> altRegionNames,
		Set<String> altCapitalNames, String type, Node shape, boolean isShapeMap) {
	
	public MapShape(Node shape, String id, String deckId, String regionName, String capital, String altRegionNames, String altCapitalNames,
			String type, boolean isShapeMap) {
		this(id, deckId, regionName, capital, parseToSet(altRegionNames), // String -> Set
				parseToSet(altCapitalNames), // String -> Set
				type, shape, isShapeMap);

		// --- A. Basis-Styling ---
        if (isShapeMap) {
            shape.getStyleClass().add("my-map-shape");
            // --- B. Layer-Logik anwenden
            ShapeLayer layer = ShapeLayer.fromJsonId(type);

            // 1. Spezifische CSS-Klasse setzen ("layer-water", "layer-neighbor" etc.)
            shape.getStyleClass().add(layer.styleClass);

            // 2. Interaktion steuern (Klicks durchlassen oder fangen)
            // Wir setzen es direkt am Node -> ShapeMapPane muss nichts mehr prüfen!
            if (!layer.interactive) {
                shape.setMouseTransparent(true);
            }
        } else {
            shape.getStyleClass().add("my-image-map-shape");
        }        
        shape.setUserData(id);
	}
	
	public int getZIndex() {
        return ShapeLayer.fromJsonId(this.type).zIndex;
    }

    public boolean isInteractive() {
        return ShapeLayer.fromJsonId(this.type).interactive;
    }

	private static Set<String> parseToSet(String commaSeparated) {
		if (commaSeparated == null || commaSeparated.isEmpty()) {
			return Set.of();
		}
		return Arrays.stream(commaSeparated.split(",")).map(String::trim).collect(Collectors.toSet());
	}
	
	public boolean isMatchingCapital(String text) {
	    if (text == null || text.isEmpty()) {
	        return false;
	    }
	    String normalized = text.toLowerCase().trim();
	    
	    // Check main capital name
	    if (capitalName.toLowerCase().trim().equals(normalized)) {
	        return true;
	    }
	    
	    // Check alternative capital names
	    if (altCapitalNames != null)
	    	return altCapitalNames.stream()
	    			.anyMatch(alt -> alt.toLowerCase().trim().equals(normalized));
	    
	    return false;
	}
	
	public boolean isMatchingRegion(String text) {
	    if (text == null || text.isEmpty()) {
	        return false;
	    }
	    String normalized = text.toLowerCase().trim();
	    
	    // Check main region name
	    if (regionName.toLowerCase().trim().equals(normalized)) {
	        return true;
	    }
	    
	    // Check alternative region names
	    if (altRegionNames != null)
	    	return altRegionNames.stream()
	    		.anyMatch(alt -> alt.toLowerCase().trim().equals(normalized));
	    
	    return false;
	}
	
	public boolean isMatching(String text) {
		return isMatchingCapital(text) || isMatchingRegion(text);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapShape other = (MapShape) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	private enum ShapeLayer {
	    // Definition: (GeoJsonId, Z-Index, Css-Klasse, Interaktiv?)
	    INTERACTIVE("0", 0, "layer-region", true), // null = Standard
	    NEIGHBOR("1", 10, "layer-neighbor", false),
	    WATER("2", 20, "layer-water", false),
	    OVERLAY("3", 30, "layer-overlay", false); // z.B. Bundeslandgrenzen

	    private final String jsonId;
	    private final int zIndex;
	    private final String styleClass;
	    private final boolean interactive;

	    ShapeLayer(String jsonId, int zIndex, String styleClass, boolean interactive) {
	        this.jsonId = jsonId;
	        this.zIndex = zIndex;
	        this.styleClass = styleClass;
	        this.interactive = interactive;
	    }

	    // Statische Lookup-Methode
	    public static ShapeLayer fromJsonId(String id) {
	        for (ShapeLayer layer : values()) {
	            if (id != null && id.equals(layer.jsonId)) return layer;
	        }
	        throw new RuntimeException("Unerwarteter Value im Typen eines Shapes: " + id);
	    }
	}
}