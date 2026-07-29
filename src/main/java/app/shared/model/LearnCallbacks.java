package app.shared.model;

import java.util.function.Consumer;

/**
 * Die vier Rückmeldungen einer Lern-Session an ihren Presenter.
 *
 * <p>Gebündelt, weil sie immer zusammen auftreten — sonst stünden sie in jedem
 * View-Konstruktor einzeln.</p>
 *
 * @param mapElementClicked eine Form auf der Karte wurde geklickt (id)
 * @param mcAnswerClicked   eine Antwort der Auswahl wurde geklickt (Index)
 * @param textTyped         im Eingabefeld wurde getippt (aktueller Text)
 * @param backClicked       der Zurück-Knopf wurde gedrückt
 */
public record LearnCallbacks(
		Consumer<String> mapElementClicked,
		Consumer<Integer> mcAnswerClicked,
		Consumer<String> textTyped,
		Runnable backClicked) {}
