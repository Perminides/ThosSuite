package app.shared.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.shared.model.ScreenView;
import app.shared.model.ShapeGeometry;
import app.shared.model.SketchColor;
import app.shared.model.AnkiCallbacks;
import app.shared.skin.LearnComponent;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import javafx.scene.Node;
import app.shared.ui.components.MultipleChoicePane;
import app.shared.ui.components.SuiteIconButton;
import app.shared.ui.components.SuiteImage;
import app.shared.ui.components.SuiteInfoLabel;
import app.shared.ui.components.SuiteTextField;
import app.shared.ui.components.map.LearnMap;

/**
 * Die gemeinsame Oberfläche aller Anki-Sessions: Frage, Bild, Multiple Choice, Fortschritt,
 * Historie, Zurück-Knopf. Sechs Bestandteile, die jede Lernform hat.
 *
 * <p>Sie übersetzt Absicht in Anzeige: „stell eine Frage" wird hier zu „setze diesen Text ins
 * Fragefeld". Der Presenter im Feature sagt nur, <em>was</em> passieren soll.</p>
 *
 * <p>Was die Lernformen unterscheidet, entscheiden die drei Unterklassen:</p>
 * <pre>
 * ShapeMapLearnView   Shape-Karte · Eingabefeld
 * McLearnView         keine Karte · kein Eingabefeld
 * ImageMapLearnView   Bild-Karte  · Eingabefeld
 * </pre>
 *
 * <p>Der Hintergrund unterscheidet sie nicht: jede Session fragt dieselbe Staffelung ab — eigenes
 * Bild der Karte, sonst das der Kategorie, sonst das leere.</p>
 *
 * <p>Benannt nach dem, was sie sind, nicht nach dem Deck, das sie gerade bedient — deshalb nehmen
 * sie Deck-Id, Kartenname und Kategorie entgegen und kennen keinen davon.</p>
 *
 * <p><b>Achtung beim Erweitern:</b> {@link #rebuild()} ruft {@link #createMap()} — also eine
 * Methode der Unterklasse. Der Basis-Konstruktor darf es deshalb <b>nicht</b> aufrufen, sonst liefe
 * es, bevor die Felder der Unterklasse gesetzt sind. Jede Unterklasse ruft {@code rebuild()} als
 * <b>letzte</b> Zeile ihres Konstruktors.</p>
 *
 * <p><b>Eigene Bestandteile</b> hängt eine Unterklasse an, indem sie {@code rebuild()} überschreibt,
 * darin <b>zuerst</b> {@code super.rebuild()} ruft und danach {@link #addComponents(Node...)}. Die
 * Reihenfolge ist Pflicht: {@code super.rebuild()} räumt den Host leer, würde also alles wegwischen,
 * was vorher angehängt wurde.</p>
 *
 * <p><b>Übergangszustand:</b> nur noch die Antwortauswahl kommt über eine Bau-Methode des Skins
 * ({@code createMultipleChoicePane}). Alle übrigen Bestandteile bekommen ihr Feld als
 * {@code Rectangle2D} übergeben und holen sich beim Skin nur, was ohne Schlüssel auskommt.</p>
 */
public abstract class AnkiLearnView {

	private final String deckId;
	private final String mapName;
	private final String category;
	private final AnkiCallbacks callbacks;

	private final ComponentHost canvas = new ComponentHost();

	private LearnMap map;
	private SuiteInfoLabel questionArea;
	private SuiteInfoLabel progressArea;
	private SuiteInfoLabel cardHistoryArea;
	private SuiteImage imageComponent;
	private MultipleChoicePane mcPane;
	private SuiteIconButton backButton;
	private SuiteIconButton submitButton;
	private SuiteTextField inputField;

	protected AnkiLearnView(String deckId, String mapName, String category, AnkiCallbacks callbacks) {
		this.deckId = deckId;
		this.mapName = mapName;
		this.category = category;
		this.callbacks = callbacks;
	}

	// ===== Was die Unterklassen festlegen =====

	/** Die Karte dieser Lernform. Nie {@code null} — wer keine hat, liefert eine, die nichts tut. */
	protected abstract LearnMap createMap();

	protected abstract boolean hasInputField();

	/** Ob diese Lernform überhaupt eine Antwortauswahl kennt. */
	protected boolean hasMcPane() { return true; }

	// ===== Für die Unterklassen =====

	protected String deckId()              { return deckId; }
	protected String mapName()             { return mapName; }
	protected String category()           { return category; }
	protected AnkiCallbacks callbacks() { return callbacks; }

	/** Für Unterklassen mit eigenen Bestandteilen — siehe „Achtung beim Erweitern" oben. */
	protected void addComponents(Node... parts) { canvas.addComponents(parts); }

	// ===== Aufbau =====

