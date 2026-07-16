package app.tmp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.activity.ApiClient;
import app.activity.model.Exercise;
import app.fitbit.FitbitDayProjection;
import app.fitbit.model.json.Activity;
import app.shared.model.DialogButton;
import app.shared.skin.SkinService;

/**
 * Übergangs-Vergleicher: stellt pro importiertem Tag die Fitbit-Rohwerte (Master) den
 * Health-Rohwerten gegenüber — Schritte und Rad-Kilometer — und zeigt die Differenzen
 * in einem Popup nach dem Fitbit-Popup. So wird sichtbar, sobald Google die bekannte
 * ~1%-Lücke schließt.
 *
 * <p>Schreibt nebenbei die eingedampften Health-Daten pro Tag ins {@code health_import.log}
 * (über {@link HealthImportLog}), damit sich eine Rad-km-Diskrepanz auf die einzelne
 * Aktivität zurückverfolgen lässt.</p>
 *
 * <p><b>Isoliertes Wegwerf-Gerüst.</b> Steht über Fitbit und Health, gehört keinem der
 * beiden an, und fällt im September ersatzlos weg — deshalb Paket {@code app.tmp}.</p>
 *
 * <p>Zweigeteilt für den Startup-Split: {@link #fetch} blockiert im PreTask (Splash) und
 * sammelt die Zeilen, {@link #showPopup} zeigt sie im PostTask.</p>
 */
public class Comparison {

    private final List<Row> rows = new ArrayList<>();

    /**
     * PreTask: Health-Daten pro Tag holen, ins Log schreiben und gegen die Fitbit-Rohwerte
     * stellen. Blockierend (läuft während der Splash sichtbar ist), wie der Fitbit-Abruf.
     */
    public void fetch(List<FitbitDayProjection> fitbitDays) {
        ApiClient health = new ApiClient();       // refresht das Health-Token beim Erzeugen
        HealthImportLog log = new HealthImportLog();

        for (FitbitDayProjection day : fitbitDays) {
            LocalDate date = day.date();

            int    fitbitSteps = day.steps();
            double fitbitBikeKm = bikeKmFitbit(day.activities());

            Map<LocalDate, Integer> healthStepsByDay = health.fetchDailySteps(date, date);
            Integer healthSteps = healthStepsByDay.get(date); // null = kein Health-Wert (fehlt)
            List<Exercise> healthActivities = health.fetchActivities(date, date);
            double healthBikeKm = bikeKmHealth(healthActivities);

            log.write(date, healthSteps, healthActivities);

            rows.add(new Row(date, fitbitSteps, healthSteps, fitbitBikeKm, healthBikeKm));
        }
    }

    /**
     * PostTask: die gesammelten Vergleichszeilen als Popup zeigen. Erscheint nur, wenn
     * überhaupt Tage importiert wurden.
     */
    public void showPopup() {
        if (rows.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder("Vergleich Fitbit (Master) / Health:\n\n");
        for (Row row : rows) {
            message.append(row.date()).append('\n');

            message.append("  Schritte: ").append(row.fitbitSteps()).append(" / ")
                   .append(row.healthSteps() == null ? "—" : row.healthSteps());
            if (row.healthSteps() != null) {
                message.append("  (Δ ").append(row.healthSteps() - row.fitbitSteps()).append(')');
            }
            message.append('\n');

            message.append(String.format("  Rad-km: %.2f / %.2f  (Δ %.2f)%n",
                    row.fitbitBikeKm(), row.healthBikeKm(), row.healthBikeKm() - row.fitbitBikeKm()));
        }

        SkinService.get()
                .showAlert("Fitbit/Health-Vergleich", message.toString(), DialogButton.OK);
    }

    // --- Rad-km-Definition: an EINER Stelle, für beide Seiten ---

    private static double bikeKmFitbit(List<Activity> activities) {
        double km = 0;
        for (Activity a : activities) {
            if (isBikeFitbit(a.getActivityName()) && a.getDistance() != null) {
                km += a.getDistance();
            }
        }
        return km;
    }

    private static double bikeKmHealth(List<Exercise> activities) {
        double km = 0;
        for (Exercise e : activities) {
            if (isBikeHealth(e.exerciseType()) && e.distanceKm() != null) {
                km += e.distanceKm();
            }
        }
        return km;
    }

    private static boolean isBikeFitbit(String activityName) {
        return "Bike".equals(activityName)
                || "Fahrrad".equals(activityName)
                || "Outdoor Bike".equals(activityName);
    }

    private static boolean isBikeHealth(String exerciseType) {
        return "BIKING".equals(exerciseType)
                || "OUTDOOR_BIKE".equals(exerciseType);
    }

    private record Row(
            LocalDate date,
            int fitbitSteps,
            Integer healthSteps,
            double fitbitBikeKm,
            double healthBikeKm) {}
}