package app.controller;

import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.StartScreenView;

/**
 * Der Bildschirm, der läuft, wenn nichts läuft. Er hat keinen Inhalt und darf deshalb jederzeit
 * einem anderen weichen.
 *
 * <p>!Sofort: Ich habe jetzt 5 Screens umgestellt und jeder funktioniert anders. Alle haben ihre
 * Besonderheiten. Deine Idee, eine Bauanleitung, die überall durchgezogen wird, rückt in
 * unerreichbare Ferne. Schade. Müssen wir das alles dokumentieren? Ich weiß es auch nicht mehr!</p>
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