	/** Neu aufbauen — nötig auch nach einem Skinwechsel, weil sich alle Maße geändert haben können. */
	public void rebuild() {
		Skin skin = SkinService.get();

		canvas.setWallpaper(skin.wallpaperPath(deckId, mapName, category));

		map = createMap();

		questionArea    = infoLabel(skin, Skin.TextLabelType.QUESTION);
		progressArea    = infoLabel(skin, Skin.TextLabelType.PROGRESS);
		cardHistoryArea = infoLabel(skin, Skin.TextLabelType.CARD_HISTORY);

		imageComponent = new SuiteImage(skin.learnComponentBounds(deckId, mapName, category, LearnComponent.IMAGE));

		mcPane = null;
		submitButton = null;
		if (hasMcPane()) {
			mcPane = new MultipleChoicePane(skin.learnComponentBounds(deckId, mapName, category, LearnComponent.MC));
			mcPane.addListener(callbacks.mcAnswerClicked());

			submitButton = new SuiteIconButton(Skin.IconButtonType.SUBMIT,
					skin.learnComponentBounds(deckId, mapName, category, LearnComponent.SUBMIT_BUTTON));
			submitButton.onClick(callbacks.submitClicked());
			// Sonst löst die Eingabetaste den zuletzt geklickten Knopf ein zweites Mal aus.
			submitButton.setFocusTraversable(false);
			submitButton.setDisable(true);
		}

		backButton = new SuiteIconButton(Skin.IconButtonType.BACK,
				skin.learnComponentBounds(deckId, mapName, category, LearnComponent.BACK_BUTTON));
		backButton.onClick(callbacks.backClicked());

		inputField = null;
		if (hasInputField()) {
			inputField = new SuiteTextField(skin.learnComponentBounds(deckId, mapName, category, LearnComponent.TEXT_INPUT));
			inputField.onType(callbacks.textTyped());
		}

		canvas.clear();
		canvas.addComponents(components());
	}

	/** Der Modifikator entscheidet nur über den abweichenden Hintergrund — der Rest steht in {@code .my-info-label}. */
	private SuiteInfoLabel infoLabel(Skin skin, Skin.TextLabelType typ) {
		SuiteInfoLabel label = new SuiteInfoLabel("", skin.learnTextLabelBounds(deckId, mapName, category, typ));
		label.getStyleClass().add(typ.styleClass());
		return label;
	}

	private Node[] components() {
		List<Node> parts = new ArrayList<>();
		parts.add(map.getView()); // die Karte ist ein Interface — der eine Schritt zum Node bleibt
		parts.add(questionArea);
		if (inputField != null)
			parts.add(inputField);
		parts.add(imageComponent);
		if (mcPane != null) {
			parts.add(mcPane);
			parts.add(submitButton);
		}
		parts.add(progressArea);
		parts.add(cardHistoryArea);
		parts.add(backButton);
		return parts.toArray(new Node[0]);
	}

	public ScreenView getView() {
		return canvas;
	}

	// ===== Frage, Bild, Fortschritt =====

	public void setQuestion(String text)        { questionArea.setText(text); }
	public void setImage(String imageName)      { imageComponent.setImage(imageName); }
	public void setProgress(String text)    { progressArea.setText(text); }
	public void setCardHistory(String text) { cardHistoryArea.setText(text); }

	// ===== Skizze =====
	// Sie sitzt im Bilderrahmen und teilt sich dessen Feld mit dem Bild — wer das eine zeigt,
	// verdrängt das andere.

	public void setSketch(List<ShapeGeometry> areas)        { imageComponent.setSketch(areas); }
	public void addSketch(List<ShapeGeometry> areas, int cell, double size, double offsetX,
			double offsetY) { imageComponent.addSketch(areas, cell, size, offsetX, offsetY); }
	public void moveSketchArea(int area, int cell)          { imageComponent.moveSketchArea(area, cell); }
	public void markSketchAreas(List<Integer> areas)                    { imageComponent.markSketchAreas(areas); }
	public void fillSketchAreas(List<Integer> areas, SketchColor color) { imageComponent.fillSketchAreas(areas, color); }

	// ===== Antwortauswahl =====

	public void setMultipleChoice(List<String> answers) { mcPane.initiateMultipleChoice(answers); }
	public void setMcCorrect(int id, boolean correct)   { mcPane.setCorrect(id, correct); }
	public void setMcMarked(int id, boolean marked)     { mcPane.setMarked(id, marked); }
	public void setMcSolution(Set<Integer> correctIds)  { mcPane.setCorrectAndInactive(correctIds); }

	/**
	 * Der Absende-Knopf steht immer da und ist nur bei einer Frage mit mehreren richtigen Antworten
	 * ansprechbar — er ist damit der erste Hinweis, dass gesammelt geantwortet wird.
	 */
	public void setSubmitActive(boolean active) { if (submitButton != null) submitButton.setDisable(!active); }

	/** Schaltet die Antwortauswahl ab. {@code McLearnView} überschreibt das leer — dort wäre es sinnlos. */
	public void disableMcPanel() { mcPane.clearAndSetInactive(); }

	// ===== Eingabefeld =====

	public void setTextFieldActive(boolean active) { if (inputField != null) inputField.setActive(active); }
	public void setTextInTextField(String text)    { if (inputField != null) inputField.setText(text); }

	// ===== Karte =====

	public void resetMarkers()                    { map.reset(); }
	public void setMapActive(boolean active)      { map.setActive(active); }
	public void addIdsToCorrect(Set<String> ids)  { map.markCorrect(ids); }
	public void setIdToIncorrect(String id)       { map.markIncorrect(id); }
	public void setMarkedIds(Set<String> ids)     { map.mark(ids); }
	public void setClickTargets(Set<String> ids)  { map.setClickTargets(ids); }
}
