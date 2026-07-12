package app.learn.region;

import java.util.HashSet;
import java.util.Set;

import app.learn.model.MapShape;
import app.learn.region.model.SessionSpec;

public class EliminationSessionProgress implements SessionProgress {

	private final RegionSession session;
	private final Set<MapShape> sessionRegions;
	private SessionPresenter presenter;
	private SessionSpec spec;
	private boolean hasProgressed = false;

	public EliminationSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionSession regionSession) {
		this.sessionRegions = regions;
		this.session = regionSession;
		this.spec = spec;
	}

	@Override
	public void setPresenter(SessionPresenter regionSessionPresenter) {
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
			switch (spec.getMode()) {
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
	        boolean isMatch = switch (spec.getMode()) {
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
	    
	    if (sessionRegions.isEmpty()) {
	    	if (spec.isPlaySession())
	    		session.end(true, null, "Super gemacht!", true);
	    	else
	    		session.end(true, null, null, true);
	    }
	}

	@Override
	public boolean hasProgressed() {
		return hasProgressed;
	}
}
