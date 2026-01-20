package app.controller;

import app.data.CardSortOrder;
import app.data.SessionSwitchStrategy;
import javafx.scene.layout.Pane;

public interface Session {

	public void start();
	
	/**
	 * ESC pressed
	 */
	public void escClicked();
	
	/**
	 * Für Anki-Sessions, User hat Beenden und Speichern geklickt oder alle Karten gelernt
	 */
	default void endGracefully() {};
	
	/**
	 * Bitte schließen ohne weitere Pop-Ups. User möchte eine neue Session starten
	 */
	public void closeSilent(boolean save);
	
	/**
	 * User möchte eine neue Session starten, was soll mit dieser passieren?
	 * @return
	 */
	public SessionSwitchStrategy getSwitchStrategy();
	
	public void refresh();
	public Pane getView();
	public default void endPause() {};
	public default void sort(CardSortOrder order) {};
}
