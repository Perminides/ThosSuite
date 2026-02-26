package app.data;

public enum MapMetadata {
	// !Sofort: Du musst auf das normierte gehen, wenn die Fehler hier ausgemerzt sind!
	GERMANY(MapType.SHAPE, "germany.geojson;germany states.geojson", null),
	ITALY(MapType.SHAPE, "italy.geojson", null),
	SPAIN(MapType.SHAPE, "spain.geojson", null),
	USA(MapType.SHAPE, "usa states.geojson", null),
	CARIBBEAN(MapType.SHAPE, "carribean.geojson", null),
	ENGLAND(MapType.SHAPE, "england.geojson", null),
	BERLIN(MapType.SHAPE, "berlin.geojson", null), // !Sofort: Willste in Bertlin nicht noch die Bezirksgrenzen hinzufügen? Wäre flott gemacht :)
	SCHWEIZ(MapType.SHAPE, "schweiz.geojson", null),
	HANNOVER_STADTTEILE(MapType.SHAPE, "hannover_stadtteile.geojson", null),
	OZEANIEN(MapType.SHAPE, "ozeanien.geojson", null),
	AUSTRIA(MapType.SHAPE, "austria.geojson", null),
	BAVARIA(MapType.SHAPE, "bayern_reg.geojson", null),
	HANNOVER_REGION(MapType.SHAPE, "hannover_region.geojson", null),
	
	WORLD(MapType.IMAGE, "worldAreas.geojson;worldCountries.geojson;worldLines.geojson", "world.png");

	private final MapType mapType;
	private final String[] geoJsonFile;
	private final String bgImageFile;
	

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