package app.shared.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.shared.model.McMetrics;
import app.shared.skin.SkinService;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;

/**
 * Eine Spalte aus Antwortfeldern, die sich nach und nach aufdecken. Reine Anzeige: nichts ist
 * anklickbar, nichts beschreibbar — getippt wird woanders.
 *
 * <p>Die Säule steht immer vollständig da, wie die acht Antworten bei Multiple Choice; welche Felder
 * zum aktuellen Schritt gehören, sagt ihr Zustand. Aussehen und Layout-Stufen teilt sie sich mit den
 * MC-Antworten, sie trägt dieselbe Style-Klasse und dieselben Maße.</p>
 *
 * <p>Die Zustände sind die von MC: {@code :active} heißt „im Spiel" (leer oder mit Hinweis),
 * {@code :inactive} heißt „gehört nicht zu diesem Schritt", dazu {@code :correct} für eine richtig
 * getippte und {@code :incorrect} für eine nach Zeitablauf aufgedeckte Antwort. Bei erzwungener
 * Reihenfolge kommt {@code :expected} auf das Feld, das gerade dran ist — ohne eigenes Aussehen, denn
 * das oberste nicht-grüne Feld ist ohnehin das gesuchte.</p>
 *
 * CSS-classes
 * 		Button	= "my-mc-button" + "my-answer-slot"
 */
public class AnswerSlotPane extends Pane {

	private static final PseudoClass STATE_INACTIVE = PseudoClass.getPseudoClass("inactive");
	private static final PseudoClass STATE_ACTIVE = PseudoClass.getPseudoClass("active");
	private static final PseudoClass STATE_CORRECT = PseudoClass.getPseudoClass("correct");
	private static final PseudoClass STATE_INCORRECT = PseudoClass.getPseudoClass("incorrect");
	private static final PseudoClass STATE_EXPECTED = PseudoClass.getPseudoClass("expected");

	private final McMetrics metrics;
	private final List<Button> slots = new ArrayList<>();

	/** Mit fester Lage — für absolut positionierende Hosts. Nur die Breite zählt, die Höhe ergibt sich. */
	public AnswerSlotPane(Rectangle2D bounds, int slotCount) {
		this(bounds.getWidth(), slotCount);
		setLayoutX(bounds.getMinX());
		setLayoutY(bounds.getMinY());
	}

	/** Ohne feste Lage — für Aufrufer, die die Spalte in ein Layout hängen. Muss es laut Regeldokument geben */
	public AnswerSlotPane(double width, int slotCount) {
		this.metrics = SkinService.get().mcMetrics();
		setMouseTransparent(true);

		double slotHeight = metrics.buttonHeight();
		int verticalGap = metrics.verticalGap();
		setPrefSize(width, (slotHeight * slotCount) + (verticalGap * (slotCount - 1)));

		double yPos = 0;
		for (int i = 0; i < slotCount; i++) {
			Button slot = createSlot(width, slotHeight);
			slot.setLayoutY(yPos);
			slots.add(slot);
			getChildren().add(slot);
			yPos += slotHeight + verticalGap;
		}
	}

	/** Nimmt so viele Felder ins Spiel, wie der Schritt Antworten sucht; der Rest bleibt außen vor. */
	public void showSlots(List<String> hints) {
		for (int i = 0; i < slots.size(); i++) {
			boolean inPlay = i < hints.size();
			fill(i, inPlay ? hints.get(i) : "", inPlay ? STATE_ACTIVE : STATE_INACTIVE);
		}
	}

	/** Eine richtig getippte Antwort — der Hinweis weicht ihr. */
	public void revealCorrect(int slot, String text) {
		fill(slot, text, STATE_CORRECT);
	}

	/** Was nach Ablauf der Zeit noch offen war. */
	public void revealMissing(Map<Integer, String> missing) {
		for (Map.Entry<Integer, String> eintrag : missing.entrySet())
			fill(eintrag.getKey(), eintrag.getValue(), STATE_INCORRECT);
	}

	/** Markiert das Feld, das als nächstes dran ist. {@code null} nimmt die Markierung überall weg. */
	public void setExpected(Integer slot) {
		for (int i = 0; i < slots.size(); i++)
			slots.get(i).pseudoClassStateChanged(STATE_EXPECTED, slot != null && slot == i);
	}

	/** Zurück in den Ruhezustand — die Säule bleibt stehen, gehört aber zu nichts mehr. */
	public void clear() {
		for (int i = 0; i < slots.size(); i++)
			fill(i, "", STATE_INACTIVE);
	}

	private void fill(int slot, String text, PseudoClass state) {
		Button button = slots.get(slot);
		button.setText(text);
		ButtonTextFit.apply(button, text, metrics);
		setSlotState(button, state);
		button.pseudoClassStateChanged(STATE_EXPECTED, false);
	}

	private Button createSlot(double width, double height) {
		Button slot = new Button();
		slot.setPrefSize(width, height);
		slot.setMinSize(width, height);
		slot.setMaxSize(width, height);
		slot.setLayoutX(0);
		slot.getStyleClass().addAll("my-mc-button", "my-answer-slot");
		slot.setWrapText(true);
		slot.setTextAlignment(TextAlignment.CENTER);
		slot.setAlignment(Pos.CENTER);
		setSlotState(slot, STATE_INACTIVE);
		return slot;
	}

	// Nur für Farben/Logik zuständig, fasst Layout nicht an!
	private void setSlotState(Button slot, PseudoClass state) {
		slot.pseudoClassStateChanged(STATE_INACTIVE, state == STATE_INACTIVE);
		slot.pseudoClassStateChanged(STATE_ACTIVE, state == STATE_ACTIVE);
		slot.pseudoClassStateChanged(STATE_CORRECT, state == STATE_CORRECT);
		slot.pseudoClassStateChanged(STATE_INCORRECT, state == STATE_INCORRECT);
	}
}
