package app.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.controller.RegionSession;
import app.presenter.RegionSessionPresenter;

public class RegionEliminationSessionProgress implements RegionSessionProgress {

	private final RegionSession session;
	private final Set<MapShape> sessionRegions;
	private RegionSessionPresenter presenter;
	private RegionMode mode;

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
		String text = "<p>Folgende Regionen / Orte fehlten noch:</p><p>";
		List<MapShape> sorted = new ArrayList<>(sessionRegions);
		// Dynamische Sortierung je nach Modus
	    if (mode == RegionMode.ELIMINATION_CITY) {
	        sorted.sort(Comparator.comparing(MapShape::capitalName));
	    } else {
	        sorted.sort(Comparator.comparing(MapShape::regionName));
	    }
		for (MapShape region : sorted) {
			if (mode == RegionMode.ELIMINATION_BOTH)
				text += region.regionName() + " (" + region.capitalName() + ")<br/>";
			else if (mode == RegionMode.ELIMINATION_CITY)
				text += region.capitalName() + "<br/>";
			else
				text += region.regionName() + "<br/>";
		}
		text += "</p>";
		session.end(false, null, text, false);
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
	    
	    if (sessionRegions.isEmpty())
	        session.end(true, null, null, true);
	}
}
