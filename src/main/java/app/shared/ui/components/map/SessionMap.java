package app.shared.ui.components.map;

import java.util.Set;

import app.shared.model.UiComponent;

/**
 * Die Karte einer Lern-Session — ein gemeinsames Vokabular über zwei sehr verschiedene Karten.
 *
 * <p>Umgesetzt von {@link ShapeMapPane} und {@link ImageMapPane} selbst — beide sind Nodes und
 * brauchen keinen Vermittler. Das Interface spricht durchgehend <b>Ids</b>; die Bild-Karte arbeitet
 * intern mit Geometrien und übersetzt sie über eine Funktion, die sie im Konstruktor bekommt.</p>
 *
 * <p>Damit muss die Session-Ansicht nicht wissen, welche Kartenart sie gerade hat — und die
 * MC-Session hat mit {@link NoSessionMap} einfach keine.</p>
 *
 * <p>Zwei Eigenheiten, die man nur hier sieht: bei der Shape-Karte ist {@link #setClickTargets} leer,
 * weil dort alle Formen von Anfang an im Szenengraphen liegen und ihre Klickbarkeit am Typ hängt —
 * es gibt nichts zu platzieren. Und die Bild-Karte kennt die falsch geklickte Form nicht per id, sie
 * merkt sich den letzten Klick selbst.</p>
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

	/**
	 * Nur diese Formen reagieren ab jetzt auf Klicks — die gerade gefragten.
	 *
	 * <p>Die Bild-Karte platziert sie dazu unsichtbar im Layer und macht alles andere für Klicks
	 * durchlässig. Sichtbar werden sie erst durch eine Bewertung.</p>
	 */
	void setClickTargets(Set<String> ids);

	/** Diese Formen hervorheben (Hinweis, nicht Bewertung). */
	void mark(Set<String> ids);
}
