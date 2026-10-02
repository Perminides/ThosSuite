package app.learn.region;

import java.util.HashSet;
import java.util.Set;

import app.learn.model.MapShape;
import app.learn.region.model.SessionSpec;

public class EliminationSessionProgress extends SessionProgress {

	private final Set<MapShape> sessionRegions;
	private boolean hasProgressed = false;

	public EliminationSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionDeckService service,
			Runnable onFinished) {
		super(spec, service, onFinished);
		this.sessionRegions = regions;
	}

	@Override
	public void start() {
		presenter.weWaitForEliminationText(getIds(sessionRegions));
	}

	@Override
	public void cancel() {
		finishWithMisses("Folgende Elemente wurden nicht eliminiert:", sessionRegions);
	}

	@Override
	public void textInputChanged(String text) {
	    Set<MapShape> hits = new HashSet<>();
	    
	    for (MapShape region : sessionRegions) {
	        if (matches(region, text))
	            hits.add(region);
	    }
	    
	    if (hits.isEmpty())
	        return;
	    
	    sessionRegions.removeAll(hits);
	    presenter.handleCorrectAnswers(getIds(hits));
	    hasProgressed = true;
	    
	    if (sessionRegions.isEmpty())
	    	finishCorrect();
	}

	@Override
	public boolean hasProgressed() {
		return hasProgressed;
	}
}
