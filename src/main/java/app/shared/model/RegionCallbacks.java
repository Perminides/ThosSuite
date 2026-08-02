package app.shared.model;

import java.util.function.Consumer;

/**
 * Die zwei Rückmeldungen einer Regions-Session an ihren Presenter.
 *
 * <p>Gebündelt wie {@link AnkiCallbacks}, aber als eigener Typ: Region kennt weder eine
 * Mehrfachauswahl noch einen Zurück-Knopf. Die Anki-Fassung mitzubenutzen hieße, zwei Felder
 * dauerhaft auf {@code null} zu setzen — dann sagt der Typ nicht mehr, was es wirklich gibt.</p>
 *
 * @param mapElementClicked eine Form auf der Karte wurde geklickt (id)
 * @param textTyped         im Eingabefeld wurde getippt (aktueller Text)
 */
public record RegionCallbacks(
		Consumer<String> mapElementClicked,
		Consumer<String> textTyped) {}
