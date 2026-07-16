package app.shared;

import app.shared.model.SessionSwitchStrategy;

/**
 * Vertrag für eine Bildschirmfläche, die das Hauptfenster ausfüllt – die neutrale
 * Abstraktion, mit der der Controller jeden aktiven Inhalt gleich behandelt
 * (Lern-Sessions ebenso wie AlcoholScreen, DashboardScreen, MovieViewerScreen …).
 * <p>
 * Alle Methoden sind leere Defaults: Ein Screen implementiert nur, was ihn betrifft.
 * Reagiert ein Screen auf eine Methode nicht (eine AlcoholScreen ignoriert
 * {@link #sortOrderChanged()}), ist das <b>kein Fehler</b>, sondern beabsichtigte
 * Nicht-Zuständigkeit – kein FailFast.
 * <p>
 * <b>Warum nicht in Sub-Interfaces aufgeteilt:</b> Methoden wie
 * {@link #sortOrderChanged()} und {@link #reactOnPauseClick()} sehen lern- bzw.
 * anki-spezifisch aus und könnten theoretisch in eine {@code LearnScreen}/
 * {@code AnkiScreen}-Hierarchie wandern. Das scheitert aber daran, dass der
 * Controller sie über eine {@code Screen}-Referenz aufruft
 * ({@code currentScreen.sort(...)}, {@code currentScreen.reactOnPauseClick()}) –
 * der konkrete Typ ist dort bewusst vergessen. Solange der Aufruf über {@code Screen}
 * läuft, müssen die Methoden hier liegen. Eine Trennung würde erst möglich, wenn der
 * Controller vor dem Aufruf die Screen-Art prüfte; dieser Umbau lohnt den Gewinn
 * derzeit nicht.
 */
public interface Screen {

	/**
	 * User möchte eine neue Session starten, was soll mit dieser passieren?
	 */
	SessionSwitchStrategy getSwitchStrategy();
	
	/**
	 * Bitte neu aufbauen. Vermutlich hat sich das SKin geändert.
	 */
	void refresh();
	
	/**
	 * Da braucht jemand die View. Vermutlich zum Anzeigen im MainWindow.
	 * !Sofort: Passt das hier überhaupt rein? Also wenn Screen nicht nur ein Empfänger von Befehlen ist sondern sich auch
	 * um die Anzeige kümmert, dann sehe ich nicht mehr wozu man nun auch ScreenFrame braucht... An sich finde ich es ja auch
	 * sehr gut beides in einem Interfacew abzufrühstücken Es gibt halt ein fensteraufüllendes-Objekt, welches im Spielfeld
	 * angezueigt werden will. Ok, darfs Du, aber dann musst Du dem MainWindow Zugriff auf eine Pane(Node/StackPane (weiß
	 * gerade nicht) geben, die er bei sich einhängt und Du musst bestimmte Events vom Controller verarbeiten (und darfst
	 * die auch ignorieren). Und du musst dem Controller sagen, ob Du jederzeit durch einen anderen Screen ersetzt werden
	 * darfst.
	 * 
	 */
	ScreenView getView();
	
	/**
	 * Manche Screens brauchen einen extra start-Aufruf
	 */
	default void start() {};
	
	/**
	 * ESC pressed
	 */
	default void escClicked() {};
	
	/**
	 * Bitte mach deine Aufräumarbeiten (Speichern?) vorm baldigen Schließen aber belästige
	 * Thorsten nicht mit PopUps oder ähnlichem.
	 */
	default void closeSilent(boolean save) {};
	
	/**
	 * Bitte mach deine Aufräumarbeiten (Speichern)
	 * vorm baldigen Schließen und wenn Du noch Rückfragen hast, darfst Du auch PopUps anzeigen.
	 */
	default void saveChosen() {};
	
	/**
	 * Der User hat auf Pause geklickt.
	 */
	default void reactOnPauseClick() {};
	
	/**
	 * Der User hat die Sortierreihenfolge geändert.
	 * @param order
	 */
	default void sortOrderChanged() {};
}
