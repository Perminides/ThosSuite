package app.shared.ui;

import java.nio.file.Path;

import app.shared.ui.components.SuiteBackground;

import app.shared.model.ScreenView;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Generischer Assembly-Host: hält die übergebenen Komponenten als Kinder einer Pane. Kennt kein
 * Feature, keinen Deck, kein Layout — die Komponenten bringen ihre Position selbst mit (Skin setzt
 * sie beim Erzeugen). Reine Pane (kein StackPane), damit nichts an den absolut positionierten
 * Kindern herumlayoutet. Über alle Feature-Sessions wiederverwendbar.
 */
public class ComponentHost implements ScreenView {

	private Pane pane = new Pane();
	
	public void setComponents(Node... components) {
		pane.getChildren().setAll(components);
	}
	
	public void setWallpaper(Path wallpaper) {
		pane.setBackground(SuiteBackground.of(wallpaper));
	}
	
    @Override
    public Pane getPane() {
        return pane;
    }
}