package app.fitbit;

import app.fitbit.model.json.Activity;
import app.fitbit.model.json.ActivityDaySummary;
import app.fitbit.model.json.ActivityLogList;
import app.shared.Log;
import app.shared.skin.SkinService;

/**
 * Berechnet die Tagespunkte aus den Fitbit-Activities.
 * 
 * <h3>Punkteberechnung (über Jahre verfeinert):</h3>
 * <ul>
 *   <li>20 Schritte = 1 Punkt</li>
 *   <li>1 km Fahrradfahren = 19 Punkte</li>
 *   <li>1 Spinning-Session = 300 Punkte</li>
 *   <li>1 Workout = 200 Punkte</li>
 * </ul>
 * 
 * <h3>Spezialbehandlung:</h3>
 * <ul>
 *   <li><b>Bike/Fahrrad:</b> Kilometer werden in Punkte umgerechnet, Steps werden ignoriert</li>
 *   <li><b>Spinning:</b> Steps werden von Gesamtschritten abgezogen (da in fixen 300 Punkten enthalten)</li>
 *   <li><b>Sport:</b> Steps zählen normal mit</li>
 *   <li><b>Outdoor Bike:</b> Dialog zur manuellen Korrektur (oft fehlerhafte Aufzeichnung)</li>
 *   <li><b>Workout:</b> Fixe 200 Punkte</li>
 *   <li><b>Walk:</b> Steps zählen normal mit</li>
 * </ul>
 */
public class PointsCalculator {
    
    private static final int POINTS_FOR_SPINNING = 300;
    private static final double POINTS_FOR_WORKOUT = 200;
    public static final double POINTS_FOR_STEP = 0.05; // 20 Schritte = 1 Punkt. Benötigen wir fürs Dashboard...
    private static final double POINTS_FOR_KM_BIKE = 19;
    
    /**
     * Berechnet die Tagespunkte aus Activities und Tageszusammenfassung.
     * Falls die Punkte bereits in der Zusammenfassung gespeichert sind, werden diese zurückgegeben.
     * 
     * @param activities Die Liste aller Activities des Tages
     * @param daySummary Die Tageszusammenfassung mit Gesamtschritten
     * @return Die berechneten Punkte
     */
    public static int getDayPoints(ActivityLogList activities, ActivityDaySummary daySummary) {
        // Falls Punkte bereits berechnet und gespeichert wurden, diese zurückgeben
        if (daySummary.getSummary().getPoints() != null) {
            return daySummary.getSummary().getPoints();
        }
        
        double kmOnBike = 0d;
        int steps = daySummary.getSummary().getSteps();
        double points = 0;
        
        for (Activity activity : activities.getActivities()) {
            switch (activity.getActivityName()) {
                case "Bike":
                case "Fahrrad":
                    kmOnBike += activity.getDistance();
                    
                    // Plausibilitätsprüfung: Bike sollte keine Steps haben
                    if (activity.getSteps() != 0) {
                        Log.error(PointsCalculator.class, 
                            "Wieso hat Fahrradfahren nun Auswirkungen auf die Steps? Das ist neu. Anschauen!");
                        Log.error(PointsCalculator.class, "Activity: " + activity);
                        throw new RuntimeException(
                            "Unerwartete Steps bei Bike-Activity: " + activity.getSteps() + 
                            " (Activity: " + activity + ")"
                        );
                    }
                    break;
                    
                case "Walk":
                    // Steps zählen normal mit (sind bereits in daySummary.steps enthalten)
                    break;
                    
                case "Spinning":
                    // Steps von Gesamtsumme abziehen (da in fixen 300 Punkten enthalten)
                    steps -= activity.getSteps();
                    points += POINTS_FOR_SPINNING;
                    
                    // Plausibilitätsprüfung: Spinning sollte Steps haben
                    if (activity.getSteps() == 0) {
                        Log.error(PointsCalculator.class, 
                            "Wieso hat Spinning nun keine Auswirkungen auf die Steps? Das ist neu. Anschauen!");
                        Log.error(PointsCalculator.class, "Activity: " + activity);
                        throw new RuntimeException(
                            "Spinning ohne Steps: Das ist ungewöhnlich! (Activity: " + activity + ")"
                        );
                    }
                    break;
                    
                case "Sport":
                    // Steps zählen normal mit
                    if (activity.getSteps() == 0) {
                        Log.error(PointsCalculator.class, 
                            "Bisher hatte Sport immer eine gewisse Anzahl von Schritten und die sollten dann halt zählen. " +
                            "Sport mit 0 Schritten -> Keine Ahnung wie damit umgehen...");
                        Log.error(PointsCalculator.class, "Activity: " + activity);
                        // Hier werfen wir keine Exception, nur Warnung loggen
                    }
                    break;
                    
                case "Outdoor Bike":
                    // Plausibilitätsprüfung: Outdoor Bike sollte keine Steps haben
                    if (activity.getSteps() != 0) {
                        Log.error(PointsCalculator.class, 
                            "Bisher hatte Bike Outdoor keine Schritte. Was ja auch Mist ist, aber war halt so. " +
                            "Jetzt plötzlich doch...?");
                        Log.error(PointsCalculator.class, "Activity: " + activity);
                    }
                    
                    SkinService.get().createAlert(null, "Achtung", "Scheinbar hat eine Radfahren-Aktion nicht korrekt aufgezeichnet.\nWenn Du magst, trage einfach die Kilometer im Log nach und ändere den Typ auf Bike...", false, false);
                    Log.warn(PointsCalculator.class, 
                        "Outdoor Bike erkannt - möglicherweise fehlerhafte Aufzeichnung. " +
                        "Bitte manuell im Dialog korrigieren!");
                    break;
                    
                case "Workout":
                    points += POINTS_FOR_WORKOUT;
                    break;
                    
                default:
                	SkinService.get().createAlert(null, "Achtung", "Ich ignoriere die Aktivität " + activity.getActivityName(), false, false);
                    Log.warn(PointsCalculator.class, 
                        "Unbekannte Aktivität wird ignoriert: " + activity.getActivityName());
                    break;
            }
        }
        
        // Finale Berechnung: Bike-Kilometer + Steps + bereits gesammelte Punkte
        return (int) (points + kmOnBike * POINTS_FOR_KM_BIKE + steps * POINTS_FOR_STEP);
    }
}