package app.data;

public enum MapMetadata {
	// !Sofort: Du musst auf das normierte gehen, wenn die Fehler hier ausgemerzt sind!
	GERMANY(MapType.SHAPE, "germany.geojson;germany states.geojson", null),
	ITALY(MapType.SHAPE, "italy.geojson", null),
	SPAIN(MapType.SHAPE, "spain.geojson", null),
	USA(MapType.SHAPE, "usa states.geojson", null),
	CARIBBEAN(MapType.SHAPE, "carribean.geojson", null),
	BERLIN(MapType.SHAPE, "berlin.geojson", null), // !Sofort: Willste in Bertlin nicht noch die Bezirksgrenzen hinzufügen? Wäre flott gemacht :)
	
	WORLD(MapType.IMAGE, "worldAreas.geojson;worldCountries.geojson;worldLines.geojson", "world.png");

	private final String[] geoJsonFile;
	private final String bgImageFile;
	private final MapType mapType;

	MapMetadata(MapType type, String geoJsonFile, String bgImage) {
		this.mapType = type;
		this.geoJsonFile = geoJsonFile.split(";");
		this.bgImageFile = bgImage;
	}

	public MapType getMapType() {
		return mapType;
	}

	public String[] getGeoJsonFiles() {
		return geoJsonFile;
	}

	
}