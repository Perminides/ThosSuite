package app.data;

public class AnkiLearnSessionInfo extends LearnSessionInfo {
	private final DeckType deckType;
	private final int dueNow, dueToday;

	public AnkiLearnSessionInfo(DeckType type, int dueNow, int dueToday) {
		this.deckType = type;
		this.dueNow = dueNow;
		this.dueToday = dueToday;
	}
	
	@Override
	public String formatForMenu() {
		return deckType.getDisplayName() + " (" + dueNow + " / " + dueToday + " fällig)";
	}

	@Override
	public DeckCategory getCategory() {
		return DeckCategory.ANKI_DECK;
	}

	@Override
	public boolean isStillDueToday() {
		return (dueNow != 0);
	}
	
    public DeckType getDeckType() {
    	return deckType;
    }
}