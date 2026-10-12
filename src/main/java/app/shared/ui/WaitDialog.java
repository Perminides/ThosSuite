package app.shared.ui;

import java.util.function.Supplier;

import app.shared.ui.components.SuiteDialog;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * Führt eine lange Arbeit aus, während ein modaler Dialog mit drehendem Kreis alles andere sperrt.
 * Für den Aufrufer synchron: {@link #run} kehrt erst zurück, wenn die Arbeit fertig ist.
 *
 * <p>Die dritte benannte Ausnahme von Architekturregel 13 (keine eigenen Threads). Die Arbeit läuft
 * in einem eigenen Thread, damit der FX-Thread frei bleibt — sonst dreht sich der Kreis nicht, und
 * Windows meldet das Fenster nach einigen Sekunden als „Keine Rückmeldung“. Gezeigt wird der Dialog
 * mit {@code showAndWait}, dessen verschachtelte Event-Schleife die Anzeige am Leben hält.</p>
 *
 * <p><b>Die Arbeit darf weder die Anzeige noch die Datenbank anfassen</b>, nur rechnen oder auf
 * etwas warten (HTTP). Gespeichert wird danach vom Aufrufer, wieder im FX-Thread. Die Anzeige ist
 * zusätzlich durch Regel 11 geschützt: Die Arbeit kommt aus einem Feature, und Features kennen kein
 * JavaFX.</p>
 *
 * <p>Esc und das X schließen nicht: Ein {@code Dialog} ohne Knöpfe lässt sich von außen nicht
 * schließen, nur über ein gesetztes Ergebnis. Schlägt die Arbeit fehl, wirft {@link #run} die
 * Exception im FX-Thread weiter.</p>
 */
public class WaitDialog {

	private WaitDialog() {
	}

	public static <T> T run(String message, Supplier<T> work) {
		SuiteDialog<Boolean> dialog = new SuiteDialog<>("Einen Moment");
		VBox content = dialog.contentBox();
		content.setAlignment(Pos.CENTER);
		content.getChildren().addAll(new ProgressIndicator(), new Label(message)); // Größe und Farbe aus dem Skin
		dialog.getDialogPane().setContent(content);

		Outcome<T> outcome = new Outcome<>();
		dialog.setOnShown(_ -> {
			Thread worker = new Thread(() -> {
				try {
					outcome.value = work.get();
				} catch (Throwable e) {
					outcome.error = e; // wird nach showAndWait im FX-Thread geworfen
				}
				Platform.runLater(() -> {
					dialog.setResult(Boolean.TRUE);
					dialog.close();
				});
			}, "WaitDialog");
			worker.setDaemon(true);
			worker.start();
		});
		dialog.showAndWait();

		if (outcome.error instanceof RuntimeException e)
			throw e;
		if (outcome.error instanceof Error e)
			throw e;
		if (outcome.error != null)
			throw new RuntimeException(outcome.error);
		return outcome.value;
	}

	/** Ergebnis aus dem Arbeits-Thread; sichtbar im FX-Thread durch {@code Platform.runLater}. */
	private static class Outcome<T> {
		T value;
		Throwable error;
	}
}
