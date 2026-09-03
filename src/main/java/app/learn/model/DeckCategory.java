package app.learn.model;
public enum DeckCategory {
    ANKI_DECK ("anki"),
    REGION_DECK ("region");

	private final String name;

	DeckCategory(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}