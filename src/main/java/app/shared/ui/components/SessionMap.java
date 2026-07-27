package app.shared.ui.components;

import java.util.Set;

import app.shared.model.UiComponent;

/**
 * Die Karte einer Lern-Session — ein gemeinsames Vokabular über zwei sehr verschiedene Karten.
 *
 * <p>{@link ShapeMapPane} adressiert über <b>Ids</b> und heißt „aktiv schalten", {@link ImageMapPane}
 * adressiert über <b>Geometrien</b> und heißt „zurücksetzen". Beide meinen dasselbe. Dieses
 * Interface spricht durchgehend Ids; die Bild-Karte übersetzt intern.</p>
 *
 * <p>Damit muss die Session-Ansicht nicht wissen, welche Kartenart sie gerade hat — und die
 * MC-Session hat einfach keine.</p>
 */
public interface SessionMap extends UiComponent {

	/** Alles zurück auf Anfang — keine Markierungen mehr. */
	void reset();

	/** Klickbar oder nicht. */
	void setActive(boolean active);

	/** Diese Formen sind richtig beantwortet. */
	void markCorrect(Set<String> ids);

	/** Diese Form war falsch. */
	void markIncorrect(String id);

	/** Diese Formen sind gerade gefragt. */
	void markInQuestion(Set<String> ids);

	/** Diese Formen hervorheben (Hinweis, nicht Bewertung). */
	void mark(Set<String> ids);
}
