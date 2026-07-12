package app.learn.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.MapMetadata;
import app.learn.model.MapType;
import app.learn.model.MapShape;
import app.shared.Config;

/**
 * Sollte nur(!) vom MapService benutzt werden!
 * <p>Lädt die Shapes aus den GeoJSON-Dateien und gibt Sie als GeoMap zurück</p>
 * <p>Dünner Wrapper um den GeoJsonLoader</p>
 */
public class MapRepository {
	private final GeoJsonLoader loader;

	public MapRepository() {
		this.loader = new GeoJsonLoader();
	}

	public GeoMap load(Deck type) {
		MapMetadata meta = type.getMapMetadata();
		boolean isShapeMap = meta.getMapType() == MapType.SHAPE;

		Path folder = Config.getPath("geoJsonFolder");
		List<MapShape> shapes = new ArrayList<>();
		for (String fileName : meta.getGeoJsonFiles())
			shapes.addAll(loader.load(folder.resolve(fileName), isShapeMap));

		return new GeoMap(shapes, meta.getMapType());
	}
}