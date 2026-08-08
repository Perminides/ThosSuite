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
