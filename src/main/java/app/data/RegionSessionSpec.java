package app.data;

import java.util.Set;

/**
 * A class that holds everything needed to identify a learnable RegionSession
 * 
 * Unfortunately for free playing of a region, you can merge multiple regions based on the same map
 * like Berlin-Mitte and Berlin-Ost. Therefore we introduced the additionalDeckTypes.
 * This should only ever be used when gathering the active shapes for a free play session...
 */
public class RegionSessionSpec {
	private final Deck deckType;
	private final RegionMode mode;
	private final Set<Deck> additionalDeckTypesForFreePlay;
	private final boolean isPlaySession;
	
	public RegionSessionSpec(Deck deck, RegionMode mode) {
		this(deck, mode, null, false);
	}
	
	public RegionSessionSpec(Deck deck, RegionMode mode, Set<Deck> additionals, boolean isPlaySession) {
		this.deckType = deck;
		this.mode = mode;
		this.additionalDeckTypesForFreePlay = additionals;
		this.isPlaySession = isPlaySession;
	}

	public Deck getDeckType() {
		return deckType;
	}

	public RegionMode getMode() {
		return mode;
	}
	
	public Set<Deck> getAdditonalDeckTypesForPlay() {
		return additionalDeckTypesForFreePlay;
	}
	
	public boolean isPlaySession() {
		return isPlaySession;
	}
}