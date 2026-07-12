package app.learn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.MapImagePaths;
import app.learn.model.MapMetadata;
import app.learn.model.MapShape;
import app.learn.model.MapType;
import app.learn.repository.MapRepository;
import app.shared.skin.Skin;
import app.shared.skin.SkinImageCache;
import app.shared.skin.SkinService;

/**
 * Singleton wegen des Shape-Cachings — da darf es nur einen geben.
 * Hält alle Maps. Region wie auch Anki. Nur die ShapeDefinitionen (ShapeGeometry). Keine Nodes
 */
public class MapService {
	private static MapService instance;

	private final MapRepository repository;
	private final Map<MapMetadata, GeoMap> cache = new HashMap<>();

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
		return cache.computeIfAbsent(type.getMapMetadata(), _ -> repository.load(type));
	}

	/** Die vier skin-abhängigen Bildpfade einer Bild-Karte (frameworkfrei, aus dem Skin aufgelöst). */
	public MapImagePaths imagePathsFor(Deck type) {
		Skin skin = SkinService.get();
		String mapName = type.getMapName();
		return new MapImagePaths(
				skin.getMapImagePath(mapName),
				skin.getMapOverlayImagePath(mapName),
				skin.getMapInactiveImagePath(mapName),
				skin.getMapInactiveOverlayImagePath(mapName));
	}

	/**
	 * Wärmt die Caches für ein Deck (Splash). Shapes immer; bei Bild-Karten zusätzlich die großen Bilder über
	 * den SkinImageCache. {@code warm(...)} gibt nichts zurück — so fasst MapService kein {@link javafx.scene.image.Image}
	 * an und bleibt javafx-frei.
	 */
	public void preload(Deck type) {
		getMap(type); // Füllt den Cache mit den geojson Infos

		MapMetadata meta = type.getMapMetadata();
		if (meta.getMapType() == MapType.IMAGE) {
			MapImagePaths paths = imagePathsFor(type);
			SkinImageCache images = SkinImageCache.getInstance();
			images.warm(paths.background());
			images.warm(paths.overlay());
			images.warm(paths.inactiveBackground());
			images.warm(paths.inactiveOverlay());
		}
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