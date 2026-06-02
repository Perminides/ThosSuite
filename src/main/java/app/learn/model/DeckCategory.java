package app.learn.model;
public enum DeckCategory {
    ANKI_DECK ("anki", true),
    REGION_DECK ("region", false),
    FAST_DECK ("fast", true);
	
	private final boolean retryAllowedToday;
	private final String name;
	
	DeckCategory(String name, boolean retryAllowedToday) {
		this.retryAllowedToday = retryAllowedToday;
		this.name = name;
	}
	
	public boolean isRetryAllowedToday() {
		return retryAllowedToday;
	}
	
	@Override
	public String toString() {
		return name;
	}
}