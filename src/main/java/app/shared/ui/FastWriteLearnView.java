package app.shared.ui;

import java.util.List;
import java.util.Map;

import app.shared.Sound;
import app.shared.model.AnkiCallbacks;
import app.shared.skin.LearnComponent;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import javafx.scene.Node;
import app.shared.ui.components.AnswerSlotPane;
import app.shared.ui.components.SuiteCountdownLabel;
import app.shared.ui.components.map.EmptyLearnMap;
import app.shared.ui.components.map.LearnMap;

/**
 * Fast Write: keine Karte, keine Antwortauswahl — dafür eine Spalte aus Antwortfeldern und eine Uhr,
 * die herunterzählt. Getippt wird in dasselbe Eingabefeld wie bei den Karten-Decks; die Antwortfelder
 * zeigen nur an, was schon saß und was am Ende noch fehlte.
 */
public class FastWriteLearnView extends AnkiLearnView {

	/** Die Datei liegt im soundFolder — der Name gehört der Anzeige, nicht der Suite-Struktur. */
	private static final String CORRECT_SOUND = "sound.wav";

	private final int slotCount;

	private AnswerSlotPane slotPane;
	private SuiteCountdownLabel clock;

	/** @param slotCount wie viele Antwortfelder die Säule zeigt — die Obergrenze kennt das Feature. */
	public FastWriteLearnView(String deckId, String mapName, String kategorie, int slotCount, AnkiCallbacks callbacks) {
		super(deckId, mapName, kategorie, callbacks);
		this.slotCount = slotCount;
		rebuild(); // muss die letzte Zeile sein, siehe AnkiLearnView
	}

	@Override protected LearnMap createMap()    { return new EmptyLearnMap(); }
	@Override protected boolean hasInputField() { return true; }
	@Override protected boolean hasMcPane()     { return false; }
	@Override public void disableMcPanel()      {} // ohne Auswahl gibt es nichts abzuschalten

	/** {@code super.rebuild()} muss zuerst laufen — es räumt den Host leer. Siehe AnkiLearnView. */
	@Override
	public void rebuild() {
		super.rebuild();

		if (clock != null)
			clock.stop(); // die Uhr des alten Aufbaus darf nicht weiterlaufen

		Skin skin = SkinService.get();
		slotPane = new AnswerSlotPane(
				skin.learnComponentBounds(deckId(), kategorie(), LearnComponent.ANSWER_SLOTS), slotCount);
		clock = new SuiteCountdownLabel(
				skin.learnTextLabelBounds(mapName(), kategorie(), Skin.TextLabelType.CLOCK));
		clock.getStyleClass().add(Skin.TextLabelType.CLOCK.styleClass());
		clock.onExpired(callbacks().timeExpired());

		addComponents(slotPane, clock);
	}

	// ===== Antwortfelder =====

	public void showFastStep(List<String> hints, Integer expectedSlot, int firstSeconds, int nextSeconds) {
		slotPane.showSlots(hints);
		slotPane.setExpected(expectedSlot);
		clock.start(firstSeconds, nextSeconds);
	}

	public void revealSlot(int slot, String text, Integer expectedSlot) {
		slotPane.revealCorrect(slot, text);
		slotPane.setExpected(expectedSlot);
	}

	public void revealMissing(Map<Integer, String> missing) {
		slotPane.revealMissing(missing);
		slotPane.setExpected(null);
		clock.stop();
	}

	public void clearSlots() {
		slotPane.clear();
	}

	// ===== Uhr und Ton =====

	public void restartClock() { clock.restart(); }
	public void toggleClock()  { clock.togglePause(); }
	public void stopClock()    { clock.stop(); }
	public void suspendClock() { clock.suspend(); }
	public void resumeClock()  { clock.resume(); }

	public void playCorrectSound() { Sound.play(CORRECT_SOUND); }
}
