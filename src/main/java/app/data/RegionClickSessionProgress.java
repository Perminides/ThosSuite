package app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.controller.RegionSession;
import app.presenter.RegionSessionPresenter;

public class RegionClickSessionProgress implements RegionSessionProgress{
	
	public record QuizElement(String toFind, String shapeId) {
		String getToFind() {
			return toFind;
		}
		
		String getShapeId() {
			return shapeId;
		}
	};
	
	private final RegionSession session;
	private final Set<String> sessionRegions;
	private final List<QuizElement> quizElements;
	private String wrongClicked = "";
	private int currentIndex = -1;
	
	private RegionSessionPresenter presenter;

	public RegionClickSessionProgress(Set<MapShape> regions, RegionSessionSpec spec, RegionSession regionSession) {
		this.session = regionSession;
		this.sessionRegions = new HashSet<>();
		for (MapShape region : regions) {
			sessionRegions.add(region.id());
		}
		quizElements = new ArrayList<>();
		for (MapShape region : regions) {
			if (!region.isInteractive())
				continue;
			
			String name = spec.getMode().getCapitalOrRegion() == RegionMode.CapitalOrRegion.CAPITAL ? region.capitalName() : region.regionName();
			quizElements.add(new QuizElement(name, region.id()));
		}
		Collections.shuffle(quizElements);
	}

	@Override
	public void start() {
		nextStep();
	}
	
	@Override
	public void resume() {
		wrongClicked = "";
		presenter.undoClick();
		currentIndex--;
		nextStep();
	}

	@Override
	public void elementClicked(String id) {
		if (quizElements.get(currentIndex).getShapeId().equals(id)) {
			presenter.handleClickResult(id, true, null);
			sessionRegions.remove(id);
			nextStep();
		} else {
			wrongClicked = id;
			presenter.handleClickResult(id, false, quizElements.get(currentIndex).getShapeId());
		}
	}

	@Override
	public void endPause() {
		if (!wrongClicked.isEmpty())
			session.end(false, wrongClicked, "", true);
	}
	
	@Override
	public boolean isPause() {
		return !wrongClicked.isEmpty();
	}
	
	@Override
	public void setPresenter(RegionSessionPresenter presenter) {
		this.presenter = presenter;
	}
	
	private void nextStep() {
		currentIndex++;
		if (currentIndex >= quizElements.size())
			session.end(true, null, "", true);
		else {
			presenter.showQuestion(quizElements.get(currentIndex).getToFind());
			presenter.weWaitForClick(sessionRegions);
		}
	}
}
