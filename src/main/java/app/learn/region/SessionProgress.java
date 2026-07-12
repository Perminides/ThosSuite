package app.learn.region;

import java.util.HashSet;
import java.util.Set;

import app.learn.Progress;
import app.learn.model.MapShape;

public interface SessionProgress extends Progress {

	void setPresenter(SessionPresenter regionSessionPresenter);
	void start();
	boolean hasProgressed();
	default void resume() {}; // Elimination und Write nicht implementiert
	default void endPause() {}; // Elimination nicht implementiert
	default boolean isPause() {return false;}; // Elimination nicht implementiert
	default void cancel() {};  // Elimination nicht implementiert
	default void elementClicked(String id) {} // CLICK
	default void textInputChanged(String text) {}; // ELIMINATION, WRITE
	
	default Set<String> getIds(Set<MapShape> regions) {
		Set<String> result = new HashSet<>();
		for (MapShape region : regions)
			result.add(region.id());
		return result;
	}
}
