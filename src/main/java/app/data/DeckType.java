package app.data;

public enum DeckType {
    GERMANY_CARDS("germany", "Deutschland", DeckCategory.ANKI_DECK, "germanDeckJavaFX.csv", MapMetadata.GERMANY, "newGermanCardsPerDay"),
    MC_CARDS("mc", "Multiple Choice", DeckCategory.ANKI_DECK, "mcDeckJavaFX.csv", null, "newMCCardsPerDay"),
	WORLD_CARDS("world", "Welt", DeckCategory.ANKI_DECK, "weltDeckJavaFX.csv", MapMetadata.WORLD, "newWorldCardsPerDay"),
	BUNDESLAND_SH("lk_sh", "Schleswig-Holstein", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_BW("lk_bw", "Baden-Württemberg", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_BY_NORD("lk_bn", "Bayern Nord", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_BY_SUED("lk_bs", "Bayern Süd", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_BRANDENBURG("lk_bb", "Brandenburg", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_HESSEN("lk_hs", "Hessen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_MVP("lk_mvp", "Mecklenburg-Vorpommern", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_NS("lk_ns", "Niedersachsen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_NRW("lk_nrw", "Nordrhein-Westfalen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_RP("lk_rp", "Rheinland-Pfalz", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_SAARLAND("lk_sl", "Saarland", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_SACHSEN("lk_sc", "Sachsen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_SA("lk_sa", "Sachsen-Anhalt", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	BUNDESLAND_THÜRINGEN("lk_th", "Thüringen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null),
	SPANIEN("es", "Spanien", DeckCategory.REGION_DECK, null, MapMetadata.SPAIN, null);

	private final String id; // UI properties-Dateien, stats und log Tabelle
    private final String displayName; // Im Menü
    private final DeckCategory category;
    private final String deckFileName;
    private final MapMetadata mapDef; 
    
    private final String configValueNewCards;

    DeckType(String id, String displayName, DeckCategory category, String deckFileName, MapMetadata mapDef, String configValueNewCards) {
    	this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.configValueNewCards = configValueNewCards;
        this.deckFileName = deckFileName;
        this.mapDef = mapDef;
    }

    public String getId() {
    	return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public DeckCategory getCategory() {
        return category;
    }
    
    public String getConfigValueNewCards() {
        return configValueNewCards;
    }
    
    public String getDeckFileName() {
        return deckFileName;
    }
    
    public MapMetadata getMapMetadata() {
    	return mapDef;
    }
}