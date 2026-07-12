package app.learn.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import app.learn.model.MapShape;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;

public class WriteSessionProgress implements SessionProgress {

	private final RegionSession session;
	private final Set<MapShape> sessionRegions;
	private final List<MapShape> toLearnRegions;
	private SessionPresenter presenter;
	private Mode mode;
	private int currentIndex = -1;
	private boolean isEndPause = false;

	public WriteSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionSession regionSession) {
		this.sessionRegions = regions;
		this.session = regionSession;
		this.mode = spec.getMode();
		toLearnRegions = new ArrayList<>(sessionRegions);
		Collections.shuffle(toLearnRegions);
	}

	@Override
	public void setPresenter(SessionPresenter regionSessionPresenter) {
		this.presenter = regionSessionPresenter;
	}

	@Override
	public void start() {
		presenter.prepareWriteSession(getIds(sessionRegions));
		currentIndex++;
		presenter.weWaitForWriteText(toLearnRegions.get(currentIndex).id());
	}

	@Override
	public void cancel() {
		if (isEndPause) {
			endPause();
			return;
		}
		
		String text = "";
		MapShape currentRegion = toLearnRegions.get(currentIndex);
		if (mode == Mode.WRITE_BOTH)
			text += currentRegion.regionName() + " (" + currentRegion.capitalName() + ")";
		else if (mode == Mode.WRITE_CAPITAL)
			text += currentRegion.capitalName();
		else
			text += currentRegion.regionName();
		presenter.setCorrectText(text);
		isEndPause = true;
	}

	@Override
	public void textInputChanged(String text) {
		MapShape currentRegion = toLearnRegions.get(currentIndex);

		boolean isMatch = switch (mode) {
		case WRITE_BOTH -> currentRegion.isMatching(text);
		case WRITE_CAPITAL -> currentRegion.isMatchingCapital(text);
		case WRITE_REGION -> currentRegion.isMatchingRegion(text);
		default -> throw new RuntimeException("Das kommt jetzt einigermaßen unerwartet :)");
		};

		if (!isMatch)
			return;

		presenter.handleCorrectAnswers(Set.of(currentRegion.id()));
		
		currentIndex++;
		if (currentIndex >= toLearnRegions.size())
			session.end(true, null, null, false);
		else
			presenter.weWaitForWriteText(toLearnRegions.get(currentIndex).id());
	}
	
	// !Sofort: Viel zu abruptes Ende bei FreePlay! Das muss nochmal durchdacht und refactoret werden fürs freie Spiel!
	@Override
	public void endPause() {
		if (!isEndPause)
			return;
		MapShape currentRegion = toLearnRegions.get(currentIndex);
		String result = "Folgendes Element nicht erkannt: \n\n";
		if (mode == Mode.WRITE_BOTH)
			result += currentRegion.regionName() + " (" + currentRegion.capitalName() + ")";
		else if (mode == Mode.WRITE_CAPITAL)
			result += currentRegion.capitalName();
		else
			result += currentRegion.regionName();
		session.end(false, toLearnRegions.get(currentIndex).id(), result, false);
		
	}

	@Override
	public boolean hasProgressed() {
		return currentIndex > 0;
	}
}
