package app.shared.ui;

import java.util.List;

import app.shared.model.LearnCallbacks;
import app.shared.model.ShapeGeometry;
import app.shared.skin.LearnComponent;
import app.shared.skin.SkinService;
import app.shared.ui.components.map.LearnMap;
import app.shared.ui.components.map.ShapeMapPane;

/** Shape-Karte, Eingabefeld, leerer Hintergrund. Aktuell gibt es nur das Deutschlanddeck dafür, aber wer weiß...*/
public class ShapeMapSessionView extends AnkiLearnView {

	private final List<ShapeGeometry> geometrien;

	public ShapeMapSessionView(String deckId, String mapName, String kategorie,
			List<ShapeGeometry> geometrien, LearnCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		this.geometrien = geometrien;
		rebuild(); // muss die letzte Zeile sein, siehe AnkiSessionView
	}

	@Override
	protected LearnMap createMap() {
		ShapeMapPane karte = new ShapeMapPane(geometrien,
				SkinService.get().learnComponentBounds(mapName(), kategorie(), LearnComponent.MAP));
		karte.setClickListener(callbacks().mapElementClicked());
		karte.reset();
		return karte;
	}

	@Override protected boolean hasInputField()      { return true; }
}
