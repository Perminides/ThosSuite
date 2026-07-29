package app.shared.ui;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import app.shared.model.LearnCallbacks;
import app.shared.model.ShapeGeometry;
import app.shared.skin.LearnComponent;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.components.map.ImageMapPane;
import app.shared.ui.components.map.LearnMap;

/** Welt und Hannover: Bild-Karte, Eingabefeld, leerer Hintergrund. */
public class ImageMapSessionView extends AnkiLearnView {

	private final Function<Set<String>, List<ShapeGeometry>> geometrieFuer;

	public ImageMapSessionView(String deckId, String mapName, String kategorie,
			Function<Set<String>, List<ShapeGeometry>> geometrieFuer, LearnCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		this.geometrieFuer = geometrieFuer;
		rebuild(); // muss die letzte Zeile sein, siehe AnkiSessionView
	}

	@Override
	protected LearnMap createMap() {
		Skin skin = SkinService.get();
		ImageMapPane karte = new ImageMapPane(
				skin.mapImages(mapName()),
				skin.getOverlayContentBounds(deckId()),
				skin.learnComponentBounds(deckId(), kategorie(), LearnComponent.MAP),
				geometrieFuer);
		karte.setListener(callbacks().mapElementClicked());
		return karte;
	}

	@Override protected boolean hasInputField()      { return true; }
}
