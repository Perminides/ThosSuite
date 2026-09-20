package app.activity.model;

/**
 * Eine Aktivität, nachdem sie durch den Review-Dialog gelaufen ist — die Fassung, aus der
 * die Punkte berechnet werden.
 *
 * <p>Eigener Typ neben {@link Exercise}, weil hier andere Zusagen gelten. {@code Exercise} ist das
 * unveränderte Abbild der API und unterscheidet "fehlt" von "ist null"; hinter dem Dialog gibt es
 * diesen Unterschied nicht mehr — dessen Felder sind Zahlen, ein fehlender Wert steht dort als 0.
 * Deshalb primitive Typen: sie sagen die Wahrheit über das, was ankommt.</p>
 *
 * <p>Die Startzeit ist nur noch Text — sie wird angezeigt und protokolliert, aber nirgends
 * mehr gerechnet.</p>
 *
 * @param exerciseType das Schaltkriterium der Punkteberechnung
 * @param distanceKm   Distanz in Kilometern, 0 wenn die Aktivität keine führt
 * @param steps        Schritte innerhalb der Aktivität, 0 wenn keine geliefert wurden
 * @param startTime    lokale Startzeit als Text, so wie sie im Dialog stand
 */
public record ReviewedActivity(
        String exerciseType,
        double distanceKm,
        int steps,
        String startTime) {}
