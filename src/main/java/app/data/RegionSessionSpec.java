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
	private final DeckType deckType;
	private final RegionMode mode;
	private final Set<DeckType> additionalDeckTypesForFreePlay;
	private final boolean isPlaySession;
	
	public RegionSessionSpec(DeckType deck, RegionMode mode) {
		this(deck, mode, null, false);
	}
	
	public RegionSessionSpec(DeckType deck, RegionMode mode, Set<DeckType> additionals, boolean isPlaySession) {
		this.deckType = deck;
		this.mode = mode;
		this.additionalDeckTypesForFreePlay = additionals;
		this.isPlaySession = isPlaySession;
	}

	public DeckType getDeckType() {
		return deckType;
	}

	public RegionMode getMode() {
		return mode;
	}
	
	public Set<DeckType> getAdditonalDeckTypesForPlay() {
		return additionalDeckTypesForFreePlay;
	}
	
	public boolean isPlaySession() {
		return isPlaySession;
	}
}