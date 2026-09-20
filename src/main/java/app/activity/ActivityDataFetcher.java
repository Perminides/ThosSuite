package app.activity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.activity.model.DayData;
import app.activity.model.Exercise;
import app.activity.repository.Repository;
import app.shared.AppClock;
import app.shared.Log;

/**
 * Holt die noch fehlenden Tage aus der Google-Health-API, während der Splash sichtbar ist.
 * Blockiert den aufrufenden Thread und sammelt die Rohwerte für den Review-Dialog.
 *
 * <p>Der Bereich wird in zwei Aufrufen geholt, nicht Tag für Tag: Schritte und Aktivitäten nehmen
 * beide einen Zeitraum, und die Zuordnung zum lokalen Kalendertag macht Google selbst
 * (zeitzonen- und DST-fest).</p>
 *
 * <p>Ein Tag, für den Health keine Schritte führt, wird trotzdem geholt und kommt mit
 * {@code null} durch. Er darf den Import der übrigen Tage nicht aufhalten — der Review-Dialog
 * ist die Stelle, an der so ein Tag auffällt und sich von Hand füllen lässt.</p>
 *
 * <p><b>Wirft</b>, wenn etwas schiefgeht — auch bei einem toten Netz. Ob das den Start reißen
 * darf, ist eine Aussage über den Startablauf und gehört deshalb dem Controller, nicht dieser
 * Klasse.</p>
 */
public class ActivityDataFetcher {

    private final Repository repository = new Repository();
    private final List<DayData> fetchedDays = new ArrayList<>();

    /**
     * Holt alles, was seit dem letzten Import angefallen ist, bis einschließlich gestern.
     */
    public void fetch() {
        LocalDate lastImported = repository.getLastImportedDate();

        if (lastImported == null)
            throw new RuntimeException(
                "Kein Import-Verlauf gefunden. Bitte manuell das erste Datum in die Datenbank eintragen.");

        LocalDate startDate = lastImported.plusDays(1);
        LocalDate yesterday = AppClock.TODAY.minusDays(1);

        if (startDate.isAfter(yesterday)) {
            Log.debug(this, "Kein Import nötig. Letzter Import: " + lastImported);
            return;
        }

        Log.info(this, "Import von " + startDate + " bis " + yesterday);

        ApiClient client = new ApiClient();
        Map<LocalDate, Integer> stepsByDay = client.fetchDailySteps(startDate, yesterday);
        List<Exercise> activities = client.fetchActivities(startDate, yesterday);

        LocalDate current = startDate;
        while (!current.isAfter(yesterday)) {
            // stepsByDay.get liefert null, wenn Health den Tag nicht führt — das bleibt so
            fetchedDays.add(new DayData(current, stepsByDay.get(current), activitiesOn(current, activities)));
            current = current.plusDays(1);
        }
    }

    public boolean hasData() {
        return !fetchedDays.isEmpty();
    }

    public List<DayData> getFetchedDays() {
        return fetchedDays;
    }

    private static List<Exercise> activitiesOn(LocalDate date, List<Exercise> activities) {
        List<Exercise> ofDay = new ArrayList<>();
        for (Exercise activity : activities)
            if (activity.localDate().equals(date))
                ofDay.add(activity);
        return ofDay;
    }
}
