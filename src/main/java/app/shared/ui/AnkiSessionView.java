package app.shared.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.shared.model.ScreenView;
import app.shared.model.SessionCallbacks;
import app.shared.model.UiComponent;
import app.shared.skin.SessionComponent;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.components.MultipleChoicePane;
import app.shared.ui.components.SuiteIconButton;
import app.shared.ui.components.SuiteImage;
import app.shared.ui.components.SuiteInfoLabel;
import app.shared.ui.components.SuiteTextField;
import app.shared.ui.components.map.SessionMap;

/**
 * Die gemeinsame Oberfläche aller Anki-Sessions: Frage, Bild, Multiple Choice, Fortschritt,
 * Historie, Zurück-Knopf. Sechs Bestandteile, die jede Lernform hat.
 *
 * <p>Sie übersetzt Absicht in Anzeige: „stell eine Frage" wird hier zu „setze diesen Text ins
 * Fragefeld". Der Presenter im Feature sagt nur, <em>was</em> passieren soll.</p>
 *
 * <p>Was die Lernformen unterscheidet, entscheiden die drei Unterklassen:</p>
 * <pre>
 * ShapeMapSessionView   Shape-Karte · Eingabefeld      · leerer Hintergrund
 * McSessionView         keine Karte · kein Eingabefeld · deck-eigener Hintergrund (mcWallpaperName)
 * ImageMapSessionView   Bild-Karte  · Eingabefeld      · leerer Hintergrund
 * </pre>
 *
 * <p>Benannt nach dem, was sie sind, nicht nach dem Deck, das sie gerade bedient — deshalb nehmen
 * sie Deck-Id, Kartenname und Kategorie entgegen und kennen keinen davon.</p>
 *
 * <p><b>Achtung beim Erweitern:</b> {@link #rebuild()} ruft {@link #createMap()} — also eine
 * Methode der Unterklasse. Der Basis-Konstruktor darf es deshalb <b>nicht</b> aufrufen, sonst liefe
 * es, bevor die Felder der Unterklasse gesetzt sind. Jede Unterklasse ruft {@code rebuild()} als
 * <b>letzte</b> Zeile ihres Konstruktors.</p>
 *
 * <p><b>Übergangszustand:</b> nur noch die Antwortauswahl kommt über eine Bau-Methode des Skins
 * ({@code createMultipleChoicePane}). Alle übrigen Bestandteile bekommen ihr Feld als
 * {@code Rectangle2D} übergeben und holen sich beim Skin nur, was ohne Schlüssel auskommt.</p>
 */
public abstract class AnkiSessionView {

	private final String deckId;
	private final String mapName;
	private final String kategorie;
	private final SessionCallbacks callbacks;

	private final ComponentHost canvas = new ComponentHost();

	private SessionMap karte;
	private SuiteInfoLabel questionArea;
	private SuiteInfoLabel progressArea;
	private SuiteInfoLabel cardHistoryArea;
	private SuiteImage imageComponent;
	private MultipleChoicePane mcPane;
	private SuiteIconButton backButton;
	private SuiteTextField inputField;

	protected AnkiSessionView(String deckId, String mapName, String kategorie, SessionCallbacks callbacks) {
		this.deckId = deckId;
		this.mapName = mapName;
		this.kategorie = kategorie;
		this.callbacks = callbacks;
	}

	// ===== Was die Unterklassen festlegen =====

	/** Die Karte dieser Lernform. Nie {@code null} — wer keine hat, liefert eine, die nichts tut. */
	protected abstract SessionMap createMap();

	protected abstract boolean hasInputField();

	/** {@code true} = eigenes Hintergrundbild des Decks, {@code false} = das leere. */
	protected abstract boolean usesDeckBackground();

	// ===== Für die Unterklassen =====

	protected String deckId()              { return deckId; }
	protected String mapName()             { return mapName; }
	protected String kategorie()           { return kategorie; }
	protected SessionCallbacks callbacks() { return callbacks; }

	// ===== Aufbau =====

	/** Neu aufbauen — nötig auch nach einem Skinwechsel, weil sich alle Maße geändert haben können. */
	public void rebuild() {
		Skin skin = SkinService.get();

		canvas.setBackgroundImage(usesDeckBackground()
				? skin.getBackgroundImage(mapName, kategorie)
				: skin.getEmptyBackgroundImage());

		karte = createMap();

		questionArea    = infoLabel(skin, Skin.TextLabelType.QUESTION);
		progressArea    = infoLabel(skin, Skin.TextLabelType.PROGRESS);
		cardHistoryArea = infoLabel(skin, Skin.TextLabelType.CARD_HISTORY);

		imageComponent = new SuiteImage(skin.sessionBounds(deckId, kategorie, SessionComponent.IMAGE));

		mcPane = skin.createMultipleChoicePane(deckId, kategorie);
		mcPane.addListener(callbacks.mcAnswerClicked());

		backButton = new SuiteIconButton(Skin.IconButtonType.BACK,
				skin.sessionBounds(deckId, kategorie, SessionComponent.BACK_BUTTON));
		backButton.onClick(callbacks.backClicked());

		inputField = null;
		if (hasInputField()) {
			inputField = new SuiteTextField(skin.sessionBounds(mapName, kategorie, SessionComponent.TEXT_INPUT));
			inputField.onType(callbacks.textTyped());
		}

		canvas.setComponents(bestandteile());
	}

	/** Die Kennung entscheidet nur über den abweichenden Hintergrund — der Rest steht in {@code .my-info-label}. */
	private SuiteInfoLabel infoLabel(Skin skin, Skin.TextLabelType typ) {
		SuiteInfoLabel label = new SuiteInfoLabel("", skin.sessionBounds(mapName, kategorie, typ));
		label.setId(typ + "Label");
		return label;
	}

	private UiComponent[] bestandteile() {
		List<UiComponent> parts = new ArrayList<>();
		parts.add(karte);
		parts.add(questionArea);
		if (inputField != null)
			parts.add(inputField);
		parts.add(imageComponent);
		parts.add(mcPane);
		parts.add(progressArea);
		parts.add(cardHistoryArea);
		parts.add(backButton);
		return parts.toArray(new UiComponent[0]);
	}

	public ScreenView getView() {
		return canvas;
	}

	// ===== Frage, Bild, Fortschritt =====

	public void setQuestion(String text)        { questionArea.setText(text); }
	public void setImage(String imageName)      { imageComponent.setImage(imageName); }
	public void setProgressText(String text)    { progressArea.setText(text); }
	public void setCardHistoryText(String text) { cardHistoryArea.setText(text); }

	// ===== Antwortauswahl =====

	public void setMultipleChoice(List<String> answers) { mcPane.initiateMultipleChoice(answers); }
	public void setMcCorrect(int id, boolean correct)   { mcPane.setCorrect(id, correct); }
	public void setMcSolution(Set<Integer> correctIds)  { mcPane.setCorrectAndInactive(correctIds); }

	/** Schaltet die Antwortauswahl ab. {@code McSessionView} überschreibt das leer — dort wäre es sinnlos. */
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
