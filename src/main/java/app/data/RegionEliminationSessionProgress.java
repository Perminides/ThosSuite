package app.data;

import java.util.HashSet;
import java.util.Set;

import app.controller.RegionSession;
import app.presenter.RegionSessionPresenter;

public class RegionEliminationSessionProgress implements RegionSessionProgress {

	private final RegionSession session;
	private final Set<MapShape> sessionRegions;
	private RegionSessionPresenter presenter;
	private RegionMode mode;
	private boolean hasProgressed = false;

	public RegionEliminationSessionProgress(Set<MapShape> regions, RegionSessionSpec spec, RegionSession regionSession) {
		this.sessionRegions = regions;
		this.session = regionSession;
		this.mode = spec.getMode();
	}

	@Override
	public void setPresenter(RegionSessionPresenter regionSessionPresenter) {
		this.presenter = regionSessionPresenter;
	}

	@Override
	public void start() {
		presenter.weWaitForEliminationText(getIds(sessionRegions));
	}

	@Override
	public void cancel() {
		String result = "Folgende Elemente wurden nicht eliminiert: \n\n";
		for (MapShape mapShape : sessionRegions) {
			switch (mode) {
            	case ELIMINATION_BOTH -> result = result + mapShape.regionName() + " - " + mapShape.capitalName() + "\n";
            	case ELIMINATION_CITY -> result = result + mapShape.capitalName() + "\n";
            	case ELIMINATION_REGION -> result = result + mapShape.regionName() + "\n";
            	default -> throw new RuntimeException("Das kommt jetzt einigermaßen unerwartet :)");
			};
		}
		session.end(false, null, result, false);
	}

	@Override
	public void textInputChanged(String text) {
	    Set<MapShape> matches = new HashSet<>();
	    
	    for (MapShape region : sessionRegions) {
	        boolean isMatch = switch (mode) {
	            case ELIMINATION_BOTH -> region.isMatching(text);
	            case ELIMINATION_CITY -> region.isMatchingCapital(text);
	            case ELIMINATION_REGION -> region.isMatchingRegion(text);
	            default -> throw new RuntimeException("Das kommt jetzt einigermaßen unerwartet :)");
	        };
	        
	        if (isMatch) {
	            matches.add(region);
	        }
	    }
	    
	    if (matches.isEmpty())
	        return;
	    
	    sessionRegions.removeAll(matches);
	    presenter.handleCorrectAnswers(getIds(matches));
	    hasProgressed = true;
	    
	    if (sessionRegions.isEmpty())
	        session.end(true, null, null, true);
	}

	@Override
	public boolean hasProgressed() {
		return hasProgressed;
	}
}
