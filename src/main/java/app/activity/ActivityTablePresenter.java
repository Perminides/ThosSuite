package app.activity;

import java.util.ArrayList;
import java.util.List;

import app.activity.model.DayData;
import app.activity.model.Exercise;
import app.activity.model.ReviewedActivity;
import app.activity.model.ReviewedDay;
import app.shared.model.ActivityTableRow;
import app.shared.ui.ActivityTableDialog;

/**
 * Legt die Rohwerte eines Tages in die editierbare Tabelle und liest sie korrigiert zurück.
 *
 * <p>Die Summenzeile trägt die Tagesschritte und ist die einzige nicht löschbare Zeile — daran
 * erkennt der Rückbau sie wieder.</p>
 *
 * <p>Hier endet die Unterscheidung zwischen "fehlt" und "ist null": Der Dialog kennt nur Zahlen.
 * Ein Tag ohne Schrittwert steht deshalb als 0 in der Summenzeile und lässt sich dort füllen.</p>
 */
public class ActivityTablePresenter {

    private final DayData day;

    public ActivityTablePresenter(DayData day) {
        this.day = day;
    }

    /**
     * @return der bestätigte Tag, oder {@code null}, wenn abgebrochen wurde
     */
    public ReviewedDay showAndWait() {
        List<ActivityTableRow> edited =
            new ActivityTableDialog().show("Aktivitäten bearbeiten vom " + day.date(), toRows());
        if (edited == null)
            return null;
        return fromRows(edited);
    }

    /**
     * Der Tag so, wie der Dialog ihn zeigt, bevor jemand etwas ändert. Vergleichsgrundlage
     * dafür, ob überhaupt korrigiert wurde — die fehlenden Werte stehen hier schon als 0,
     * genau wie der Dialog sie anzeigen wird.
     */
    public ReviewedDay asShown() {
        List<ReviewedActivity> activities = new ArrayList<>();
        for (Exercise activity : day.activities()) {
            activities.add(new ReviewedActivity(
                activity.exerciseType(),
                activity.distanceKm() == null ? 0.0 : activity.distanceKm(),
                activity.steps() == null ? 0 : activity.steps(),
                activity.localDateTime().toString()));
        }
        return new ReviewedDay(day.date(), shownSteps(), activities);
    }

    private List<ActivityTableRow> toRows() {
        List<ActivityTableRow> rows = new ArrayList<>();
        // Summenzeile: synthetisch, nicht löschbar
        rows.add(new ActivityTableRow("(gesamt)", "Steps", "", null, shownSteps(), false, ""));
        for (Exercise activity : day.activities()) {
            rows.add(new ActivityTableRow(
                activity.localDateTime().toString(),
                activity.exerciseType(),
                activity.distanceKm() == null ? "" : "km",
                activity.distanceKm(),
                activity.steps(),
                true,
                ""));   // carry leer: die Rohwerte liegen ohnehin unverändert in DayData
        }
        return rows;
    }

    private ReviewedDay fromRows(List<ActivityTableRow> rows) {
        int totalSteps = 0;
        List<ReviewedActivity> activities = new ArrayList<>();

        for (ActivityTableRow row : rows) {
            if (!row.deletable()) {                 // die Summenzeile (einzige nicht löschbare)
                totalSteps = row.steps();
                continue;
            }
            activities.add(new ReviewedActivity(
                row.activityName(),
                row.distance(),
                row.steps(),
                row.startTime()));
        }
        return new ReviewedDay(day.date(), totalSteps, activities);
    }

    /** Die Tagesschritte als Zahl — ein fehlender Wert wird zur 0, die im Dialog sichtbar ist. */
    private int shownSteps() {
        return day.steps() == null ? 0 : day.steps();
    }
}
