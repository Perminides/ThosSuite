package app.shared.model;

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
 * {@link #sortOrderChanged()} und {@link #reactOnPausePressed()} sehen lern- bzw.
 * anki-spezifisch aus und könnten theoretisch in eine {@code LearnScreen}/
 * {@code AnkiScreen}-Hierarchie wandern. Das scheitert aber daran, dass der
 * Controller sie über eine {@code Screen}-Referenz aufruft
 * ({@code currentScreen.sort(...)}, {@code currentScreen.reactOnPausePressed()}) –
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
	 * Bitte mach deine Aufräumarbeiten vorm baldigen Schließen aber belästige
	 * Perminides nicht mit PopUps oder ähnlichem. Speichern darfst Du nur, wenn
	 * der Wert auf true steht, sonst ist das nicht gewollt.
	 */
	default void closeSilent(boolean save) {};
	
	/**
	 * Bitte mach deine Aufräumarbeiten inklusive Speichern vorm baldigen Schließen und wenn
	 * Du noch Rückfragen hast, darfst Du auch PopUps anzeigen.
	 */
	default void closeLoud() {};
	
	/**
	 * Der User hat auf Pause geklickt.
	 */
	default void reactOnPausePressed() {};

	/**
	 * Der User hat die Eingabetaste gedrückt — dasselbe wie ein Klick auf den Absende-Knopf.
	 */
	default void enterPressed() {};

	/**
	 * Ein modaler Dialog liegt jetzt über dieser Session — sie ist unterbrochen, bis
	 * {@link #resume()} kommt oder sie geschlossen wird.
	 * <p>
	 * <b>Aktuell genau eine Verwendung:</b> Der Controller legt beim Session-Wechsel seinen
	 * Speichern-/Verwerfen-Dialog über die laufende Session. Dessen {@code showAndWait} startet eine
	 * verschachtelte Event-Schleife, in der die Session weiterläuft, obwohl der Nutzer gar nicht mehr
	 * bei ihr ist.
	 * <p>
	 * Was das für einen Screen bedeutet, entscheidet er selbst — die Anki-Session hält ihre Uhr an,
	 * alle anderen tun nichts.
	 */
	default void suspend() {};

	/**
	 * Der Dialog ist weg, ohne dass die Session ersetzt wurde — der Nutzer hat abgebrochen.
	 * Gegenstück zu {@link #suspend()}.
	 */
	default void resume() {};
	
	/**
	 * Der User hat die Sortierreihenfolge geändert.
	 * @param order
	 */
	default void sortOrderChanged() {};
}
