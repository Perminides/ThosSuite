package app.shared.ui.components;

import javafx.scene.Node;
import javafx.scene.control.Button;

/**
 * Schmale Fassade um einen Button. Nach außen nur ein Klick-Listener.
 *
 * Vorerst wird der gestylte + positionierte Button von außen hereingereicht (der allwissende Skin
 * baut ihn). SPÄTER, beim Aufbrechen der Skin-Gottklasse, zieht diese Komponente das Styling selbst
 * an sich — mindestens CSS, vermutlich alles was der Skin heute tut, inkl. Positionierung.
 */
public class IconButton implements UiComponent {

	private final Button button;

	public IconButton(Button button) {
		this.button = button;
	}

	public void onClick(Runnable action) {
		button.setOnAction(_ -> action.run());
	}

	@Override
	public Node getView() {
		return button;
	}
}