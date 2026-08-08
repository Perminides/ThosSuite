package app.shared.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.shared.model.ScreenView;
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
	private final String kategorie;
	private final AnkiCallbacks callbacks;

	private final ComponentHost canvas = new ComponentHost();

	private LearnMap karte;
	private SuiteInfoLabel questionArea;
	private SuiteInfoLabel progressArea;
	private SuiteInfoLabel cardHistoryArea;
	private SuiteImage imageComponent;
	private MultipleChoicePane mcPane;
	private SuiteIconButton backButton;
	private SuiteTextField inputField;

	protected AnkiLearnView(String deckId, String mapName, String kategorie, AnkiCallbacks callbacks) {
		this.deckId = deckId;
		this.mapName = mapName;
		this.kategorie = kategorie;
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
	protected String kategorie()           { return kategorie; }
	protected AnkiCallbacks callbacks() { return callbacks; }

	/** Für Unterklassen mit eigenen Bestandteilen — siehe „Achtung beim Erweitern" oben. */
	protected void addComponents(Node... teile) { canvas.addComponents(teile); }

	// ===== Aufbau =====

	/** Neu aufbauen — nötig auch nach einem Skinwechsel, weil sich alle Maße geändert haben können. */
	public void rebuild() {
		Skin skin = SkinService.get();

		canvas.setWallpaper(skin.wallpaperPath(mapName, kategorie));

		karte = createMap();

		questionArea    = infoLabel(skin, Skin.TextLabelType.QUESTION);
		progressArea    = infoLabel(skin, Skin.TextLabelType.PROGRESS);
		cardHistoryArea = infoLabel(skin, Skin.TextLabelType.CARD_HISTORY);

		imageComponent = new SuiteImage(skin.learnComponentBounds(deckId, kategorie, LearnComponent.IMAGE));

		// !Sofort: Bild, Auswahl und Knopf staffeln über deckId, Eingabefeld und Textfelder über
		// mapName. Bei allen Decks sind beide gleich, deshalb fällt es nicht auf — ein Deck mit
		// abweichendem mapName läse seine Maße aus zwei verschiedenen Properties.
		mcPane = null;
		if (hasMcPane()) {
			mcPane = new MultipleChoicePane(skin.learnComponentBounds(deckId, kategorie, LearnComponent.MC));
			mcPane.addListener(callbacks.mcAnswerClicked());
		}

		backButton = new SuiteIconButton(Skin.IconButtonType.BACK,
				skin.learnComponentBounds(deckId, kategorie, LearnComponent.BACK_BUTTON));
		backButton.onClick(callbacks.backClicked());

		inputField = null;
		if (hasInputField()) {
			inputField = new SuiteTextField(skin.learnComponentBounds(mapName, kategorie, LearnComponent.TEXT_INPUT));
			inputField.onType(callbacks.textTyped());
		}

		canvas.clear();
		canvas.addComponents(bestandteile());
	}

	/** Der Modifikator entscheidet nur über den abweichenden Hintergrund — der Rest steht in {@code .my-info-label}. */
	private SuiteInfoLabel infoLabel(Skin skin, Skin.TextLabelType typ) {
		SuiteInfoLabel label = new SuiteInfoLabel("", skin.learnTextLabelBounds(mapName, kategorie, typ));
		label.getStyleClass().add(typ.styleClass());
		return label;
	}

	private Node[] bestandteile() {
		List<Node> parts = new ArrayList<>();
		parts.add(karte.getView()); // die Karte ist ein Interface — der eine Schritt zum Node bleibt
		parts.add(questionArea);
		if (inputField != null)
			parts.add(inputField);
		parts.add(imageComponent);
		if (mcPane != null)
			parts.add(mcPane);
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

	// ===== Antwortauswahl =====

	public void setMultipleChoice(List<String> answers) { mcPane.initiateMultipleChoice(answers); }
	public void setMcCorrect(int id, boolean correct)   { mcPane.setCorrect(id, correct); }
	public void setMcSolution(Set<Integer> correctIds)  { mcPane.setCorrectAndInactive(correctIds); }

	/** Schaltet die Antwortauswahl ab. {@code McLearnView} überschreibt das leer — dort wäre es sinnlos. */
	public void disableMcPanel() { mcPane.clearAndSetInactive(); }

	// ===== Eingabefeld =====

	public void setTextFieldActive(boolean active) { if (inputField != null) inputField.setActive(active); }
	public void setTextInTextField(String text)    { if (inputField != null) inputField.setText(text); }

	// ===== Karte =====

	public void resetMarkers()                    { karte.reset(); }
	public void setMapActive(boolean active)      { karte.setActive(active); }
	public void addIdsToCorrect(Set<String> ids)  { karte.markCorrect(ids); }
	public void setIdToIncorrect(String id)       { karte.markIncorrect(id); }
	public void setMarkedIds(Set<String> ids)     { karte.mark(ids); }
	public void setClickTargets(Set<String> ids)  { karte.setClickTargets(ids); }
}
