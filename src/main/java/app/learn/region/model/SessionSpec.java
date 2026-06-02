package app.learn.region.model;

import java.util.Set;

import app.learn.model.Deck;

/**
 * A class that holds everything needed to identify a learnable RegionSession
 * 
 * Unfortunately for free playing of a region, you can merge multiple regions based on the same map
 * like Berlin-Mitte and Berlin-Ost. Therefore we introduced the additionalDeckTypes.
 * This should only ever be used when gathering the active shapes for a free play session...
 */
public class SessionSpec {
	private final Deck deckType;
	private final Mode mode;
	private final Set<Deck> additionalDeckTypesForFreePlay;
	private final boolean isPlaySession;
	
	public SessionSpec(Deck deck, Mode mode) {
		this(deck, mode, null, false);
	}
	
	public SessionSpec(Deck deck, Mode mode, Set<Deck> additionals, boolean isPlaySession) {
		this.deckType = deck;
		this.mode = mode;
		this.additionalDeckTypesForFreePlay = additionals;
		this.isPlaySession = isPlaySession;
	}

	public Deck getDeckType() {
		return deckType;
	}

	public Mode getMode() {
		return mode;
	}
	
	public Set<Deck> getAdditonalDeckTypesForPlay() {
		return additionalDeckTypesForFreePlay;
	}
	
	public boolean isPlaySession() {
		return isPlaySession;
	}
}