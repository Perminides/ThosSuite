package app.controller;

import app.shared.Screen;
import app.shared.model.SessionSwitchStrategy;
import app.shared.skin.SkinService;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

class StartScreen implements Screen {
	
	private Pane pane;
	
	public StartScreen() {
		pane = new Pane();
		pane.setBackground(new Background(SkinService.get().getStartBackgroundImage()));
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
	public Pane getView() {
		return pane;
	}

}
