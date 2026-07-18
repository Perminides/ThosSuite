package app.learn.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import app.shared.model.ShapeGeometry;

/**
 * Reine Datenklasse: die Attribute aus der geoJSON plus die framework-freie {@link ShapeGeometry}. Kein Node —
 * der sichtbare Node entsteht erst im {@code MapNodeBuilder}. Damit ist diese Klasse (und der Lern-Kern) an dieser
 * Stelle JavaFX-frei.
 *
 * <p>id <b>und</b> type wohnen in der {@link ShapeGeometry} (die der MapNodeBuilder ohnehin braucht) und werden
 * über {@link #id()} bzw. {@link #isPlayable()} durchgereicht. Je eine Wahrheit — analog zur id, die schon länger
 * dort lebt.</p>
 *
 * = Geometry + die ganzen Attribute aus Regionsspielen
 */
public record MapShape(String deckId, String regionName, String capitalName, Set<String> altRegionNames,
		Set<String> altCapitalNames, ShapeGeometry geometry) {

	/**
	 * Der rohe GeoJSON-type, dessen Shapes Lernstoff sind (die spielbare Ebene). Deckt sich mit
	 * {@code ShapeLayer.INTERACTIVE} auf der shared-Seite — beide leiten aus demselben rohen type ab,
	 * bewusst getrennt (Darstellung dort, Lernstoff hier). Siehe ShapeGeometry-Doc.
	 */
	private static final String PLAYABLE_TYPE = "0";

	public MapShape(ShapeGeometry geometry, String deckId, String regionName, String capital, String altRegionNames,
			String altCapitalNames) {
		this(deckId, regionName, capital, parseToSet(altRegionNames), // String -> Set
				parseToSet(altCapitalNames), // String -> Set
				geometry);
	}

	/** Die id des Shapes — sie lebt in der Geometrie. */
	public String id() {
		return geometry.id();
	}

	/**
	 * Ist dieses Shape Lernstoff — wird es also abgefragt? Learn-Domänenbegriff, abgeleitet aus dem rohen
	 * {@code type} der Geometrie. Löst das frühere überladene {@code isInteractive} ab, das versehentlich
	 * beides trug: "Klicks registrieren" (Darstellung, jetzt shared) und "ist Lernstoff" (hier).
	 */
	public boolean isPlayable() {
		return PLAYABLE_TYPE.equals(geometry.type());
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

		if (capitalName.toLowerCase().trim().equals(normalized)) {
			return true;
		}

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

		if (regionName.toLowerCase().trim().equals(normalized)) {
			return true;
		}

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
		result = prime * result + ((id() == null) ? 0 : id().hashCode());
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
		if (id() == null) {
			if (other.id() != null)
				return false;
		} else if (!id().equals(other.id()))
			return false;
		return true;
	}
}