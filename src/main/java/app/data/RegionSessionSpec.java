package app.data;

public class RegionSessionSpec {
	private final DeckType deckType;
	private final RegionMode mode;
	
	public RegionSessionSpec(DeckType deck, RegionMode mode) {
		this.deckType = deck;
		this.mode = mode;
	}

	public DeckType getDeckType() {
		return deckType;
	}

	public RegionMode getMode() {
		return mode;
	}
	
}