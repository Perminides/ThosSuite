package app.shared.ui.components;

import app.shared.model.McMetrics;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

/**
 * Misst, ob ein Antworttext in seinen Button passt, und schaltet die passende Layout-Stufe:
 * einzeilig, gequetscht zweizeilig ({@code :squeezed}) oder klein ({@code :tiny}).
 *
 * <p>Die Stufen sind additiv zu den Logik-Zuständen und werden bei jedem Aufruf zurückgesetzt. Wie
 * eine Stufe aussieht, steht im Stylesheet — hier wird nur entschieden, welche es sein muss.</p>
 *
 * <h2>Beim Justieren eines Skins</h2>
 *
 * <p><b>Gemessen wird an einem Stellvertreter, und der ist etwas optimistisch.</b> Ein blanker
 * {@code Text} meldet für zwei gequetschte Zeilen rund 1,4 Pixel weniger, als der fertige Knopf
 * tatsächlich braucht (bei Aptos 24 und 2px Rahmen: 48,99 gegen 50,40). Die Entscheidung kann
 * deshalb {@code :squeezed} wählen, obwohl die zweite Zeile nicht mehr hineinpasst — dann kürzt
 * JavaFX einzeilig mit Auslassungspunkten, statt auf {@code :tiny} durchzufallen.</p>
 *
 * <p><b>An einem Text liegt das nie.</b> Die Höhe eines umbrochenen Textes hängt allein an der
 * Zeilenzahl, nicht an den Buchstaben — bei Aptos 24 sind es 48,99 für zwei Zeilen und 68,69 für
 * drei. Zwischen den Stufen liegen zwanzig Pixel; in das 1,4-Pixel-Fenster kann kein Text fallen.
 * Es ist eine Eigenschaft der Skin-Einstellungen.</p>
 *
 * <p><b>Was zu tun ist, wenn Antworten Punkte statt kleiner Schrift zeigen:</b> die senkrechten
 * Insets von {@code borderSmallComponent} um ein bis zwei Pixel erhöhen. Jeder Pixel dort macht den
 * Knopf zwei Pixel höher, und in der gequetschten Stufe kommt das voll dem Text zugute, weil
 * {@code :squeezed} das senkrechte Padding ohnehin auf null setzt. Zwei Pixel Reserve reichen; wer
 * auf Kante justiert, ist eine Rundung vom Problem entfernt.</p>
 */
final class ButtonTextFit {

	private static final PseudoClass STATE_SQUEEZED = PseudoClass.getPseudoClass("squeezed");
	private static final PseudoClass STATE_TINY = PseudoClass.getPseudoClass("tiny");

	private ButtonTextFit() {}

	static void apply(Button btn, String text, McMetrics metrics) {
		btn.pseudoClassStateChanged(STATE_SQUEEZED, false);
		btn.pseudoClassStateChanged(STATE_TINY, false);

		double availableTextWidth = btn.getPrefWidth() - metrics.horizontalOverhead();

		Text measure = new Text(text);
		measure.setFont(metrics.font());

		if (measure.getLayoutBounds().getWidth() <= availableTextWidth)
			return;

		measure.setWrappingWidth(availableTextWidth);
		measure.setLineSpacing(metrics.lineSpacingSqueezed());

		// Limit ist die Button-Höhe ohne die beiden Rahmen. Kein Padding!
		double absoluteMaxHeight = btn.getPrefHeight() - (metrics.borderWidth() * 2);
		boolean passtGequetscht = measure.getLayoutBounds().getHeight() <= absoluteMaxHeight;
		btn.pseudoClassStateChanged(passtGequetscht ? STATE_SQUEEZED : STATE_TINY, true);
	}
}
