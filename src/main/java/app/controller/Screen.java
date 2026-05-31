package app.controller;

import app.data.CardSortOrder;
import app.data.SessionSwitchStrategy;
import javafx.scene.layout.Pane;

public interface Screen {

	/**
	 * User möchte eine neue Session starten, was soll mit dieser passieren?
	 */
	public SessionSwitchStrategy getSwitchStrategy();
	
	/**
	 * Bitte neu aufbauen. Vermutlich hat sich das SKin geändert.
	 */
	public void refresh();
	
	/**
	 * Da braucht jemand die View. Vermutlich zum Anzeigen im MainWindow.
	 */
	public Pane getView();
	
	/**
	 * Manche Screens brauchen einen extra start-Aufruf
	 */
	default void start() {};
	
	/**
	 * ESC pressed
	 */
	default void escClicked() {};
	
	/**
	 * Bitte mach deine Aufräumarbeiten (Speichern?) vorm baldigen Schließen aber belästige Thorsten nicht mit PopUps oder ähnlichem.
	 */
	default void closeSilent(boolean save) {};
	
	/**
	 * Bitte mach deine Aufräumarbeiten (Speichern?) vorm baldigen Schließen und wenn Du noch Rückfragen hast, darfs Du auch PopUps anzeigen.
	 */
	default void endGracefully() {};
	
	/**
	 * Der User hat auf Pause geklickt.
	 */
	public default void reactOnPauseClick() {};
	
	/**
	 * Der User hat die Sortierreihenfolge geändert.
	 * @param order
	 */
	public default void sort(CardSortOrder order) {};
}
