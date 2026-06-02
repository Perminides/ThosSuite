package app.learn.region.model;

import app.learn.model.LearnSessionInfo;

public class RegionLearnSessionInfo extends LearnSessionInfo {
	private final SessionSpec spec;
	private final int level;
	private final boolean isDueToday;

	public RegionLearnSessionInfo(SessionSpec spec, int level, boolean isDueToday) {
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
	public boolean isStillDueToday() {
		return isDueToday;
	}
	
	public SessionSpec getSpec() {
		return spec;
	}
}