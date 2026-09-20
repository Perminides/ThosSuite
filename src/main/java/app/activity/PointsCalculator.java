package app.activity;

import app.activity.model.ReviewedActivity;
import app.activity.model.ReviewedDay;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.ui.Alerts;

/**
 * Berechnet die Tagespunkte aus den reviewten Aktivitäten und der Tagesschrittzahl.
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
     * Die Tagespunkte aus Tagesschritten, Rad-Kilometern und Pauschalen.
     */
    public static int getDayPoints(ReviewedDay day) {
        double kmOnBike = 0d;
        double points = 0;

        for (ReviewedActivity activity : day.activities()) {
            switch (activity.exerciseType()) {
                case "WALKING" -> {
                    // Die Schritte zählen über die Tagessumme mit
                }
                case "BIKING" -> {
                    warnOnUnexpectedSteps(activity);
                    kmOnBike += activity.distanceKm();
                }
                case "OUTDOOR_BIKE" -> {
                    warnOnUnexpectedSteps(activity);
                    Alerts.show("Achtung", "Scheinbar hat eine Radfahren-Aktion nicht korrekt aufgezeichnet.\nWenn Du magst, trage einfach die Kilometer im Log nach und ändere den Typ auf BIKING...", ButtonEnum.OK);
                    Log.warn(PointsCalculator.class, "OUTDOOR_BIKE erkannt - möglicherweise fehlerhafte Aufzeichnung. Bitte manuell im Dialog korrigieren!");
                }
                case "SPINNING" -> {
                    warnOnUnexpectedSteps(activity);
                    points += POINTS_FOR_SPINNING;
                }
                case "WORKOUT" -> {
                    warnOnUnexpectedSteps(activity);
                    points += POINTS_FOR_WORKOUT;
                }
                default -> {
                    Alerts.show("Achtung", "Ich ignoriere die Aktivität " + activity.exerciseType(), ButtonEnum.OK);
                    Log.warn(PointsCalculator.class, "Unbekannter Aktivitätstyp wird ignoriert: " + activity.exerciseType());
                }
            }
        }

        return (int) (points + kmOnBike * POINTS_FOR_KM_BIKE + day.steps() * POINTS_FOR_STEP);
    }

    /**
     * Meldet Schritte an einer Aktivität, die ihre Punkte pauschal oder über Kilometer bekommt.
     */
    private static void warnOnUnexpectedSteps(ReviewedActivity activity) {
        if (activity.steps() == 0)
            return;

        Log.warn(PointsCalculator.class, "Aktivität mit eigenen Punkten trägt Schritte: " + activity);
        Alerts.show("Achtung", activity.exerciseType() + " bringt auf einmal " + activity.steps()
                + " Schritte mit. Die stecken schon in der Tagessumme und werden hier ein zweites Mal\n"
                + "bewertet. Bitte anschauen!", ButtonEnum.OK);
    }
}
