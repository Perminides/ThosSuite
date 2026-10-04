package app.activity;

import java.util.ArrayList;
import java.util.List;

import app.activity.model.DayPoints;
import app.activity.model.ReviewedActivity;
import app.activity.model.ReviewedDay;

/**
 * Berechnet die Tagespunkte aus den reviewten Aktivitäten und der Tagesschrittzahl.
 *
 * <p>Rechnet und meldet, fragt aber nicht: Auffälligkeiten kommen als Notizen im
 * {@link DayPoints} zurück, nicht als Dialog. Damit läuft die Berechnung auch dort, wo kein
 * Fenster aufgehen darf.</p>
 *
 * <h3>Punkteberechnung (über Jahre verfeinert):</h3>
 * <ul>
 *   <li>20 Schritte = 1 Punkt</li>
 *   <li>1 km Fahrradfahren = 19 Punkte</li>
 *   <li>1 Spinning-Session = 300 Punkte</li>
 *   <li>1 Workout = 200 Punkte</li>
 * </ul>
 *
 * <h3>Behandlung je Aktivitätstyp</h3>
 * <ul>
 *   <li><b>WALKING:</b> keine eigenen Punkte — die Schritte stecken bereits in der Tagessumme</li>
 *   <li><b>BIKING:</b> Kilometer werden in Punkte umgerechnet</li>
 *   <li><b>OUTDOOR_BIKE:</b> trägt keine Kilometer bei und meldet sich zur manuellen Korrektur</li>
 *   <li><b>SPINNING:</b> fixe 300 Punkte</li>
 *   <li><b>WORKOUT:</b> fixe 200 Punkte</li>
 *   <li><b>alles andere:</b> wird gemeldet und ignoriert</li>
 * </ul>
 *
 * <p><b>Warum es keine erschöpfende Typ-Tabelle gibt:</b> Google kann den {@code exerciseType}-Enum
 * jederzeit erweitern. Der {@code default}-Zweig ist deshalb die einzige Behandlung, die nicht
 * still veraltet — er zeigt den Rohwert des Typs, damit er sich direkt in den Code übernehmen
 * lässt.</p>
 *
 * <p><b>Der Schritt-Wächter:</b> Aktivitäten, die eigene Punkte erzeugen, dürfen keine Schritte
 * mitbringen, sonst wären dieselben Meter zweimal bewertet — einmal über die Tagessumme, einmal
 * über die Pauschale. Google liefert für sie derzeit gar kein Schritt-Feld; sollte sich das
 * ändern, meldet der Wächter es, statt es unbemerkt durchzulassen.</p>
 */
public class PointsCalculator {

    private static final int POINTS_FOR_SPINNING = 300;
    private static final double POINTS_FOR_WORKOUT = 200;
    public static final double POINTS_FOR_STEP = 0.05; // 20 Schritte = 1 Punkt. Benötigen wir fürs Dashboard...
    private static final double POINTS_FOR_KM_BIKE = 19;

    /**
     * Die Tagespunkte aus Tagesschritten, Rad-Kilometern und Pauschalen, samt allem, was dabei
     * auffiel.
     */
    public static DayPoints getDayPoints(ReviewedDay day) {
        double kmOnBike = 0d;
        double points = 0;
        List<String> notes = new ArrayList<>();

        for (ReviewedActivity activity : day.activities()) {
            switch (activity.exerciseType()) {
                case "WALKING" -> {
                    // Die Schritte zählen über die Tagessumme mit
                }
                case "BIKING" -> {
                    noteUnexpectedSteps(activity, notes);
                    kmOnBike += activity.distanceKm();
                }
                case "OUTDOOR_BIKE" -> {
                    noteUnexpectedSteps(activity, notes);
                    notes.add(
                        "Eine Radfahren-Aktion hat scheinbar nicht korrekt aufgezeichnet und trägt keine Kilometer bei.\n"
                        + "Wenn Du magst, trage die Kilometer im Log nach und ändere den Typ auf BIKING.");
                }
                case "SPINNING" -> {
                    noteUnexpectedSteps(activity, notes);
                    points += POINTS_FOR_SPINNING;
                }
                case "WORKOUT" -> {
                    noteUnexpectedSteps(activity, notes);
                    points += POINTS_FOR_WORKOUT;
                }
                default -> notes.add("Die Aktivität " + activity.exerciseType()
                        + " kenne ich nicht und habe sie mit null Punkten gezählt.");
            }
        }

        int total = (int) (points + kmOnBike * POINTS_FOR_KM_BIKE + day.steps() * POINTS_FOR_STEP);
        return new DayPoints(total, notes);
    }

    /**
     * Notiert Schritte an einer Aktivität, die ihre Punkte pauschal oder über Kilometer bekommt.
     */
    private static void noteUnexpectedSteps(ReviewedActivity activity, List<String> notes) {
        if (activity.steps() == 0)
            return;

        notes.add(activity.exerciseType() + " bringt auf einmal " + activity.steps()
                + " Schritte mit. Die stecken schon in der Tagessumme und werden hier ein zweites Mal\n"
                + "bewertet. Bitte anschauen!");
    }
}
