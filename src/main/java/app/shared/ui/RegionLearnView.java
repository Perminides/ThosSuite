package app.shared.ui;

import java.util.List;
import java.util.Set;

import app.shared.model.RegionCallbacks;
import app.shared.model.ScreenView;
import app.shared.model.ShapeGeometry;
import app.shared.model.ShapeMapState;
import app.shared.skin.LearnComponent;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteInfoLabel;
import app.shared.ui.components.SuiteTextField;
import app.shared.ui.components.map.ShapeMapPane;

/**
 * Die Oberfläche einer Region-Session: eine Karte, dazu entweder ein Fragefeld (Klick-Modi) oder
 * ein Eingabefeld (Schreib- und Eliminierungs-Modi).
 *
 * <p>Sie übersetzt Absicht in Anzeige: „stell eine Frage" wird hier zu „setze diesen Text ins
 * Fragefeld". Der Presenter im Feature sagt nur, <em>was</em> passieren soll — er kennt weder die
 * Bestandteile noch ihre Maße.</p>
 *
 * <p>Bekommt alles im Konstruktor: Deck- und Kartenname, Kategorie, Geometrien und die zwei
 * Rückmeldungen. Sie kennt weder {@code Deck} noch {@code MapService}; das bleibt im Feature.</p>
 *
 * <p>Die Bestandteile bekommen ihr Feld als {@code Rectangle2D} übergeben — diese View weiß, welche
 * Karte gerade läuft, die Bausteine nicht. Beim Skin holen sie sich nur, was ohne Schlüssel
 * auskommt.</p>
 */
public class RegionLearnView {

	private final String deckId;
	private final String mapName;
	private final String category;
	private final List<ShapeGeometry> geometries;
	private final RegionCallbacks callbacks;

	private final ComponentHost host = new ComponentHost();

	private ShapeMapPane map;
	private SuiteInfoLabel questionArea;
	private SuiteTextField inputField;

	public RegionLearnView(String deckId, String mapName, String category, List<ShapeGeometry> geometries,
			boolean mitFragefeld,
			RegionCallbacks callbacks) {
		this.deckId = deckId;
		this.mapName = mapName;
		this.category = category;
		this.geometries = geometries;
		this.callbacks = callbacks;
		rebuild(mitFragefeld);
	}

	/** Neu aufbauen — nötig nach einem Skinwechsel, weil sich alle Maße geändert haben können. */
	public void rebuild(boolean mitFragefeld) {
		Skin skin = SkinService.get();
		host.setWallpaper(skin.wallpaperPath(deckId, mapName, category));

		map = new ShapeMapPane(geometries, skin.learnComponentBounds(deckId, mapName, category, LearnComponent.MAP));
		map.setClickListener(callbacks.mapElementClicked());

		host.clear();
		if (mitFragefeld) {
			questionArea = new SuiteInfoLabel("",
					skin.learnTextLabelBounds(deckId, mapName, category, Skin.TextLabelType.QUESTION));
			questionArea.getStyleClass().add(Skin.TextLabelType.QUESTION.styleClass());
			// Einzeiliger Streifen mit einem einzelnen Namen darin — linksbündig sähe verloren aus.
			questionArea.centerText();
			inputField = null;
			host.addComponents(map, questionArea);
		} else {
			inputField = new SuiteTextField(skin.learnComponentBounds(deckId, mapName, category, LearnComponent.TEXT_INPUT));
			inputField.onType(callbacks.textTyped());
			questionArea = null;
			host.addComponents(map, inputField);
		}
	}

	public ScreenView getView() {
		return host;
	}

	// ----- Karte -----

	public void addIdsToActive(Set<String> ids)       { map.markActive(ids); }
	public void addIdsToMarked(Set<String> ids)       { map.mark(ids); }
	public void moveAllToActive()                     { map.reset(); }
	public void moveResolvedToActive()                { map.moveResolvedToActive(); }
	public void addIdsToCorrect(Set<String> elements) { map.markCorrect(elements); }
	public void addIdsToInactive(Set<String> elements){ map.markInactive(elements); }
	public void setIdToIncorrect(String element)      { map.markIncorrect(element); }
	public void setMapActive(boolean active)          { map.setActive(active); }

	public ShapeMapState getState()                   { return map.getState(); }
	public void setState(ShapeMapState state)         { map.setState(state); }

	// ----- Text -----

	public void setTextInTextField(String string)     { inputField.setText(string); }
	public void setTextFieldActive(boolean active)    { inputField.setActive(active); }

	public void setQuestion(String text) {
		if (questionArea != null)
			questionArea.setText(text);
	}

	public String getQuestion() {
		return questionArea != null ? questionArea.getText() : null;
	}
}
