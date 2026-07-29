package app.shared.ui.components;

import java.util.function.Consumer;

import app.shared.model.UiComponent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.TextField;

/**
 * Ein Textfeld mit dem, was eine Eingabe-Situation braucht — vor allem
 * {@link #setActive(boolean)}: „aktiv schalten" heißt hier leeren, freigeben und fokussieren, drei
 * Dinge, die immer zusammen passieren.
 *
 * <p>Das Aussehen kommt über die JavaFX-eigene Klasse {@code .text-field}, die der Skin suite-weit
 * gestaltet. Die Komponente sieht deshalb überall richtig aus, auch ohne Maße.</p>
 */
public class SuiteTextField extends TextField implements UiComponent {

	/** Ohne feste Lage — für Aufrufer, die die Komponente in ein Layout hängen. */
	public SuiteTextField() {
	}

	/**
	 * Mit fester Lage — für absolut positionierende Hosts. Die Höhe bleibt offen, die bestimmen
	 * Schrift und Padding.
	 */
	public SuiteTextField(Rectangle2D bounds) {
		this();
		setLayoutX(bounds.getMinX());
		setLayoutY(bounds.getMinY());
		setPrefWidth(bounds.getWidth());
	}

	public void onType(Consumer<String> listener) {
		setOnKeyReleased(_ -> listener.accept(getText()));
	}

	/** Aktiv heißt: leer, bedienbar und im Fokus. */
	public void setActive(boolean active) {
		if (active) {
			setText("");
			setDisable(false);
			requestFocus();
		} else {
			setDisable(true);
		}
	}

	/** Ist selbst der Node. Bleibt, solange der ComponentHost über {@link UiComponent} einhängt. */
	@Override
	public Node getView() {
		return this;
	}
}
