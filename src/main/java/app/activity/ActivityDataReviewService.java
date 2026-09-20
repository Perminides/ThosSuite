package app.activity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.activity.model.DayData;
import app.activity.model.Exercise;
import app.activity.model.ReviewedActivity;
import app.activity.model.ReviewedDay;
import app.activity.repository.Repository;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.ui.Alerts;

/**
 * Zeigt für jeden abgeholten Tag den Review-Dialog, rechnet die Punkte und schreibt den Tag weg.
 *
 * <p>Gespeichert wird erst nach dem Review. Deshalb ist ein leeres {@code adjusted_data}
 * eindeutig: Es heißt "nichts korrigiert" und nie "noch nicht angesehen".</p>
 */
public class ActivityDataReviewService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Repository repository = new Repository();
    private final ActivityDataFetcher dataFetcher;

    public ActivityDataReviewService(ActivityDataFetcher dataFetcher) {
        this.dataFetcher = dataFetcher;
    }

    /**
     * Arbeitet die abgeholten Tage der Reihe nach ab. Ein Abbruch im Dialog beendet den Durchlauf;
     * die bis dahin bestätigten Tage bleiben gespeichert.
     */
    public void showDialogsAndSave() {
        List<DayImportResult> results = new ArrayList<>();

        for (DayData day : dataFetcher.getFetchedDays()) {
            DayImportResult result = showDialogAndSave(day);

            if (result == null) {
                Log.info(this, "Import abgebrochen bei Tag: " + day.date());
                break;
            }

            results.add(result);
        }

        if (!results.isEmpty())
            showSummary(results);
    }

    /**
     * @return das Ergebnis des Tages, oder {@code null}, wenn im Dialog abgebrochen wurde
     */
    private DayImportResult showDialogAndSave(DayData day) {
        Log.info(this, "Zeige Dialog für: " + day.date());
        warnOnMissingSteps(day);

        ActivityTablePresenter dialog = new ActivityTablePresenter(day);
        ReviewedDay reviewed = dialog.showAndWait();

        if (reviewed == null)
            return null;

        int points = PointsCalculator.getDayPoints(reviewed);
        Log.info(this, day.date() + " → " + points + " Punkte");

        String adjustedData = reviewed.equals(dialog.asShown()) ? null : toJson(reviewed);
        repository.saveDay(day.date(), points, toJson(day), adjustedData);

        return new DayImportResult(day.date(), points);
    }

    /**
     * Meldet einen Tag, für den Health keine Schritte führt, bevor sein Dialog aufgeht. Im Dialog
     * steht dann eine 0, die wie ein echter Wert aussieht — deshalb die Ansage vorweg, statt
     * darauf zu bauen, dass die Null beim Durchklicken auffällt.
     */
    private void warnOnMissingSteps(DayData day) {
        if (day.steps() != null)
            return;

        Log.warn(this, "Keine Health-Schritte für " + day.date());
        Alerts.show("Keine Schritte", "Health führt für " + day.date() + " keine Schrittzahl —\n"
                + "Uhr nicht getragen oder nicht synchronisiert.\n\n"
                + "Im folgenden Dialog steht deshalb eine 0 in der Summenzeile.\n"
                + "Trag die Zahl von Hand ein, sonst zählt der Tag mit 0 Schritten.", ButtonEnum.OK);
    }

    private void showSummary(List<DayImportResult> results) {
        StringBuilder message = new StringBuilder("Import abgeschlossen:\n\n");

        for (DayImportResult result : results)
            message.append(result.date()).append(" → ").append(result.points()).append(" Punkte\n");

        Alerts.show("Aktivitäts-Import", message.toString(), ButtonEnum.OK);
    }

    // --- JSON für raw_data und adjusted_data ---

    /**
     * Die Rohwerte der API. {@code null} bleibt {@code null} — hier steht, was Google geliefert
     * hat, und ein fehlendes Feld ist keine Null.
     */
    private static String toJson(DayData day) {
        List<Map<String, Object>> activities = new ArrayList<>();
        for (Exercise activity : day.activities()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", activity.exerciseType());
            entry.put("km", activity.distanceKm());
            entry.put("steps", activity.steps());
            entry.put("startTime", activity.localDateTime().toString());
            activities.add(entry);
        }
        return write(day.date(), day.steps(), activities);
    }

    /**
     * Die im Dialog bestätigten Werte, in denselben Feldern wie die Rohwerte, damit sich beide
     * Spalten nebeneinander lesen lassen.
     */
    private static String toJson(ReviewedDay day) {
        List<Map<String, Object>> activities = new ArrayList<>();
        for (ReviewedActivity activity : day.activities()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", activity.exerciseType());
            entry.put("km", activity.distanceKm());
            entry.put("steps", activity.steps());
            entry.put("startTime", activity.startTime());
            activities.add(entry);
        }
        return write(day.date(), day.steps(), activities);
    }

    private static String write(LocalDate date, Integer steps, List<Map<String, Object>> activities) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("date", date.toString());
        root.put("steps", steps);
        root.put("activities", activities);

        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fehler beim Schreiben der Tagesdaten für " + date, e);
        }
    }

    private record DayImportResult(LocalDate date, int points) {}
}
