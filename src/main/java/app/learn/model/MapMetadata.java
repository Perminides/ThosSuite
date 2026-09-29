package app.learn.model;

public enum MapMetadata {
	GERMANY(MapType.SHAPE, "germany.geojson;germany states.geojson"),
	ITALY(MapType.SHAPE, "italy.geojson"),
	SPAIN(MapType.SHAPE, "spain.geojson"),
	USA(MapType.SHAPE, "usa states.geojson"),
	CARIBBEAN(MapType.SHAPE, "carribean.geojson"),
	ENGLAND(MapType.SHAPE, "england.geojson"),
	BERLIN(MapType.SHAPE, "berlin.geojson"), // !Idee: Willste in Berlin nicht noch die Bezirksgrenzen hinzufügen? Wäre flott gemacht :)
	SCHWEIZ(MapType.SHAPE, "schweiz.geojson"),
	HANNOVER_STADTTEILE(MapType.SHAPE, "hannover_stadtteile.geojson"),
	OZEANIEN(MapType.SHAPE, "ozeanien.geojson"),
	AUSTRIA(MapType.SHAPE, "austria.geojson"),
	BAVARIA(MapType.SHAPE, "bayern_reg.geojson"),
	HANNOVER_REGION(MapType.SHAPE, "hannover_region.geojson"),
	
	WORLD(MapType.IMAGE, "worldAreas.geojson;worldCountries.geojson;worldLines.geojson"),
	HANNOVER(MapType.IMAGE, "hannoverAreas.geojson");

	private final MapType mapType;
	private final String[] geoJsonFile;
	

	MapMetadata(MapType type, String geoJsonFile) {
		this.mapType = type;
		this.geoJsonFile = geoJsonFile.split(";");
	}

	public MapType getMapType() {
		return mapType;
	}

	public String[] getGeoJsonFiles() {
		return geoJsonFile;
	}

	
}