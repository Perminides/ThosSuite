package app.data;

public class RegionLearnSessionInfo extends LearnSessionInfo {
	private final RegionSessionSpec spec;
	private final int level;
	private final boolean isDueToday;

	RegionLearnSessionInfo(RegionSessionSpec spec, int level, boolean isDueToday) {
		this.spec = spec;
		this.level = level;
		this.isDueToday = isDueToday;
	}

	// getter für spec
	@Override
	public String formatForMenu() {
		return spec.getDeckType().getDisplayName() + ": " + spec.getMode().toString() + " (" + level + ")";
	}

	@Override
	public DeckCategory getCategory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isStillDueToday() {
		return isDueToday;
	}
	
	public RegionSessionSpec getSpec() {
		return spec;
	}
}