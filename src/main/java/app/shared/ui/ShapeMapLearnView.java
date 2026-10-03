package app.shared.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.shared.model.AnkiCallbacks;
import app.shared.model.ShapeGeometry;
import app.shared.skin.LearnComponent;
import app.shared.skin.SkinService;
import app.shared.ui.components.map.LearnMap;
import app.shared.ui.components.map.ShapeMapPane;

/** Shape-Karte, Eingabefeld, leerer Hintergrund. Aktuell gibt es nur das Deutschlanddeck dafür, aber wer weiß...*/
public class ShapeMapLearnView extends AnkiLearnView {

	private final List<ShapeGeometry> geometries;

	public ShapeMapLearnView(String deckId, String mapName, String category,
			List<ShapeGeometry> geometries, AnkiCallbacks callbacks) {
		super(deckId, mapName, category, callbacks);
		this.geometries = geometries;
		rebuild(); // muss die letzte Zeile sein, siehe AnkiLearnView
	}

	@Override
	protected LearnMap createMap() {
		ShapeMapPane map = new ShapeMapPane(geometries,
				SkinService.get().learnComponentBounds(deckId(), mapName(), category(), LearnComponent.MAP),
				allIds());
		map.setClickListener(callbacks().mapElementClicked());
		return map;
	}

	/** Hier ist die ganze Karte das Spielfeld — die Pane behält davon, was Klicks annehmen kann. */
	private Set<String> allIds() {
		Set<String> ids = new HashSet<>();
		for (ShapeGeometry geometry : geometries)
			ids.add(geometry.id());
		return ids;
	}

	@Override protected boolean hasInputField()      { return true; }
}
