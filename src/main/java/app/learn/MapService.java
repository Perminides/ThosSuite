package app.learn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.MapMetadata;
import app.learn.model.MapShape;
import app.learn.repository.MapRepository;

/**
 * Singleton wegen des Shape-Cachings — da darf es nur einen geben.
 * Hält alle Maps. Region wie auch Anki. Nur die ShapeDefinitionen (ShapeGeometry). Keine Nodes
 */
public class MapService {
	private static MapService instance;

	private final MapRepository repository;
	private final Map<MapMetadata, GeoMap> shapeMapCache = new HashMap<>();

	private MapService() {
		this.repository = new MapRepository();
	}

	public static MapService getInstance() {
		if (instance == null) {
			instance = new MapService();
		}
		return instance;
	}

	/**
	 * Die Shapes einer Karte (gecacht). Skin-unabhängig — Shapes ändern sich bei Skin-Wechsel nicht, also
	 * braucht dieser Cache keine Invalidierung (die betrifft nur die Bilder, und die liegen im SkinImageCache).
	 */
	public GeoMap getMap(Deck type) {
		return shapeMapCache.computeIfAbsent(type.getMapMetadata(), _ -> repository.load(type));
	}

	/**
	 * Wärmt den Shape-Cache eines Decks vor (Splash).
	 *
	 * <p>Nur die Shapes — die großen Bilder einer Bild-Karte wärmt die Skin-Seite, angestoßen vom
	 * Controller. Dieses Feature nennt dafür nur den Kartennamen und fasst weder Pfad noch Bild an.</p>
	 */
	public void preloadShapes(Deck type) {
		getMap(type);
	}

	/**
	 * Returns the mapShapes that are playable in this session.
	 * E.g. Only the shapes from Lower Saxony...
	 */
	public Set<MapShape> getPlayableShapesForDeck(Deck type) {
		GeoMap map = getMap(type);
		return map.getShapes().stream()
				.filter(shape -> type.getId().equals(shape.deckId()))
				.filter(shape -> shape.isPlayable())
				.collect(Collectors.toSet());
	}
}