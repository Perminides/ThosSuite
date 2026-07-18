package app.shared.ui.surfaces;

import java.util.Arrays;

import app.shared.ui.contracts.ScreenView;
import app.shared.ui.contracts.UiComponent;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.Pane;

/**
 * Generischer Assembly-Host: hält die übergebenen Komponenten als Kinder einer Pane. Kennt kein
 * Feature, keinen Deck, kein Layout — die Komponenten bringen ihre Position selbst mit (Skin setzt
 * sie beim Erzeugen). Reine Pane (kein StackPane), damit nichts an den absolut positionierten
 * Kindern herumlayoutet. Über alle Feature-Sessions wiederverwendbar.
 */
public class ComponentHost implements ScreenView {

	private Pane pane = new Pane();
	
	public void setComponents(UiComponent... components) {
		Node[] views = Arrays.stream(components).map(UiComponent::getView).toArray(Node[]::new);
		pane.getChildren().setAll(views);
	}
	
	/**
	 * !Sofort: Hiermit zwingst Du eine KLasse im Feature sich ein javafx Image zu holen und das an den ComponentHost
	 * weiterzureichen. Das ist gechummelt! Sauberer wäre eine Featurewissenfreie Enum mitzugeben, über die sich der
	 * Host das passende BackgroundImage vom Skin holt. Jaha. Keine Ahnung, ob Du das wirklich so streng nehmen willst.
	 * Aber sauber ist das hier halt nicht!
	 * 
	 * !Sofort: Du musst auch in der Dokumentation sowas hier festhalten, oder? Also wie wird mit Hintergrundbildern in dem
	 * ganzen Konstrukt umgegangen. Uff.
	 * 
	 * @param image
	 */
	public void setBackgroundImage(BackgroundImage image) {
		pane.setBackground(new Background(image));
	}
	
    @Override
    public Pane getPane() {
        return pane;
    }
}