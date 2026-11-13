package app.learn.region.model;

public enum Mode {
    CLICK_REGION_BLANK(SubCategory.CLICK, "Region auf Karte finden (schwer)", CapitalOrRegion.REGION, EasyHard.HARD),
    CLICK_REGION_COLORED(SubCategory.CLICK, "Region auf Karte finden (leicht)", CapitalOrRegion.REGION, EasyHard.EASY),
    CLICK_CITY_BLANK(SubCategory.CLICK, "Hauptort auf Karte finden (schwer)", CapitalOrRegion.CAPITAL, EasyHard.HARD),
    CLICK_CITY_COLORED(SubCategory.CLICK, "Hauptort auf Karte finden (leicht)", CapitalOrRegion.CAPITAL, EasyHard.EASY),
    ELIMINATION_BOTH(SubCategory.ELIMINATION, "Elimination Region oder Ort", CapitalOrRegion.BOTH),
    ELIMINATION_CITY(SubCategory.ELIMINATION, "Elimination Hauptort", CapitalOrRegion.CAPITAL),
    ELIMINATION_REGION(SubCategory.ELIMINATION, "Elimination Region", CapitalOrRegion.REGION),
    WRITE_CAPITAL(SubCategory.WRITE, "Name des Hauptorts", CapitalOrRegion.CAPITAL),
    WRITE_REGION(SubCategory.WRITE, "Name der Region", CapitalOrRegion.REGION),
    WRITE_BOTH(SubCategory.WRITE, "Name der Region oder des Hauptorts", CapitalOrRegion.BOTH);

    private final SubCategory subCategory;
    private final String displayName;
    private final CapitalOrRegion capitalOrRegion;
    /** Nur im Klick-Modus belegt, sonst {@code null}. */
    private final EasyHard easyHard;

    Mode(SubCategory subCategory, String displayName, CapitalOrRegion capitalOrRegion) {
        this(subCategory, displayName, capitalOrRegion, null);
    }

    Mode(SubCategory subCategory, String displayName, CapitalOrRegion capitalOrRegion, EasyHard easyHard) {
        if ((subCategory == SubCategory.CLICK) != (easyHard != null))
            throw new RuntimeException("Leicht/schwer gibt es genau im Klick-Modus, nicht in " + subCategory + ".");
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
    
    /**
     * Ein reines Klick-Merkmal: „schwer" ist die blanke Karte, auf der nur der letzte Treffer grün
     * bleibt, „leicht" die farbige, auf der Erledigtes stehen bleibt und keine Klicks mehr annimmt.
     *
     * <p>Andere Modi tragen es bewusst nicht. Bekommt etwa das Benennen einmal eine leichtere
     * Variante, ist das ein eigenes Merkmal mit eigenem Namen — sonst hieße „leicht" je Modus etwas
     * anderes, und ein generischer Leser verwechselt die Bedeutungen.</p>
     */
    public enum EasyHard {
    	EASY, HARD
    }
    
    /** Der Name für die Oberfläche. Kein {@code toString()} — sonst nimmt eine beiläufige
     *  Konkatenation still den Anzeigenamen, wo die Konstante gemeint war. */
    public String getDisplayName() {
    	return displayName;
    }

	public SubCategory getSubCategory() {
		return subCategory;
	}

	public CapitalOrRegion getCapitalOrRegion() {
		return capitalOrRegion;
	}
	
	/** Nur für Klick-Modi; jeder andere Modus hat kein leicht/schwer, die Frage ist dort ein Denkfehler. */
	public EasyHard getEasyHard() {
		if (subCategory != SubCategory.CLICK)
			throw new RuntimeException(this + " ist kein Klick-Modus und kennt kein leicht/schwer.");
		return this.easyHard;
	}
}
