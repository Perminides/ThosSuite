package app.controller;

import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.StartScreenView;

/**
 * Der Bildschirm, der läuft, wenn nichts läuft. Er hat keinen Inhalt und darf deshalb jederzeit
 * einem anderen weichen.
 */
class StartScreen implements Screen {

	private final StartScreenView view = new StartScreenView();

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		return SessionSwitchStrategy.IMMEDIATE;
	}

	@Override
	public void refresh() {
		view.rebuild();
	}

	@Override
	public ScreenView getView() {
		return view;
	}
}
