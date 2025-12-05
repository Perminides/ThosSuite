package app.data;

public enum RegionMode {
    CLICK_REGION_BLANK(SubCategory.CLICK, "Region auf Karte finden (schwer)", CapitalOrRegion.REGION, EasyHard.HARD),
    CLICK_REGION_COLORED(SubCategory.CLICK, "Region auf Karte finden (leicht)", CapitalOrRegion.REGION, EasyHard.EASY),
    CLICK_CITY_BLANK(SubCategory.CLICK, "Hauptort auf Karte finden (schwer)", CapitalOrRegion.CAPITAL, EasyHard.HARD),
    CLICK_CITY_COLORED(SubCategory.CLICK, "Hauptort auf Karte finden (leicht)", CapitalOrRegion.CAPITAL, EasyHard.EASY),
    ELIMINATION_BOTH(SubCategory.ELIMINATION, "Elimination Region oder Ort", CapitalOrRegion.BOTH, EasyHard.EASY),
    ELIMINATION_CITY(SubCategory.ELIMINATION, "Elimination Hauptort", CapitalOrRegion.CAPITAL, EasyHard.HARD),
    ELIMINATION_REGION(SubCategory.ELIMINATION, "Elimination Region", CapitalOrRegion.REGION, EasyHard.HARD),
    WRITE_CAPITAL(SubCategory.WRITE, "Name des Hauptorts", CapitalOrRegion.CAPITAL, EasyHard.HARD),
    WRITE_REGION(SubCategory.WRITE, "Name der Region", CapitalOrRegion.REGION, EasyHard.HARD),
    WRITE_BOTH(SubCategory.WRITE, "Name der Region oder des Hauptorts", CapitalOrRegion.BOTH, EasyHard.EASY);
    
    private final SubCategory subCategory;
    private final String displayName;
    private final CapitalOrRegion capitalOrRegion;
    private final EasyHard easyHard;
    
    RegionMode(SubCategory subCategory, String displayName, CapitalOrRegion capitalOrRegion, EasyHard easyHard) {
        this.subCategory = subCategory;
        this.displayName = displayName;
        this.capitalOrRegion = capitalOrRegion;
        this.easyHard = easyHard;
    }
    
    public enum SubCategory {
        CLICK, ELIMINATION, WRITE
    }
    
    public enum CapitalOrRegion {
    	CAPITAL, REGION, BOTH
    }
    
    public enum EasyHard {
    	EASY, HARD
    }
    
    @Override
    public String toString() {
    	return displayName;
    }

	public SubCategory getSubCategory() {
		return subCategory;
	}

	public CapitalOrRegion getCapitalOrRegion() {
		return capitalOrRegion;
	}
	
	public EasyHard getEasyHard() {
		return this.easyHard;
	}
}
