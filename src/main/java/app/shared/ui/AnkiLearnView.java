package app.shared.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.shared.model.CardHistory;
import app.shared.model.CollectChip;
import app.shared.model.ScreenView;
import app.shared.model.SessionProgressCounter;
import app.shared.model.ShapeGeometry;
import app.shared.model.SketchColor;
import app.shared.model.SketchStructure;
import app.shared.model.AnkiCallbacks;
import app.shared.skin.LearnComponent;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import app.shared.ui.components.CollectChipPane;
import app.shared.ui.components.MultipleChoicePane;
import app.shared.ui.components.SuiteIconButton;
import app.shared.ui.components.SuiteImage;
import app.shared.ui.components.SuiteInfoLabel;
import app.shared.ui.components.SuiteSuggestionTextField;
import app.shared.ui.components.map.LearnMap;

/**
 * Die gemeinsame Oberfläche aller Anki-Sessions: Frage, Bild, Multiple Choice, Fortschritt,
 * Historie, Zurück-Knopf. Sechs Bestandteile, die jede Lernform hat.
 *
 * <p>Sie übersetzt Absicht in Anzeige: „stell eine Frage" wird hier zu „setze diesen Text ins
 * Fragefeld". Der Presenter im Feature sagt nur, <em>was</em> passieren soll.</p>
 *
 * <p>Was die Lernformen unterscheidet, entscheiden die vier Unterklassen:</p>
 * <pre>
 * ShapeMapLearnView   Shape-Karte · Eingabefeld
 * ImageMapLearnView   Bild-Karte  · Eingabefeld
 * McLearnView         keine Karte · kein Eingabefeld
 * FastWriteLearnView  keine Karte · Eingabefeld · keine Antwortauswahl
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
 * <p>Gebaut wird hier, nicht beim Skin: jeder Bestandteil bekommt sein Feld als
 * {@code Rectangle2D} übergeben und holt sich beim Skin nur, was ohne Schlüssel auskommt.</p>
 *
 * <p><b>Optionale Bestandteile</b> hängen an je einem Schalter — {@link #hasMcPane()} und
 * {@link #hasInputField()}. Beide folgen demselben Muster: {@code rebuild()} baut den Bestandteil
 * nur, wenn der Schalter wahr ist, und die Methoden, die ihn bedienen, tun bei einer Lernform ohne
 * ihn nichts. Eine Unterklasse muss dafür nichts überschreiben.</p>
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
	private SuiteSuggestionTextField inputField;

	private Rectangle2D questionBounds;
	private CollectChipPane collectChips;
	private List<String> collectSuggestions = List.of();
	private double collectGap;

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
			mcPane = new MultipleChoicePane(skin.learnComponentBounds(deckId, mapName, category, LearnComponent.MC),
					skin.mcMetrics(deckId, mapName, category));
			mcPane.addListener(callbacks.mcAnswerClicked());

			submitButton = new SuiteIconButton(Skin.IconButtonType.SUBMIT,
					skin.learnComponentBounds(deckId, mapName, category, LearnComponent.SUBMIT_BUTTON));
			submitButton.onClick(callbacks.submitClicked());
			// Sonst löst die Eingabetaste den zuletzt geklickten Knopf ein zweites Mal aus.
			submitButton.setFocusTraversable(false);
			submitButton.setInactive(true);
		}

		backButton = new SuiteIconButton(Skin.IconButtonType.BACK,
				skin.learnComponentBounds(deckId, mapName, category, LearnComponent.BACK_BUTTON));
		backButton.onClick(callbacks.backClicked());

		inputField = null;
		collectChips = null;
		questionBounds = skin.learnTextLabelBounds(deckId, mapName, category, Skin.TextLabelType.QUESTION);
		if (hasInputField())
			buildInput(skin);

		canvas.clear();
		canvas.addComponents(components());
	}

	/**
	 * Ein Eingabefeld für Input, Fast und Collect. Ohne Vorschläge prüft es bei jedem Tastendruck;
	 * bei Collect bekommt es die Vorschläge, eine Auswahl wird zum Chip. Die Chips hängen am unteren
	 * Rand des Fragefelds, siehe {@link #layoutCollect()}.
	 */
	private void buildInput(Skin skin) {
		inputField = new SuiteSuggestionTextField("", skin.learnComponentBounds(deckId, mapName, category, LearnComponent.TEXT_INPUT));
		inputField.onType(callbacks.textTyped());
		inputField.setSuggestionsAbove(skin.learnSuggestionsAbove());
		inputField.setOnSelected(word -> {
			inputField.clearSilent();
			callbacks.collectAdded().accept(word);
		});
		// Mit gesetztem onAction kommt Enter nicht mehr beim Fenster an — das Feld sendet selbst ab. Freitext zählt nicht.
		inputField.setOnAction(_ -> {
			if (inputField.getText().isBlank())
				callbacks.submitClicked().run();
		});

		collectGap = skin.learnChipGap();
		collectChips = new CollectChipPane(questionBounds.getWidth());
		collectChips.setLayoutX(questionBounds.getMinX());
		collectChips.onRemove(word -> {
			callbacks.collectRemoved().accept(word);
			inputField.requestFocus();
		});
		collectChips.setVisible(false);
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
		if (inputField != null) {
			parts.add(inputField);
			parts.add(collectChips);
		}
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

	// ===== Was als Nächstes erwartet wird =====
	// Der Presenter sagt nur die Absicht; welche Bausteine dafür an- und ausgehen, entscheidet die View.
	// Was nicht gefragt ist, wird abgeräumt: das MC-Feld, die Chips eines Collect-Steps. Ein neuer
	// Step beginnt ohne Auswahl, der Absende-Knopf ist deshalb grau.

	public void askMultipleChoice(List<String> answers) {
		setSubmitActive(false);
		setMapActive(false);
		setTextInTextField("");
		setTextFieldActive(false);
		endCollect();
		setMultipleChoice(answers);
	}

	public void askClick(Set<String> ids) {
		setSubmitActive(false);
		setClickTargets(ids);
		setMapActive(true);
		setTextInTextField("");
		setTextFieldActive(false);
		disableMcPanel();
		endCollect();
	}

	public void askText() {
		setSubmitActive(false);
		endCollect(); // sonst schlüge das Feld noch die Wörter eines vorigen Collect-Steps vor
		setTextFieldActive(true);
		setMapActive(false);
		disableMcPanel();
	}

	public void askCollect(List<String> suggestions) {
		setSubmitActive(false);
		setMapActive(false);
		disableMcPanel();
		startCollect(suggestions);
	}

	/** Friert ein, räumt nichts weg: was aufgedeckt oder beantwortet dasteht, bleibt sichtbar. */
	public void pause() {
		setMapActive(false);
		setTextFieldActive(false);
		setSubmitActive(false);
		freezeMcPanel();
	}

	/** Kartenende: alles weg, was die Karte hinterlassen hat. */
	public void clearCard() {
		resetMarkers();
		setImage(null);
		setTextInTextField("");
		setQuestion("");
		disableMcPanel();
		endCollect();
	}

	/** Die Lösung steht im Feld, tippen lässt sich nichts mehr. */
	public void revealText(String solution) {
		setTextInTextField(solution);
		setTextFieldActive(false);
	}

	/** Gewertet: die Chips zeigen das Ergebnis, ändern lässt sich nichts mehr. */
	public void revealCollect(List<CollectChip> chips) {
		setSubmitActive(false);
		setCollectChips(chips);
		setTextFieldActive(false);
	}

	// ===== Frage, Bild, Fortschritt =====

	public void setQuestion(String text)        { questionArea.setText(text); }
	public void setImage(String imageName)      { imageComponent.setImage(imageName); }
	public void setProgress(SessionProgressCounter progress) {
		progressArea.setText("Korrekt: " + progress.correct()
				+ "\nFalsch: " + progress.incorrect()
				+ "\nOffen: " + progress.open());
	}

	public void setCardHistory(CardHistory history) {
		cardHistoryArea.setText("Zuletzt gespielt: " + history.lastPlayed()
				+ "\nLevel: " + history.level()
				+ "\nFalsch beantwortet: " + history.wrongCount());
	}

	/** Eine Karte, die noch nie gespielt wurde, hat nichts zu zeigen. */
	public void clearCardHistory() { cardHistoryArea.setText(""); }

	// ===== Skizze =====
	// Sie sitzt im Bilderrahmen und teilt sich dessen Feld mit dem Bild — wer das eine zeigt,
	// verdrängt das andere.

	public void setSketch(SketchStructure sketch)           { imageComponent.setSketch(sketch); }
	public void addSketch(List<ShapeGeometry> areas, int cell, double size, double offsetX,
			double offsetY) { imageComponent.addSketch(areas, cell, size, offsetX, offsetY); }
	public void markSketchAreas(List<Integer> areas)                    { imageComponent.markSketchAreas(areas); }
	public void fillSketchAreas(List<Integer> areas, SketchColor color) { imageComponent.fillSketchAreas(areas, color); }

	// ===== Antwortauswahl =====

	private void setMultipleChoice(List<String> answers) { mcPane.initiateMultipleChoice(answers); }
	public void setMcCorrect(int id, boolean correct)   { mcPane.setCorrect(id, correct); }
	public void setMcMarked(int id, boolean marked)     { mcPane.setMarked(id, marked); }
	public void setMcSolution(Set<Integer> correctIds)  { mcPane.setCorrectAndInactive(correctIds); }
	public void rejectMcClick(int id)                   { mcPane.rejectClick(id); }

	/**
	 * Der Absende-Knopf steht immer da und ist nur bei einer Frage mit mehreren richtigen Antworten
	 * ansprechbar — er ist damit der erste Hinweis, dass gesammelt geantwortet wird.
	 *
	 * <p>Grau heißt hier wie bei den MC-Knöpfen nur „gerade gibt es nichts abzusenden"; abgeschaltet
	 * wird der Knopf nie. Was ein Klick bedeutet, entscheidet der Presenter — in der Pause schaltet
	 * er weiter, sonst sendet er ab oder läuft ins Leere.</p>
	 */
	public void setSubmitActive(boolean active) { if (submitButton != null) submitButton.setInactive(!active); }
	public void setBackActive(boolean active) { if (backButton != null) backButton.setDisable(!active); }

	/** Leert die Antwortauswahl — wenn als Nächstes anders geantwortet wird und am Kartenende. */
	private void disableMcPanel() {
		if (hasMcPane())
			mcPane.clearAndSetInactive();
	}

	/** Die Pause: offene Knöpfe werden grau, stehen bleibt, was dasteht. Ein Klick beendet die Pause. */
	private void freezeMcPanel() {
		if (hasMcPane())
			mcPane.freeze();
	}

	// ===== Eingabefeld =====

	public void setTextFieldActive(boolean active) { if (inputField != null) inputField.setActive(active); }
	public void setTextInTextField(String text)    { if (inputField != null) inputField.setText(text); }

	// ===== Sammeln (Collect) =====

	private void startCollect(List<String> suggestions) {
		if (inputField == null)
			throw new RuntimeException("Collect braucht eine Lernform mit Eingabefeld");
		collectSuggestions = List.copyOf(suggestions);
		inputField.setAllItems(collectSuggestions);
		inputField.setActive(true);
		collectChips.setChips(List.of());
		collectChips.setVisible(true);
		layoutCollect();
	}

	/** Was schon als Chip dasteht, wird nicht nochmal vorgeschlagen. */
	public void setCollectChips(List<CollectChip> chips) {
		collectChips.setChips(chips);
		layoutCollect();
		Set<String> taken = new HashSet<>();
		for (CollectChip chip : chips)
			taken.add(chip.text());
		List<String> remaining = new ArrayList<>();
		for (String word : collectSuggestions)
			if (!taken.contains(word))
				remaining.add(word);
		inputField.setAllItems(remaining);
	}

	/** Ohne Vorschläge, ohne Chips, volle Fragefeld-Höhe — das Feld ist wieder ein gewöhnliches Eingabefeld. */
	private void endCollect() {
		if (inputField == null)
			return;
		collectSuggestions = List.of();
		inputField.setAllItems(collectSuggestions);
		collectChips.setChips(List.of());
		collectChips.setVisible(false);
		layoutCollect();
	}

	/**
	 * Die Chips sitzen bündig am unteren Rand des Fragefeld-Rechtecks, das Fragefeld schrumpft um ihre
	 * Höhe und den Abstand aus dem Skin.
	 *
	 * <p>Läuft dort, wo sich die Chips ändern, und rechnet ihre Höhe selbst aus, statt auf die von
	 * JavaFX zu warten: Die steht erst im nächsten Layout-Durchlauf fest, und eine Änderung aus diesem
	 * Durchlauf heraus erschien erst beim übernächsten — nach dem letzten × blieb das Fragefeld klein,
	 * bis das Fenster den Fokus verlor.</p>
	 */
	private void layoutCollect() {
		double chips = 0;
		if (collectChips.isVisible() && !collectChips.getChildren().isEmpty()) {
			collectChips.applyCss();   // neue Chips haben ihr Stylesheet sonst noch nicht, ihre Größe wäre falsch
			chips = collectChips.prefHeight(questionBounds.getWidth());
		}
		double reserved = chips > 0 ? chips + collectGap : 0;
		collectChips.setLayoutY(questionBounds.getMaxY() - chips);
		questionArea.setFixedHeight(questionBounds.getHeight() - reserved);
	}

	// ===== Karte =====

	private void resetMarkers()                   { map.reset(); }
	private void setMapActive(boolean active)     { map.setActive(active); }
	public void addIdsToCorrect(Set<String> ids)  { map.markCorrect(ids); }
	public void setIdToIncorrect(String id)       { map.markIncorrect(id); }
	public void setMarkedIds(Set<String> ids)     { map.mark(ids); }
	private void setClickTargets(Set<String> ids) { map.setClickTargets(ids); }
}
