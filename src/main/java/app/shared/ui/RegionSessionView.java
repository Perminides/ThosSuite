package app.shared.ui;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.shared.model.ScreenView;
import app.shared.model.ShapeGeometry;
import app.shared.model.ShapeMapState;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.components.ShapeMapPane;
import app.shared.ui.components.SuiteInfoLabel;
import app.shared.ui.components.SuiteTextField;

// !Sofort: Leider gibt es noch einen Fehler im freien Spiel. Die falsch geklickten bei schwer verschwinden nicht, nur die richtig geklickten...

/**
 * Die Oberfläche einer Region-Session: eine Karte, dazu entweder ein Fragefeld (Klick-Modi) oder
 * ein Eingabefeld (Schreib- und Eliminierungs-Modi).
 *
 * <p>Sie übersetzt Absicht in Anzeige: „stell eine Frage" wird hier zu „setze diesen Text ins
 * Fragefeld". Der Presenter im Feature sagt nur, <em>was</em> passieren soll — er kennt weder die
 * Bestandteile noch ihre Maße.</p>
 *
 * <p>Bekommt alles im Konstruktor: Kartenname, Kategorie, Geometrien und die zwei Rückmeldungen.
 * Sie kennt weder {@code Deck} noch {@code MapService}; das bleibt im Feature.</p>
 *
 * <p><b>Übergangszustand:</b> die Komponenten kommen noch über die Bau-Methoden des Skins
 * ({@code createSessionInfoLabel}, {@code createInputField}), weil dieselben Methoden noch von den
 * drei Anki-Panes gebraucht werden. Wenn die nachziehen, nehmen {@code SuiteInfoLabel} und
 * {@code SuiteTextField} ihre Maße selbst entgegen und die Bau-Methoden verlassen den Skin. Kein
 * Dauerzustand.</p>
 */
public class RegionSessionView {

	private final String mapName;
	private final String kategorie;
	private final List<ShapeGeometry> geometrien;
	private final Consumer<String> onMapElementClicked;
	private final Consumer<String> onTextTyped;

	private final ComponentHost host = new ComponentHost();

	private ShapeMapPane karte;
	private SuiteInfoLabel questionArea;
	private SuiteTextField inputField;

	public RegionSessionView(String mapName, String kategorie, List<ShapeGeometry> geometrien,
			boolean mitFragefeld,
			Consumer<String> onMapElementClicked,
			Consumer<String> onTextTyped) {
		this.mapName = mapName;
		this.kategorie = kategorie;
		this.geometrien = geometrien;
		this.onMapElementClicked = onMapElementClicked;
		this.onTextTyped = onTextTyped;
		rebuild(mitFragefeld);
	}

	/** Neu aufbauen — nötig nach einem Skinwechsel, weil sich alle Maße geändert haben können. */
	public void rebuild(boolean mitFragefeld) {
		Skin skin = SkinService.get();
		host.setBackgroundImage(skin.getBackgroundImage(mapName, kategorie));

		karte = new ShapeMapPane(geometrien, mapName, kategorie);
		karte.setClickListener(onMapElementClicked);

		// !Sofort: Hier wäre allerdings CENTER schon angesagt
		if (mitFragefeld) {
			questionArea = skin.createSessionInfoLabel(mapName, kategorie, Skin.TextLabelType.QUESTION);
			questionArea.setText("");
			inputField = null;
			host.setComponents(karte, questionArea);
		} else {
			inputField = new SuiteTextField(skin.createInputField(mapName, kategorie));
			inputField.onType(onTextTyped);
			questionArea = null;
			host.setComponents(karte, inputField);
		}
	}

	public ScreenView getView() {
		return host;
	}

	// ----- Karte -----

	public void addIdsToActive(Set<String> ids)       { karte.makeActive(ids); }
	public void addIdsToMarked(Set<String> ids)       { karte.addToMarked(ids); }
	public void moveAllToActive()                     { karte.moveAllToActive(); }
	public void moveCorrectToActive()                 { karte.moveCorrectToActive(); }
	public void addIdsToCorrect(Set<String> elements) { karte.addToCorrect(elements); }
	public void addIdsToInactive(Set<String> elements){ karte.makeInactive(elements); }
	public void setIdToIncorrect(String element)      { karte.addToIncorrect(element); }
	public void setMapActive(boolean active)          { karte.setInteractive(active); }

	public ShapeMapState getState()                   { return karte.getState(); }
	public void setState(ShapeMapState state)         { karte.setState(state); }

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
