package app.data;

public enum Deck {
    // Anki Decks
    GERMANY_CARDS("germany", "Deutschland", "germany", DeckCategory.ANKI_DECK, "germanDeckJavaFX.csv", MapMetadata.GERMANY, "newGermanCardsPerDay", false),
    MC_CARDS("mc", "Multiple Choice", "mc", DeckCategory.ANKI_DECK, "mcDeckJavaFX.csv", null, "newMCCardsPerDay", false),
    WORLD_CARDS("world", "Welt", "world", DeckCategory.ANKI_DECK, "weltDeckJavaFX.csv", MapMetadata.WORLD, "newWorldCardsPerDay", false),
    HANNOVER_CARDS("hannover", "Hannover", "hannover", DeckCategory.ANKI_DECK, "hannoverDeckJavaFX.csv", MapMetadata.HANNOVER, "newWorldCardsPerDay", false),
    
    // Bundesländer
    BUNDESLAND_SH("lk_sh", "Schleswig-Holstein", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BW("lk_bw", "Baden-Württemberg", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BY_NORD("lk_bn", "Bayern Nord", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BY_SUED("lk_bs", "Bayern Süd", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BRANDENBURG("lk_bb", "Brandenburg", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_HESSEN("lk_hs", "Hessen", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_MVP("lk_mvp", "Mecklenburg-Vorpommern", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_NS("lk_ns", "Niedersachsen", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_NRW("lk_nrw", "Nordrhein-Westfalen", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_RP("lk_rp", "Rheinland-Pfalz", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_SAARLAND("lk_sl", "Saarland", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_SACHSEN("lk_sc", "Sachsen", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_SA("lk_sa", "Sachsen-Anhalt", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_THÜRINGEN("lk_th", "Thüringen", "lk", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    
    // Andere
    SPANIEN("es", "Spanien", "es", DeckCategory.REGION_DECK, null, MapMetadata.SPAIN, null, true),
    ITALIEN("it", "Italien", "it", DeckCategory.REGION_DECK, null, MapMetadata.ITALY, null, true),
	USA("us", "USA", "us", DeckCategory.REGION_DECK, null, MapMetadata.USA, null, true),
	CARIBBEAN("cs", "Karibik", "cs", DeckCategory.REGION_DECK, null, MapMetadata.CARIBBEAN, null, true),
	ENGLAND("en", "England", "en", DeckCategory.REGION_DECK, null, MapMetadata.ENGLAND, null, true),
	SCHWEIZ("ch", "Schweiz", "ch", DeckCategory.REGION_DECK, null, MapMetadata.SCHWEIZ, null, true),
	OZEANIEN("oz", "Ozeanien", "oz", DeckCategory.REGION_DECK, null, MapMetadata.OZEANIEN, null, true),
	HANNOVER_STADTTEILE("hs", "Hannover Stadt", "hs", DeckCategory.REGION_DECK, null, MapMetadata.HANNOVER_STADTTEILE, null, false),
	HANNOVER_REGION("hr", "Hannover Region", "hr", DeckCategory.REGION_DECK, null, MapMetadata.HANNOVER_REGION, null, false),
	AUSTRIA("au", "Österreich", "au", DeckCategory.REGION_DECK, null, MapMetadata.AUSTRIA, null, true),
	BAVARIA("br", "Bayern Reg", "br", DeckCategory.REGION_DECK, null, MapMetadata.BAVARIA, null, true),
	
	BERLIN_WEST("be_we", "Berlin West", "be", DeckCategory.REGION_DECK, null, MapMetadata.BERLIN, null, false),
	BERLIN_NORD("be_no", "Berlin Nord", "be", DeckCategory.REGION_DECK, null, MapMetadata.BERLIN, null, false),
	BERLIN_MITTE("be_mi", "Berlin Mitte", "be", DeckCategory.REGION_DECK, null, MapMetadata.BERLIN, null, false),
	BERLIN_OST("be_os", "Berlin Ost", "be", DeckCategory.REGION_DECK, null, MapMetadata.BERLIN, null, false);
	

    private final String id; 
    private final String displayName; 
    private final String mapName; // Needed for skin most and for all...
    private final DeckCategory category;
    private final String deckFileName; // Only needed for Anki
    private final MapMetadata mapDef; 
    private final String configValueNewCards;
    private final boolean hasCapital;

    Deck(String id, String displayName, String mapName, DeckCategory category, String deckFileName, MapMetadata mapDef, String configValueNewCards, boolean hasCapital) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.mapName = mapName;
        this.configValueNewCards = configValueNewCards;
        this.deckFileName = deckFileName;
        this.mapDef = mapDef;
        this.hasCapital = hasCapital;
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
    
    public boolean hasCapital() {
        return hasCapital;
    }
    
    public String getMapName() {
    	return mapName;
    }
}