package app.shared;

import javafx.stage.Window;

/**
 * Das Fenster, das allen Dialogen und Alerts der Suite als Owner dient. Wird einmal vom
 * {@code Controller} gesetzt, direkt nach {@code mainWindow.show()}.
 *
 * <p>Der Owner ist <b>Pflicht</b>, nicht Kosmetik: JavaFX bindet die Stylesheets der
 * Dialog-Scene per {@code Bindings.bindContent} an die Scene des Owners
 * ({@code HeavyweightDialog.updateStageBindings}). Ohne Owner bleibt ein Dialog ungestylt —
 * siehe die rohen Alerts in {@code ThosSuiteApp}. Die Bindung ist live: ein Skinwechsel wirkt
 * auch auf bereits offene Dialoge.</p>
 *
 * <p><b>Das ist globaler Zustand, und das ist der Schummel.</b> Nicht, dass er hier steht statt
 * woanders — sondern dass ein JavaFX-Fenster im Fundament abgelegt wird, damit jeder Dialog es
 * sich von dort holen kann, statt es durchgereicht zu bekommen. Der Preis dafür ist bewusst
 * gezahlt: Durchreichen hieße, jeden Dialog-Aufruf der Suite um einen Parameter zu erweitern, den
 * niemand je anders befüllen würde. Es gibt genau ein Hauptfenster, und es lebt so lange wie die
 * Suite.</p>
 *
 * <p>Eine eigene Klasse und kein Feld in einer Sammelklasse, damit die Frage „wo wohnt das
 * Owner-Fenster?" eine vorhersagbare Antwort hat.</p>
 */
public final class DialogOwner {

	private static Window window;

	private DialogOwner() {
	}

	public static void set(Window owner) {
		window = owner;
	}

	public static Window window() {
		return window;
	}
}
