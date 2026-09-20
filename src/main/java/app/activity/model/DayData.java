package app.activity.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Die abgeholten Rohwerte eines Tages, so wie die Google-Health-API sie liefert — vor dem
 * Review-Dialog und vor jeder Punkteberechnung.
 *
 * <p><b>Fehlende Schritte sind keine Null.</b> Führt Health für einen Tag keinen Schrittwert,
 * heißt das "Uhr nicht getragen oder nicht synchronisiert" — eine Null würde daraus eine
 * gelaufene Strecke von 0 machen und eine erfüllte Woche zerreißen. Der Unterschied bleibt
 * deshalb bis zum Dialog erhalten; erst dort wird daraus eine Zahl, die man sieht und ändern
 * kann.</p>
 *
 * @param date       der lokale Kalendertag
 * @param steps      Tagesschritte über alle Quellen; {@code null}, wenn Health für den Tag
 *                   keinen Wert führt
 * @param activities die Aktivitäten des Tages, ungemerged in der Reihenfolge der API
 */
public record DayData(LocalDate date, Integer steps, List<Exercise> activities) {}
