package app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
	private final Set<String> wrongClicked = new TreeSet<>();
	private final RegionSessionSpec spec;
	private boolean isPaused = false;
	private int currentIndex = -1;
	
	private RegionSessionPresenter presenter;

	public RegionClickSessionProgress(Set<MapShape> regions, RegionSessionSpec spec, RegionSession regionSession) {
		this.session = regionSession;
		this.spec = spec;
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
		wrongClicked.clear(); // Resume wird nur in echten Lernsessions aufgerufen. Der falsche Klick soll dann ignoriert werden.
		isPaused = false;
		presenter.undoClick();
		currentIndex--;
		nextStep();
	}

	@Override
	public void elementClicked(String id) {
		if (isPaused)
			endPause();
		
		if (quizElements.get(currentIndex).getShapeId().equals(id)) {
			presenter.handleClickResult(id, true, null);
			sessionRegions.remove(id);
			nextStep();
		} else {
			wrongClicked.add(quizElements.get(currentIndex).shapeId());
			isPaused = true; 
			presenter.handleClickResult(id, false, quizElements.get(currentIndex).getShapeId());
		}
	}

	@Override
	public void endPause() { // Durch Klick im Presenter oder Pause-Taste 
		if (spec.isPlaySession()) {
			isPaused = false;
			nextStep();
		} else if (isPaused) {
			session.end(false, getId(wrongClicked), "Statt " + quizElements.get(currentIndex).toFind() + " wurde " + getNameForId(wrongClicked) + " geklickt.", true);
		}
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
		if (currentIndex >= quizElements.size()) {
			if (spec.isPlaySession()) {
				if (wrongClicked.isEmpty()) 	// Freies Spiel ohne Fehler. Super!
					session.end(true, null, "Super gemacht!", false);
				else { 							// Freies Spiel mit Fehlern. Die listen wir auf
					String result = "Folgende Elemente wurden nicht erkannt: \n\n";
					for (String wrongId : wrongClicked) {
						result = result + getNameForId(wrongId) + "\n";
					}
					session.end(false, "", result, false);
				}
			} else {
				if (wrongClicked.isEmpty()) 	// Lernsession korrekt beantwortet
					session.end(true, null, null, false);
				else {
					throw new RuntimeException("Moment. Entweder wird ein falscher Klick zurückgenommen oder es wird ohne nextStep beendet. Hierhin dürfte der Code nie kommen. Untersuchen!");
				}
			}
		}
		else {
			presenter.showQuestion(quizElements.get(currentIndex).getToFind());
			presenter.weWaitForClick(sessionRegions);
		}
	}
	
	private String getId(Set<String> ids) {
		if (ids.size() != 1)
			throw new RuntimeException("Moment. Wieso gibt es mehr als einen Klick hier?");
		for (String element : ids) {
			return element;
		}
		return null;
	}
	
	private String getNameForId(String wrongClicked) {
		return getNameForId (Set.of(wrongClicked));
	}

	
	private String getNameForId(Set<String> wrongClicked) {
		if (wrongClicked.size() != 1)
			throw new RuntimeException("Moment. Wieso gibt es mehr als einen flaschen Klick hier?");
		for (QuizElement element : quizElements) {
			if (wrongClicked.contains(element.shapeId))
				return element.toFind();
		}
		return "";
	}

	@Override
	public boolean hasProgressed() {
		return currentIndex > 0;
	}
}
