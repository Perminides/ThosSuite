package app.learn.anki.model;

import app.learn.model.Deck;
import app.learn.model.LearnSessionInfo;

public class AnkiLearnSessionInfo extends LearnSessionInfo {
	private final Deck deckType;
	private final int dueNow, dueToday;

	public AnkiLearnSessionInfo(Deck type, int dueNow, int dueToday) {
		this.deckType = type;
		this.dueNow = dueNow;
		this.dueToday = dueToday;
	}
	
	@Override
	public String formatForMenu() {
		return deckType.getDisplayName() + " (" + dueNow + " / " + dueToday + " fällig)";
	}

	@Override
	public boolean isStillDueToday() {
		return (dueNow != 0);
	}
	
    public Deck getDeckType() {
    	return deckType;
    }
}