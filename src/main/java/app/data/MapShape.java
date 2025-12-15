package app.data;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.shape.Shape;

public record MapShape(String id, String deckId, String regionName, String capitalName, Set<String> altRegionNames, Set<String> altCapitalNames, String fixedColorSet,
		Shape shape) {
	
	public MapShape(Shape shape, String id, String deckId, String regionName, String capital, String altRegionNames, String altCapitalNames,
			String fixedColorSet) {
		this(id, deckId, regionName, capital, parseToSet(altRegionNames), // String -> Set
				parseToSet(altCapitalNames), // String -> Set
				fixedColorSet, shape);
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
	
	public boolean isDecoration() {
		return (fixedColorSet != null);
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
}