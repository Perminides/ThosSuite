package app.shared.model;

// !Später auf das noch zu definierende CardInterface umswitchen, wir wollen ja alle Kartenlisten sortieren können, auch FastHints... 
public enum CardSortOrder {
	RANDOM("Zufällig"),
	BY_LEVEL_DESC("Höchstes Level zuerst"),
	BY_LEVEL_ASC("Niedrigstes Level zuerst"), 
	BY_WRONG_COUNT_DESC("Schwere zuerst"),
	BY_WRONG_COUNT_ASC("Leichte zuerst");

	private final String displayName;

	CardSortOrder(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
