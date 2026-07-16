package app.shared.ui.components;

import java.util.function.Consumer;

import javafx.scene.Node;
import javafx.scene.control.TextField;

/**
 * Schmale Fassade um ein TextField. Nach außen nur, was eine Lern-Session braucht: Text setzen/lesen,
 * aktiv schalten, auf Eingaben lauschen. Die rohe javafx-API bleibt drinnen.
 *
 * Vorerst wird das gestylte + positionierte TextField von außen hereingereicht (der allwissende Skin
 * baut es). SPÄTER, beim Aufbrechen der Skin-Gottklasse, zieht diese Komponente das Styling selbst an
 * sich — mindestens CSS, vermutlich alles was der Skin heute tut, inkl. Positionierung.
 */
public class InputField implements UiComponent {

	private final TextField field;

	public InputField(TextField field) {
		this.field = field;
	}

	public void onType(Consumer<String> listener) {
		field.setOnKeyReleased(_ -> listener.accept(field.getText()));
	}

	public void setText(String text) {
		field.setText(text);
	}

	public void setActive(boolean active) {
		if (active) {
			field.setText("");
			field.setDisable(false);
			field.requestFocus();
		} else {
			field.setDisable(true);
		}
	}

	@Override
	public Node getView() {
		return field;
	}
}