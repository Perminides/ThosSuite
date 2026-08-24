package app.shared.ui.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;

/**
 * Ein Feld mit einer großen Zahl, die im Sekundentakt herunterzählt. Erreicht sie null, hält die Uhr
 * an und meldet es einmal.
 *
 * <p>{@code :paused} sagt schlicht, dass die Uhr nicht läuft — von Hand angehalten
 * ({@link #togglePause()}), abgelaufen oder nach {@link #stop()}. Eine stehende Zahl ist damit von
 * einer tickenden zu unterscheiden. {@code stop()} friert den angezeigten Wert ein, statt ihn zu
 * löschen, so bleibt sichtbar, wie viel Zeit übrig war.</p>
 *
 * CSS-classes
 * 		das Label selbst	= "my-info-label" + "clock"
 */
public class SuiteCountdownLabel extends SuiteInfoLabel {

	private static final PseudoClass STATE_PAUSED = PseudoClass.getPseudoClass("paused");

	private final Timeline timeline;

	private Runnable onExpired;
	private int perAnswerSeconds;
	private int remaining;
	private boolean ranBeforePause;

	public SuiteCountdownLabel(Rectangle2D bounds) {
		super("", bounds);
		centerText();
		timeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> tick()));
		timeline.setCycleCount(Animation.INDEFINITE);
	}

	public void onExpired(Runnable listener) {
		this.onExpired = listener;
	}

	/**
	 * Startet die Uhr für den ersten Wert und merkt sich, mit wie viel jede weitere Antwort beginnt.
	 * Die erste bekommt mehr, weil zu ihr auch das Lesen der Frage gehört.
	 */
	public void start(int firstSeconds, int followingSeconds) {
		this.perAnswerSeconds = followingSeconds;
		begin(firstSeconds);
	}

	/** Zieht die Uhr für die nächste Antwort wieder voll auf. */
	public void restart() {
		begin(perAnswerSeconds);
	}

	public void togglePause() {
		if (timeline.getStatus() == Animation.Status.RUNNING) {
			timeline.pause();
			pseudoClassStateChanged(STATE_PAUSED, true);
		} else if (timeline.getStatus() == Animation.Status.PAUSED) {
			timeline.play();
			pseudoClassStateChanged(STATE_PAUSED, false);
		}
	}

	public void stop() {
		timeline.stop();
		pseudoClassStateChanged(STATE_PAUSED, true);
	}

	/**
	 * Hält die Uhr an, weil etwas anderes die Aufmerksamkeit hat. Merkt sich dabei, ob sie überhaupt
	 * lief — eine von Hand eingefrorene Uhr soll {@link #resume()} nicht auftauen.
	 */
	public void suspend() {
		ranBeforePause = timeline.getStatus() == Animation.Status.RUNNING;
		if (ranBeforePause) {
			timeline.pause();
			pseudoClassStateChanged(STATE_PAUSED, true);
		}
	}

	public void resume() {
		if (!ranBeforePause)
			return;
		ranBeforePause = false;
		timeline.play();
		pseudoClassStateChanged(STATE_PAUSED, false);
	}

	private void begin(int seconds) {
		remaining = seconds;
		setText(String.valueOf(remaining));
		pseudoClassStateChanged(STATE_PAUSED, false);
		timeline.playFromStart();
	}

	// Erst anhalten, dann melden: ein zweiter Tick nach dem Ablauf ist damit ausgeschlossen.
	private void tick() {
		remaining--;
		setText(String.valueOf(remaining));
		if (remaining <= 0) {
			stop();
			if (onExpired != null)
				onExpired.run();
		}
	}
}
