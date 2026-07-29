package app.shared.ui.components;

import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

/**
 * Ein Knopf mit Symbol statt Beschriftung. Nach außen nur ein Klick-Listener.
 *
 * <p>Welche Bilddatei zu welcher Rolle gehört und ob sie eingefärbt wird, hängt am Skin und nicht am
 * Aufrufer — deshalb holt der Knopf sich das Bild selbst. Übergeben wird nur die Rolle. Das Aussehen
 * kommt über die eigene Klasse {@code .my-icon-button}, also überall gleich.</p>
 */
public class SuiteIconButton extends Button {

	/** Ohne feste Lage — für Aufrufer, die die Komponente in ein Layout hängen. */
	public SuiteIconButton(Skin.IconButtonType rolle) {
		setGraphic(new ImageView(SkinService.get().iconFor(rolle)));
		getStyleClass().add("my-icon-button");
	}

	/** Mit fester Lage — für absolut positionierende Hosts. */
	public SuiteIconButton(Skin.IconButtonType rolle, Rectangle2D bounds) {
		this(rolle);
		setPrefSize(bounds.getWidth(), bounds.getHeight());
		setLayoutX(bounds.getMinX());
		setLayoutY(bounds.getMinY());
	}

	public void onClick(Runnable action) {
		setOnAction(_ -> action.run());
	}

}
