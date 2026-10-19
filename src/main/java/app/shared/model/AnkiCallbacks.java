package app.shared.model;

import java.util.function.Consumer;

/**
 * Die Rückmeldungen einer Lern-Session an ihren Presenter.
 *
 * <p>Gebündelt, weil sie immer zusammen auftreten — sonst stünden sie in jedem
 * View-Konstruktor einzeln.</p>
 *
 * @param mapElementClicked eine Form auf der Karte wurde geklickt (id)
 * @param mcAnswerClicked   eine Antwort der Auswahl wurde geklickt (Index)
 * @param textTyped         im Eingabefeld wurde getippt (aktueller Text)
 * @param backClicked       der Zurück-Knopf wurde gedrückt
 * @param submitClicked     die markierte Auswahl soll geprüft werden
 * @param timeExpired       die Uhr eines Schritts mit Zeitlimit ist abgelaufen
 * @param collectAdded      ein Vorschlag wurde gewählt und soll als Chip dazu (Text)
 * @param collectRemoved    das × eines Chips wurde geklickt (Text)
 */
public record AnkiCallbacks(
		Consumer<String> mapElementClicked,
		Consumer<Integer> mcAnswerClicked,
		Consumer<String> textTyped,
		Runnable backClicked,
		Runnable submitClicked,
		Runnable timeExpired,
		Consumer<String> collectAdded,
		Consumer<String> collectRemoved) {}
