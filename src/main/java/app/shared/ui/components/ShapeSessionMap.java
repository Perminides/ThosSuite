package app.shared.ui.components;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.shared.model.ShapeGeometry;
import javafx.scene.Node;

/**
 * {@link SessionMap} auf Basis einer {@link ShapeMapPane} — die Deutschlandkarte.
 *
 * <p>Die Shape-Karte spricht ohnehin Ids, hier wird also nur umbenannt.</p>
 */
public class ShapeSessionMap implements SessionMap {

	private final ShapeMapPane pane;

	public ShapeSessionMap(List<ShapeGeometry> geometrien, String mapName, String kategorie,
			Consumer<String> onClicked) {
		this.pane = new ShapeMapPane(geometrien, mapName, kategorie);
		this.pane.setClickListener(onClicked);
		this.pane.moveAllToActive();
	}

	@Override public void reset()                       { pane.moveAllToActive(); }
	@Override public void setActive(boolean active)     { pane.setInteractive(active); }
	@Override public void markCorrect(Set<String> ids)  { pane.addToCorrect(ids); }
	@Override public void markIncorrect(String id)      { pane.addToIncorrect(id); }
	@Override public void mark(Set<String> ids)         { pane.addToMarked(ids); }

	/** Die Shape-Karte kennt kein „gerade gefragt" — die Deutschland-Session nutzte das nie. */
	@Override public void markInQuestion(Set<String> ids) { }

	@Override public Node getView() { return pane.getView(); }
}
