package app.data;

public enum DeckType {
    // Anki Decks
    GERMANY_CARDS("germany", "Deutschland", DeckCategory.ANKI_DECK, "germanDeckJavaFX.csv", MapMetadata.GERMANY, "newGermanCardsPerDay", false),
    MC_CARDS("mc", "Multiple Choice", DeckCategory.ANKI_DECK, "mcDeckJavaFX.csv", null, "newMCCardsPerDay", false),
    WORLD_CARDS("world", "Welt", DeckCategory.ANKI_DECK, "weltDeckJavaFX.csv", MapMetadata.WORLD, "newWorldCardsPerDay", false),
    
    // Bundesländer
    BUNDESLAND_SH("lk_sh", "Schleswig-Holstein", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BW("lk_bw", "Baden-Württemberg", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BY_NORD("lk_bn", "Bayern Nord", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BY_SUED("lk_bs", "Bayern Süd", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_BRANDENBURG("lk_bb", "Brandenburg", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_HESSEN("lk_hs", "Hessen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_MVP("lk_mvp", "Mecklenburg-Vorpommern", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_NS("lk_ns", "Niedersachsen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_NRW("lk_nrw", "Nordrhein-Westfalen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_RP("lk_rp", "Rheinland-Pfalz", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_SAARLAND("lk_sl", "Saarland", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_SACHSEN("lk_sc", "Sachsen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_SA("lk_sa", "Sachsen-Anhalt", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    BUNDESLAND_THÜRINGEN("lk_th", "Thüringen", DeckCategory.REGION_DECK, null, MapMetadata.GERMANY, null, true),
    
    // Andere
    SPANIEN("es", "Spanien", DeckCategory.REGION_DECK, null, MapMetadata.SPAIN, null, true),
    ITALIEN("it", "Italien", DeckCategory.REGION_DECK, null, MapMetadata.ITALY, null, true),
	USA("us", "USA", DeckCategory.REGION_DECK, null, MapMetadata.USA, null, true),
	CARIBBEAN("cs", "Karibik", DeckCategory.REGION_DECK, null, MapMetadata.CARIBBEAN, null, true);

    private final String id; 
    private final String displayName; 
    private final DeckCategory category;
    private final String deckFileName;
    private final MapMetadata mapDef; 
    private final String configValueNewCards;
    private final boolean hasCapital;

    DeckType(String id, String displayName, DeckCategory category, String deckFileName, MapMetadata mapDef, String configValueNewCards, boolean hasCapital) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
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
}