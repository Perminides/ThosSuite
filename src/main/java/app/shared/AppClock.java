package app.shared;

import java.time.LocalDate;

/**
 * Der <b>Arbeitstag</b> der Suite: der Kalendertag, an dem sie gestartet wurde, eingefroren bis
 * zum Beenden.
 *
 * <p><b>Wofür.</b> Für alles, was mit Daten rechnet, die beim Start feststehen: die Fälligkeit
 * von Karten und Regionen samt dem Fortschreiben des Lernstands, und die beim Start geholten
 * Activity- und Alkoholdaten samt ihrer Auswertung im Dashboard.</p>
 *
 * <p><b>Warum.</b> Zwei Gründe, und beide verlangen dasselbe. Eine Lernsession, die um 23:58
 * beginnt und um 00:03 speichert, muss durchgehend einen Tag meinen — sonst wird gegen den einen
 * Tag gerechnet und auf den anderen geschrieben. Und eine Auswertung muss denselben Tag nehmen,
 * mit dem ihre Daten geholt wurden: Der Wochen-Streak kann am Montag um 00:30 nichts über den
 * Sonntag sagen, weil der noch nicht importiert ist. Die laufende Uhr lieferte dort eine Zahl
 * über einen Tag, für den es keine Daten gibt.</p>
 *
 * <p><b>Wofür nicht.</b> Zeitstempel ({@code played_timestamp}, Logzeilen), Zeitspannen („wie
 * lange her") und Uhrzeiten („ist es vor 6 Uhr") nehmen die laufende Uhr. Sie fragen nicht nach
 * dem Arbeitstag.</p>
 */
public final class AppClock {
    public static final LocalDate TODAY = LocalDate.now();
    private AppClock() {}
    /** Legt den Arbeitstag fest, indem die Klasse geladen wird. {@code ThosSuiteApp} ruft sie als Erstes. */
    public static void init() {
    }
}