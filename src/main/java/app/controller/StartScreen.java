package app.controller;

import app.shared.model.SessionSwitchStrategy;
import app.shared.skin.SkinService;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

/**
 * !Sofort: Hier wollen wir nicht ernsthaft Screen und Screenview trennen. Aber ich habe jetzt 5 Screens umgestellt
 * und jeder funktioniert anders. Alle haben Ihre Besonderheiten. Deine Idee, eine Bauanleitung, die überall durchgezogen
 * wird, rückt in unerreichbare Ferne. Schade. Müssen wir das alles dokumentieren? Ich weiß es auch nicht mehr! 
 */
class StartScreen implements Screen, ScreenView {
	
	private Pane pane;
	
	public StartScreen() {
		pane = new Pane();
		refresh();
	}

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		return SessionSwitchStrategy.IMMEDIATE;
	}

	@Override
	public void refresh() {
		pane.setBackground(new Background(SkinService.get().getStartBackgroundImage()));
	}

	@Override
	public ScreenView getView() {
		return this;
	}
	
	@Override
	public Pane getPane() {
		return pane;
	}

}
